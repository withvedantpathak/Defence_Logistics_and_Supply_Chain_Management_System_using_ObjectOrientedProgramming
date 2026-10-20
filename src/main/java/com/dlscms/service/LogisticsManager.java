package com.dlscms.service;

import com.dlscms.exception.InsufficientStockException;
import com.dlscms.exception.InvalidStateTransitionException;
import com.dlscms.exception.LogisticsException;
import com.dlscms.exception.NoVehicleAvailableException;
import com.dlscms.exception.ValidationException;
import com.dlscms.model.Base;
import com.dlscms.model.Delivery;
import com.dlscms.model.DeliveryStatus;
import com.dlscms.model.InventoryItem;
import com.dlscms.model.Location;
import com.dlscms.model.Priority;
import com.dlscms.model.RequestStatus;
import com.dlscms.model.Supply;
import com.dlscms.model.SupplyRequest;
import com.dlscms.model.vehicle.Vehicle;
import com.dlscms.strategy.SourceCandidate;
import com.dlscms.strategy.SourceSelectionStrategy;
import com.dlscms.strategy.Strategies;
import com.dlscms.strategy.VehicleAssignmentStrategy;
import com.dlscms.strategy.VehicleOption;
import com.dlscms.util.Validator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Facade that coordinates requests, inventory, fleet and deliveries. Source and vehicle selection rules
 * are injected (Strategy pattern) and can be swapped at run time.
 */
public final class LogisticsManager {
    private final World world;
    private final InventoryService inventory;
    private final FleetService fleet;
    private final RequestService requests;
    private final DeliveryService deliveries;
    private final ReportService reports;
    private SourceSelectionStrategy sourceStrategy;
    private VehicleAssignmentStrategy vehicleStrategy;
    private boolean autoDispatch;

    /**
     * @param world           the network state
     * @param sourceStrategy  donor selection rule
     * @param vehicleStrategy vehicle selection rule
     */
    public LogisticsManager(World world, SourceSelectionStrategy sourceStrategy,
                            VehicleAssignmentStrategy vehicleStrategy) {
        this.world = world;
        this.inventory = new InventoryService(world);
        this.fleet = new FleetService(world);
        this.requests = new RequestService(world);
        this.deliveries = new DeliveryService(world, inventory);
        this.reports = new ReportService(world);
        this.sourceStrategy = sourceStrategy;
        this.vehicleStrategy = vehicleStrategy;
        world.setSourceStrategyName(sourceStrategy.name());
        world.setVehicleStrategyName(vehicleStrategy.name());
    }

    /**
     * Convenience factory using the strategy names stored in the world.
     *
     * @param world the network state
     * @return a manager configured from the world's strategy names
     * @throws ValidationException if a stored strategy name is unknown
     */
    public static LogisticsManager forWorld(World world) throws ValidationException {
        return new LogisticsManager(world, Strategies.source(world.getSourceStrategyName()),
                Strategies.vehicle(world.getVehicleStrategyName()));
    }

    // ---- accessors ----

    /** @return the network state */
    public World world() {
        return world;
    }

    /** @return inventory service */
    public InventoryService inventory() {
        return inventory;
    }

    /** @return fleet service */
    public FleetService fleet() {
        return fleet;
    }

    /** @return request service */
    public RequestService requests() {
        return requests;
    }

    /** @return delivery service */
    public DeliveryService deliveries() {
        return deliveries;
    }

    /** @return report service */
    public ReportService reports() {
        return reports;
    }

    /** @return active donor-selection strategy */
    public SourceSelectionStrategy sourceStrategy() {
        return sourceStrategy;
    }

    /** @return active vehicle-selection strategy */
    public VehicleAssignmentStrategy vehicleStrategy() {
        return vehicleStrategy;
    }

    /** @return true if deliveries are dispatched automatically as soon as they are planned */
    public boolean isAutoDispatch() {
        return autoDispatch;
    }

    /** @param on true to dispatch automatically after planning */
    public void setAutoDispatch(boolean on) {
        this.autoDispatch = on;
    }

    /**
     * @param name NEAREST, SURPLUS or BALANCED
     * @throws ValidationException if unknown
     */
    public void useSourceStrategy(String name) throws ValidationException {
        sourceStrategy = Strategies.source(name);
        world.setSourceStrategyName(sourceStrategy.name());
        world.setDirty(true);
    }

    /**
     * @param name SMALLEST or FASTEST
     * @throws ValidationException if unknown
     */
    public void useVehicleStrategy(String name) throws ValidationException {
        vehicleStrategy = Strategies.vehicle(name);
        world.setVehicleStrategyName(vehicleStrategy.name());
        world.setDirty(true);
    }

    // ---- bases (F1) ----

    /**
     * Registers a new base.
     *
     * @param id       base id
     * @param name     display name
     * @param lat      latitude
     * @param lon      longitude
     * @param capacity storage capacity in units
     * @return the new base
     * @throws LogisticsException if invalid or the id exists
     */
    public Base addBase(String id, String name, double lat, double lon, int capacity) throws LogisticsException {
        Base b = new Base(id, name, new Location(name, lat, lon), capacity);
        world.bases.add(b);
        world.setDirty(true);
        return b;
    }

