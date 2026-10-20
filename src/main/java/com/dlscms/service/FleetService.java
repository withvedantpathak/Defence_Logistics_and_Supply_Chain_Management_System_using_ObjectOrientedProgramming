package com.dlscms.service;

import com.dlscms.exception.InvalidIdException;
import com.dlscms.exception.LogisticsException;
import com.dlscms.model.Base;
import com.dlscms.model.Supply;
import com.dlscms.model.SupplyCategory;
import com.dlscms.model.Trackable;
import com.dlscms.model.vehicle.Vehicle;
import com.dlscms.model.vehicle.VehicleStatus;
import com.dlscms.strategy.VehicleOption;
import java.util.ArrayList;
import java.util.List;

/** Looks up vehicles and evaluates which free vehicles could make a trip. */
public final class FleetService {
    private final World world;

    /** @param world the shared network state */
    public FleetService(World world) {
        this.world = world;
    }

    /** @return the whole fleet ordered by id */
    public List<Vehicle> all() {
        return world.vehicles.all();
    }

    /**
     * @param id vehicle id
     * @return the vehicle
     * @throws InvalidIdException if unknown
     */
    public Vehicle get(String id) throws InvalidIdException {
        return world.vehicles.get(id);
    }

    /**
     * @param category only vehicles able to carry this category, or null for any
     * @return available vehicles ordered by id
     */
    public List<Vehicle> available(SupplyCategory category) {
        return world.vehicles.filter(v -> v.getStatus() == VehicleStatus.AVAILABLE
                && (category == null || v.canCarry(category)));
    }

    /**
     * Evaluates every free vehicle that can carry the supply for a trip source to destination.
     *
     * @param supply the supply to carry
     * @param source donor base
     * @param dest   destination base
     * @return options ordered by vehicle id (only vehicles that can carry at least one unit)
     * @throws LogisticsException if a vehicle refers to an unknown base
     */
    public List<VehicleOption> options(Supply supply, Base source, Base dest) throws LogisticsException {
        List<VehicleOption> out = new ArrayList<>();
        double haul = source.distanceKm(dest);
        for (Vehicle v : available(supply.getCategory())) {
            int units = v.unitsPerTrip(supply.getCategory(), supply.getUnitWeightKg());
            if (units < 1) {
                continue;
            }
            Base at = world.bases.get(v.getCurrentBaseId());
            double positioning = at.distanceKm(source);
            double eta = v.estimateHours(positioning + haul, supply.getCategory());
            out.add(new VehicleOption(v, positioning, haul, units, eta));
        }
        return out;
    }

    /**
     * Takes a vehicle out of or back into service.
     *
     * @param id vehicle id
     * @param on true for maintenance
     * @return the vehicle
     * @throws LogisticsException if unknown or busy
     */
    public Vehicle setMaintenance(String id, boolean on) throws LogisticsException {
        Vehicle v = get(id);
        v.setMaintenance(on);
        world.setDirty(true);
        return v;
    }

    /**
     * Generic helper (bounded type parameter): keeps only items with the given status label.
     *
     * @param items  trackable items
     * @param status status label such as AVAILABLE
     * @param <T>    any {@link Trackable} type
     * @return matching items
     */
    public static <T extends Trackable> List<T> withStatus(List<T> items, String status) {
        List<T> out = new ArrayList<>();
        for (T t : items) {
            if (t.statusLabel().equalsIgnoreCase(status)) {
                out.add(t);
            }
        }
        return out;
    }
}
