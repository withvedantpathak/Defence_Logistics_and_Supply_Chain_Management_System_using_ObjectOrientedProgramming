package com.dlscms.service;

import com.dlscms.model.Base;
import com.dlscms.model.Delivery;
import com.dlscms.model.Inventory;
import com.dlscms.model.Supply;
import com.dlscms.model.SupplyRequest;
import com.dlscms.model.vehicle.Vehicle;
import com.dlscms.util.IdGenerator;
import java.util.Map;
import java.util.TreeMap;

/** The complete mutable state of the logistics network (all registries, clock and id counters). */
public final class World {
    /** All bases. */
    public final Registry<Base> bases = new Registry<>("base", Base::getId);
    /** The supply catalogue. */
    public final Registry<Supply> supplies = new Registry<>("supply", Supply::getId);
    /** Stock at every base. */
    public final Inventory inventory = new Inventory();
    /** The fleet. */
    public final Registry<Vehicle> vehicles = new Registry<>("vehicle", Vehicle::getId);
    /** All supply requests. */
    public final Registry<SupplyRequest> requests = new Registry<>("request", SupplyRequest::getId);
    /** All deliveries. */
    public final Registry<Delivery> deliveries = new Registry<>("delivery", Delivery::getId);
    /** Simulated clock. */
    public final SimClock clock = new SimClock();
    /** Request id source. */
    public final IdGenerator requestIds = new IdGenerator("R-", 4);
    /** Delivery id source. */
    public final IdGenerator deliveryIds = new IdGenerator("D-", 4);
    /** Extra metadata preserved when saving (seed, scale ...). */
    public final Map<String, String> extraMeta = new TreeMap<>();

    private String sourceStrategyName = "NEAREST";
    private String vehicleStrategyName = "FASTEST";
    private boolean dirty;

    /** @return name of the active source-selection strategy */
    public String getSourceStrategyName() {
        return sourceStrategyName;
    }

    /** @param n strategy name */
    public void setSourceStrategyName(String n) {
        this.sourceStrategyName = n;
    }

    /** @return name of the active vehicle-assignment strategy */
    public String getVehicleStrategyName() {
        return vehicleStrategyName;
    }

    /** @param n strategy name */
    public void setVehicleStrategyName(String n) {
        this.vehicleStrategyName = n;
    }

    /** @return true if there are changes not yet saved */
    public boolean isDirty() {
        return dirty;
    }

    /** @param d new dirty flag */
    public void setDirty(boolean d) {
        this.dirty = d;
    }
}