    /**
     * Removes a base that holds no stock, hosts no vehicles and has no open requests or deliveries.
     *
     * @param id base id
     * @return the removed base
     * @throws LogisticsException if unknown or still in use
     */
    public Base removeBase(String id) throws LogisticsException {
        Base b = world.bases.get(id);
        for (InventoryItem it : world.inventory.forBase(b.getId())) {
            if (it.getQty() > 0) {
                throw new ValidationException("Cannot remove " + b.getId() + ": it still holds stock ("
                        + it.getQty() + " " + it.getSupplyId() + ")");
            }
        }
        for (Vehicle v : world.vehicles.all()) {
            if (v.getHomeBaseId().equals(b.getId()) || v.getCurrentBaseId().equals(b.getId())) {
                throw new ValidationException("Cannot remove " + b.getId() + ": vehicle " + v.getId()
                        + " is based there");
            }
        }
        if (!deliveries.baseIsIdle(b.getId())) {
            throw new ValidationException("Cannot remove " + b.getId() + ": it has open deliveries");
        }
        for (SupplyRequest r : world.requests.all()) {
            if (r.getRequesterBaseId().equals(b.getId()) && !r.getStatus().isFinal()) {
                throw new ValidationException("Cannot remove " + b.getId() + ": request " + r.getId() + " is open");
            }
        }
        world.bases.remove(b.getId());
        world.setDirty(true);
        return b;
    }

    // ---- requests (F3) ----

    /**
     * @param baseId   requesting base
     * @param supplyId supply
     * @param qty      units
     * @param priority urgency
     * @return the new NEW request
     * @throws LogisticsException if invalid
     */
    public SupplyRequest createRequest(String baseId, String supplyId, int qty, Priority priority)
            throws LogisticsException {
        return requests.create(baseId, supplyId, qty, priority);
    }

    // ---- planning (F4) ----

    private void requirePlannable(SupplyRequest r) throws InvalidStateTransitionException {
        if (r.getStatus() != RequestStatus.APPROVED && r.getStatus() != RequestStatus.FULFILLING) {
            throw new InvalidStateTransitionException("Request " + r.getId() + " is " + r.getStatus()
                    + "; cannot plan deliveries");
        }
        if (r.remaining() <= 0) {
            throw new InvalidStateTransitionException("Request " + r.getId()
                    + " is already fully planned or delivered");
        }
    }

    private Delivery planOne(SupplyRequest req, String forcedVehicleId, Set<String> excluded)
            throws LogisticsException {
        Supply supply = world.supplies.get(req.getSupplyId());
        Base dest = world.bases.get(req.getRequesterBaseId());
        List<SourceCandidate> candidates = new ArrayList<>();
        for (SourceCandidate c : inventory.candidates(supply, dest, req.getPriority())) {
            if (!excluded.contains(c.base().getId())) {
                candidates.add(c);
            }
        }
        if (candidates.isEmpty()) {
            throw new InsufficientStockException("Insufficient stock: no other base has spare "
                    + supply.getId() + " for " + req.getId());
        }
        Optional<SourceCandidate> chosen = sourceStrategy.select(req, candidates);
        if (chosen.isEmpty()) {
            throw new InsufficientStockException("No acceptable donor base for " + supply.getId());
        }
        Base source = chosen.get().base();
        List<VehicleOption> options = fleet.options(supply, source, dest);
        if (forcedVehicleId != null) {
            Vehicle forced = fleet.get(forcedVehicleId);
            List<VehicleOption> filtered = new ArrayList<>();
            for (VehicleOption o : options) {
                if (o.vehicle().getId().equals(forced.getId())) {
                    filtered.add(o);
                }
            }
            if (filtered.isEmpty()) {
                throw new NoVehicleAvailableException("Vehicle " + forced.getId() + " is not available or cannot carry "
                        + supply.getCategory());
            }
            options = filtered;
        }
        if (options.isEmpty()) {
            excluded.add(source.getId());
            throw new NoVehicleAvailableException("No vehicle available for " + supply.getCategory()
                    + " from " + source.getId());
        }
        int want = Math.min(req.remaining(), chosen.get().surplus());
        VehicleOption option = vehicleStrategy.select(options, want)
                .orElseThrow(() -> new NoVehicleAvailableException("No vehicle available for " + supply.getCategory()));
        int qty = Math.min(want, option.unitsPerTrip());
        return deliveries.plan(req, source, option, qty);
    }

