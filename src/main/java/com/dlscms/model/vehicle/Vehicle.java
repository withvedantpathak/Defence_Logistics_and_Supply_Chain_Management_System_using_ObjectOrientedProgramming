package com.dlscms.model.vehicle;

import com.dlscms.exception.InvalidStateTransitionException;
import com.dlscms.exception.ValidationException;
import com.dlscms.model.Persistable;
import com.dlscms.model.SupplyCategory;
import com.dlscms.model.Trackable;
import com.dlscms.util.Csv;
import com.dlscms.util.Validator;

/**
 * Abstract base of all vehicles. Subclasses decide what they can carry and how fast/how much
 * (polymorphism); the travel-time calculation is a template method built from overridable hooks.
 */
public abstract class Vehicle implements Trackable, Persistable {
    /** CSV header of vehicles.csv. */
    public static final String HEADER =
            "id,type,home_base,current_base,capacity_kg,speed_kmph,status";

    private final String id;
    private final String homeBaseId;
    private String currentBaseId;
    private final int capacityKg;
    private final int speedKmph;
    private VehicleStatus status;

    /**
     * @param id            unique id such as TR-01
     * @param homeBaseId    base the vehicle belongs to
     * @param currentBaseId base where it is now
     * @param capacityKg    rated payload in kg
     * @param speedKmph     cruising speed in km/h
     * @param status        initial status
     * @throws ValidationException if any value is invalid
     */
    protected Vehicle(String id, String homeBaseId, String currentBaseId, int capacityKg, int speedKmph,
                      VehicleStatus status) throws ValidationException {
        this.id = Validator.id(id, "vehicle id");
        this.homeBaseId = Validator.id(homeBaseId, "home base");
        this.currentBaseId = Validator.id(currentBaseId, "current base");
        this.capacityKg = Validator.positive(capacityKg, "capacity");
        this.speedKmph = Validator.positive(speedKmph, "speed");
        if (status == null) {
            throw new ValidationException("vehicle status is required");
        }
        this.status = status;
    }

    // ---- abstract behaviour every vehicle type must define ----

    /** @return type name used in files and screens, e.g. "Truck" */
    public abstract String typeName();

    /**
     * @param category supply category
     * @return true if this vehicle type is able to carry that category
     */
    public abstract boolean canCarry(SupplyCategory category);

    // ---- overridable hooks (defaults here; subclasses specialise them) ----

    /**
     * @param category supply category
     * @return usable payload in kg for that category
     */
    public double capacityFor(SupplyCategory category) {
        return capacityKg;
    }

    /**
     * @param category supply category
     * @return speed in km/h for that category
     */
    public double effectiveSpeedKmph(SupplyCategory category) {
        return speedKmph;
    }

    /** @return hours spent loading and unloading */
    public double loadingHours() {
        return 1.0;
    }

    /** @return multiplier turning straight-line distance into travelled distance */
    public double roadFactor() {
        return 1.25;
    }

    /**
     * Template method: travel time = travelled distance / speed + loading time.
     *
     * @param km       straight-line distance
     * @param category what is carried
     * @return hours
     */
    public final double estimateHours(double km, SupplyCategory category) {
        return km * roadFactor() / effectiveSpeedKmph(category) + loadingHours();
    }

    /**
     * @param category   supply category
     * @param unitWeight weight of one unit in kg
     * @return whole units that fit in one trip (0 if none or if the category cannot be carried)
     */
    public int unitsPerTrip(SupplyCategory category, double unitWeight) {
        if (!canCarry(category)) {
            return 0;
        }
        return (int) Math.floor(capacityFor(category) / unitWeight);
    }

    // ---- state changes ----

    /** @throws InvalidStateTransitionException unless AVAILABLE */
    public void assign() throws InvalidStateTransitionException {
        require(status == VehicleStatus.AVAILABLE, "assign");
        status = VehicleStatus.ASSIGNED;
    }

    /** @throws InvalidStateTransitionException unless ASSIGNED */
    public void depart() throws InvalidStateTransitionException {
        require(status == VehicleStatus.ASSIGNED, "depart");
        status = VehicleStatus.IN_TRANSIT;
    }

    /**
     * Frees the vehicle at a base (after arrival or after a cancelled/failed trip).
     *
     * @param baseId base where it now is
     * @throws InvalidStateTransitionException if in maintenance
     */
    public void release(String baseId) throws InvalidStateTransitionException {
        require(status == VehicleStatus.ASSIGNED || status == VehicleStatus.IN_TRANSIT, "release");
        currentBaseId = baseId;
        status = VehicleStatus.AVAILABLE;
    }

    /**
     * @param on true to take out of service, false to return it
     * @throws InvalidStateTransitionException if busy
     */
    public void setMaintenance(boolean on) throws InvalidStateTransitionException {
        if (on) {
            require(status == VehicleStatus.AVAILABLE, "enter maintenance");
            status = VehicleStatus.MAINTENANCE;
        } else {
            require(status == VehicleStatus.MAINTENANCE, "leave maintenance");
            status = VehicleStatus.AVAILABLE;
        }
    }

    private void require(boolean ok, String action) throws InvalidStateTransitionException {
        if (!ok) {
            throw new InvalidStateTransitionException("Vehicle " + id + " is " + status + "; cannot " + action);
        }
    }

    // ---- accessors ----

    /** @return vehicle id */
    public String getId() {
        return id;
    }

    /** @return home base id */
    public String getHomeBaseId() {
        return homeBaseId;
    }

    /** @return base where the vehicle currently is */
    public String getCurrentBaseId() {
        return currentBaseId;
    }

    /** @return rated payload in kg */
    public int getCapacityKg() {
        return capacityKg;
    }

    /** @return cruising speed in km/h */
    public int getSpeedKmph() {
        return speedKmph;
    }

    /** @return status */
    public VehicleStatus getStatus() {
        return status;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String statusLabel() {
        return status.name();
    }

    @Override
    public String locationLabel() {
        return currentBaseId;
    }

    @Override
    public String csvHeader() {
        return HEADER;
    }

    @Override
    public String toCsvRow() {
        return Csv.join(id, typeName(), homeBaseId, currentBaseId, capacityKg, speedKmph, status);
    }

    /**
     * Factory used when reading CSV files.
     *
     * @param f fields in header order
     * @return the right vehicle subclass
     * @throws ValidationException if the type or any value is invalid
     */
    public static Vehicle fromCsv(String[] f) throws ValidationException {
        VehicleStatus st;
        try {
            st = VehicleStatus.valueOf(f[6].trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("unknown vehicle status '" + f[6] + "'");
        }
        int cap = Validator.parseInt(f[4], "capacity_kg");
        int speed = Validator.parseInt(f[5], "speed_kmph");
        switch (f[1].trim()) {
            case "Truck":
                return new Truck(f[0], f[2], f[3], cap, speed, st);
            case "FuelTruck":
                return new FuelTruck(f[0], f[2], f[3], cap, speed, st);
            case "MedicalVan":
                return new MedicalVan(f[0], f[2], f[3], cap, speed, st);
            case "Helicopter":
                return new Helicopter(f[0], f[2], f[3], cap, speed, st);
            default:
                throw new ValidationException("unknown vehicle type '" + f[1] + "'");
        }
    }

    @Override
    public String toString() {
        return id + " (" + typeName() + ")";
    }
}
