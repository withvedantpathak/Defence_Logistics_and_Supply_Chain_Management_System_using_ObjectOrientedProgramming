package com.dlscms.model.vehicle;

import com.dlscms.exception.ValidationException;
import com.dlscms.model.SupplyCategory;

/** Tanker: carries fuel only and takes longer to load and unload. */
public final class FuelTruck extends Vehicle {

    /**
     * @param id            vehicle id
     * @param homeBaseId    home base
     * @param currentBaseId current base
     * @param capacityKg    payload in kg
     * @param speedKmph     speed in km/h
     * @param status        initial status
     * @throws ValidationException if invalid
     */
    public FuelTruck(String id, String homeBaseId, String currentBaseId, int capacityKg, int speedKmph,
            VehicleStatus status) throws ValidationException {
        super(id, homeBaseId, currentBaseId, capacityKg, speedKmph, status);
    }

    @Override
    public String typeName() {
        return "FuelTruck";
    }

    @Override
    public boolean canCarry(SupplyCategory category) {
        return category == SupplyCategory.FUEL;
    }

    @Override
    public double loadingHours() {
        return 1.5;
    }
}
