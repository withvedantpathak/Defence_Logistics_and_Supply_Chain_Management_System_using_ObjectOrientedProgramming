package com.dlscms.model.vehicle;

import com.dlscms.exception.ValidationException;
import com.dlscms.model.SupplyCategory;

/** Air transport: any category except fuel; flies straight (no road factor) and loads quickly. */
public final class Helicopter extends Vehicle {

    /**
     * @param id            vehicle id
     * @param homeBaseId    home base
     * @param currentBaseId current base
     * @param capacityKg    payload in kg
     * @param speedKmph     speed in km/h
     * @param status        initial status
     * @throws ValidationException if invalid
     */
    public Helicopter(String id, String homeBaseId, String currentBaseId, int capacityKg, int speedKmph,
            VehicleStatus status) throws ValidationException {
        super(id, homeBaseId, currentBaseId, capacityKg, speedKmph, status);
    }

    @Override
    public String typeName() {
        return "Helicopter";
    }

    @Override
    public boolean canCarry(SupplyCategory category) {
        return category != SupplyCategory.FUEL;
    }

    @Override
    public double roadFactor() {
        return 1.0;
    }

    @Override
    public double loadingHours() {
        return 0.25;
    }
}
