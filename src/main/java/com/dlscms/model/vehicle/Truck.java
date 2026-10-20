package com.dlscms.model.vehicle;

import com.dlscms.exception.ValidationException;
import com.dlscms.model.SupplyCategory;

/** General-purpose road truck: carries everything except fuel; bulky shelter items use only 80% of the payload. */
public final class Truck extends Vehicle {

    /**
     * @param id            vehicle id
     * @param homeBaseId    home base
     * @param currentBaseId current base
     * @param capacityKg    payload in kg
     * @param speedKmph     speed in km/h
     * @param status        initial status
     * @throws ValidationException if invalid
     */
    public Truck(String id, String homeBaseId, String currentBaseId, int capacityKg, int speedKmph,
            VehicleStatus status) throws ValidationException {
        super(id, homeBaseId, currentBaseId, capacityKg, speedKmph, status);
    }

    @Override
    public String typeName() {
        return "Truck";
    }

    @Override
    public boolean canCarry(SupplyCategory category) {
        return category != SupplyCategory.FUEL;
    }

    @Override
    public double capacityFor(SupplyCategory category) {
        return category == SupplyCategory.SHELTER ? getCapacityKg() * 0.8 : getCapacityKg();
    }
}
