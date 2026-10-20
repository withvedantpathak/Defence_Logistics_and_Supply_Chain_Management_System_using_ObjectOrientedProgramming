package com.dlscms.service;

import com.dlscms.exception.LogisticsException;
import com.dlscms.model.Base;
import com.dlscms.model.Delivery;
import com.dlscms.model.DeliveryStatus;
import com.dlscms.model.InventoryItem;
import com.dlscms.model.Supply;
import com.dlscms.model.SupplyRequest;
import com.dlscms.model.vehicle.Vehicle;
import com.dlscms.strategy.VehicleOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Plans, dispatches, completes and fails deliveries, keeping stock, vehicles and requests consistent.
 * Stock rule: planning reserves at the donor; dispatch removes from the donor; arrival adds to the
 * destination; failure returns the stock to the donor. Total stock (on hand + in transit) never changes.
 */
public final class DeliveryService {
    private final World world;
    private final InventoryService inventory;

    /**
     * @param world     the shared network state
     * @param inventory inventory service
     */
    public DeliveryService(World world, InventoryService inventory) {
        this.world = world;
        this.inventory = inventory;
    }

    /**
     * @param id delivery id
     * @return the delivery
     * @throws LogisticsException if unknown
     */
    public Delivery get(String id) throws LogisticsException {
        return world.deliveries.get(id);
    }

    /**
     * Plans one delivery: reserves stock at the donor, assigns the vehicle, records the request progress.
     * All three effects happen or none do.
     *
     * @param request the request served
     * @param source  donor base
     * @param option  the vehicle option chosen
     * @param qty     units to carry (must not exceed the option's capacity)
     * @return the new PLANNED delivery
     * @throws LogisticsException if stock, vehicle or request state does not allow it
     */
    public Delivery plan(SupplyRequest request, Base source, VehicleOption option, int qty)
            throws LogisticsException {
        Supply supply = world.supplies.get(request.getSupplyId());
        Vehicle vehicle = option.vehicle();
        InventoryItem item = inventory.cell(source.getId(), supply.getId());
        item.reserve(qty);
        try {
            vehicle.assign();
        } catch (LogisticsException e) {
            item.release(qty);
            throw e;
        }
        try {
            request.onPlanned(qty);
        } catch (LogisticsException e) {
            item.release(qty);
            vehicle.release(vehicle.getCurrentBaseId());
            throw e;
        }
        long duration = Math.max(1, (long) Math.ceil(option.etaHours()));
        long now = world.clock.now();
        Delivery d = new Delivery(world.deliveryIds.next(), request.getId(), source.getId(),
                request.getRequesterBaseId(), supply.getId(), qty, vehicle.getId(), DeliveryStatus.PLANNED,
                now, -1, now + duration, -1, option.haulKm(), "");
        world.deliveries.add(d);
        world.setDirty(true);
        return d;
    }

    /**
     * Starts a planned delivery: stock leaves the donor and the vehicle departs.
     *
     * @param id delivery id
     * @return the delivery
     * @throws LogisticsException if unknown or not PLANNED
     */
    public Delivery dispatch(String id) throws LogisticsException {
        Delivery d = get(id);
        d.dispatch(world.clock.now());
        inventory.cell(d.getSourceBaseId(), d.getSupplyId()).dispatchOut(d.getQty());
        world.vehicles.get(d.getVehicleId()).depart();
        world.setDirty(true);
        return d;
    }

    /**
     * Completes a delivery at the given hour: stock is added at the destination, the vehicle is freed there
     * and the request's progress is updated.
     *
     * @param id   delivery id
     * @param hour arrival hour
     * @return the delivery
     * @throws LogisticsException if unknown or not IN_TRANSIT
     */
    public Delivery complete(String id, long hour) throws LogisticsException {
        Delivery d = get(id);
        d.complete(hour);
        inventory.cell(d.getDestBaseId(), d.getSupplyId()).receive(d.getQty());
        world.vehicles.get(d.getVehicleId()).release(d.getDestBaseId());
        world.requests.get(d.getRequestId()).onDelivered(d.getQty());
        world.setDirty(true);
        return d;
    }

    /**
     * Fails a delivery: reserved or in-transit stock goes back to the donor and the vehicle is freed.
     *
     * @param id     delivery id
     * @param reason why it failed
     * @return the delivery
     * @throws LogisticsException if unknown or already finished
     */
    public Delivery fail(String id, String reason) throws LogisticsException {
        Delivery d = get(id);
        DeliveryStatus before = d.getStatus();
        d.fail(reason, world.clock.now());
        InventoryItem item = inventory.cell(d.getSourceBaseId(), d.getSupplyId());
        Vehicle v = world.vehicles.get(d.getVehicleId());
        if (before == DeliveryStatus.PLANNED) {
            item.release(d.getQty());
            v.release(v.getCurrentBaseId());
        } else {
            item.receive(d.getQty());
            v.release(d.getSourceBaseId());
        }
        world.requests.get(d.getRequestId()).onFailed(d.getQty());
        world.setDirty(true);
        return d;
    }

    /**
     * Advances the clock and completes every in-transit delivery whose ETA has passed (in ETA order,
     * recorded at its ETA hour, not at the new clock time).
     *
     * @param hours hours to advance
     * @return deliveries completed by this tick
     * @throws LogisticsException if a completion fails (should not happen for consistent data)
     */
    public List<Delivery> tick(long hours) throws LogisticsException {
        world.clock.advance(hours);
        List<Delivery> due = new ArrayList<>();
        for (Delivery d : world.deliveries.all()) {
            if (d.getStatus() == DeliveryStatus.IN_TRANSIT && d.getEtaHour() <= world.clock.now()) {
                due.add(d);
            }
        }
        due.sort(Comparator.comparingLong(Delivery::getEtaHour).thenComparing(Delivery::getId));
        List<Delivery> done = new ArrayList<>();
        for (Delivery d : due) {
            done.add(complete(d.getId(), d.getEtaHour()));
        }
        world.setDirty(true);
        return done;
    }

    /**
     * @param supplyId supply id
     * @return units of that supply currently travelling (IN_TRANSIT)
     */
    public int inTransitQty(String supplyId) {
        int sum = 0;
        for (Delivery d : world.deliveries.all()) {
            if (d.getStatus() == DeliveryStatus.IN_TRANSIT && d.getSupplyId().equals(supplyId)) {
                sum += d.getQty();
            }
        }
        return sum;
    }

    /** @return true if no open delivery involves the base as source or destination */
    public boolean baseIsIdle(String baseId) {
        for (Delivery d : world.deliveries.all()) {
            if (d.getStatus().isOpen()
                    && (d.getSourceBaseId().equals(baseId) || d.getDestBaseId().equals(baseId))) {
                return false;
            }
        }
        return true;
    }
}