    /**
     * Plans as many deliveries as needed (one per donor/vehicle trip) to cover the request's remaining units.
     * Stops when the request is fully planned or no further stock/vehicle can be found; partial planning is
     * allowed and the request stays FULFILLING.
     *
     * @param requestId request id
     * @return the deliveries planned by this call (at least one)
     * @throws InsufficientStockException  if no delivery could be planned because no base has spare stock
     * @throws NoVehicleAvailableException if stock exists but no vehicle can carry it
     * @throws LogisticsException          if the request is unknown or not in a plannable state
     */
    public List<Delivery> fulfil(String requestId) throws LogisticsException {
        SupplyRequest req = requests.get(requestId);
        requirePlannable(req);
        List<Delivery> created = new ArrayList<>();
        Set<String> excluded = new HashSet<>();
        boolean sawNoVehicle = false;
        LogisticsException last = null;
        while (req.remaining() > 0) {
            try {
                created.add(planOne(req, null, excluded));
            } catch (NoVehicleAvailableException e) {
                sawNoVehicle = true;
                last = e;
            } catch (InsufficientStockException e) {
                last = e;
                break;
            }
        }
        if (created.isEmpty()) {
            if (sawNoVehicle) {
                throw new NoVehicleAvailableException("No vehicle available for the supply "
                        + req.getSupplyId() + " (try TICK, FLT AVAIL, or free a vehicle)");
            }
            throw last != null ? last : new InsufficientStockException("Nothing could be planned");
        }
        if (autoDispatch) {
            for (Delivery d : created) {
                deliveries.dispatch(d.getId());
            }
        }
        return created;
    }

    /**
     * Plans a single delivery for a request, optionally forcing a particular vehicle.
     *
     * @param requestId request id
     * @param vehicleId vehicle id, or null to let the strategy choose
     * @return the planned delivery
     * @throws LogisticsException if no stock, no (or the named) vehicle is unavailable, or the state is wrong
     */
    public Delivery planOne(String requestId, String vehicleId) throws LogisticsException {
        SupplyRequest req = requests.get(requestId);
        requirePlannable(req);
        Delivery d = planOne(req, vehicleId, new HashSet<>());
        if (autoDispatch) {
            deliveries.dispatch(d.getId());
        }
        return d;
    }

    /**
     * Tries to plan every approved request in priority order. Failures of single requests are counted, not thrown.
     *
     * @return number of deliveries planned
     */
    public int fulfilAll() {
        int planned = 0;
        for (SupplyRequest r : requests.actionable()) {
            try {
                planned += fulfil(r.getId()).size();
            } catch (LogisticsException e) {
                // no stock or no vehicle right now: the request stays APPROVED for a later attempt
            }
        }
        return planned;
    }

    // ---- delivery lifecycle (F5) ----

    /**
     * Dispatches every PLANNED delivery.
     *
     * @return how many deliveries were dispatched
     * @throws LogisticsException if a dispatch is refused
     */
    public int dispatchAllPlanned() throws LogisticsException {
        int n = 0;
        for (Delivery d : world.deliveries.filter(x -> x.getStatus() == DeliveryStatus.PLANNED)) {
            dispatch(d.getId());
            n++;
        }
        return n;
    }

    /**
     * @param id delivery id
     * @return the dispatched delivery
     * @throws LogisticsException if unknown or not PLANNED
     */
    public Delivery dispatch(String id) throws LogisticsException {
        return deliveries.dispatch(id);
    }

    /**
     * @param id delivery id
     * @return the delivery, completed now
     * @throws LogisticsException if unknown or not IN_TRANSIT
     */
    public Delivery complete(String id) throws LogisticsException {
        return deliveries.complete(id, world.clock.now());
    }

    /**
     * @param id     delivery id
     * @param reason why it failed
     * @return the failed delivery
     * @throws LogisticsException if unknown or finished
     */
    public Delivery fail(String id, String reason) throws LogisticsException {
        return deliveries.fail(id, reason);
    }

    /**
     * Advances the simulated clock.
     *
     * @param hours hours to advance (must be positive)
     * @return deliveries that arrived during this period
     * @throws LogisticsException if hours is not positive
     */
    public List<Delivery> tick(long hours) throws LogisticsException {
        if (hours <= 0) {
            throw new ValidationException("hours must be a positive whole number, got " + hours);
        }
        return deliveries.tick(hours);
    }

    /**
     * @param id request id
     * @return the request's deliveries ordered by id
     */
    public List<Delivery> deliveriesOf(String id) {
        String rid = id.trim().toUpperCase();
        return world.deliveries.filter(d -> d.getRequestId().equals(rid));
    }

    /**
     * Sanity check used by tests: total stock of a supply (on hand plus in transit).
     *
     * @param supplyId supply id
     * @return units on hand plus units in transit
     */
    public int totalUnits(String supplyId) {
        return inventory.totalOnHand(supplyId) + deliveries.inTransitQty(supplyId);
    }

    /** @return true if any delivery is planned or in transit */
    public boolean hasOpenDeliveries() {
        for (Delivery d : world.deliveries.all()) {
            if (d.getStatus() == DeliveryStatus.PLANNED || d.getStatus() == DeliveryStatus.IN_TRANSIT) {
                return true;
            }
        }
        return false;
    }
}
