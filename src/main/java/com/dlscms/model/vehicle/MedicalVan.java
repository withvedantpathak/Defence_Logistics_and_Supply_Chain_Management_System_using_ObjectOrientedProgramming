package com.dlscms.model.vehicle;

import com.dlscms.exception.ValidationException;
import com.dlscms.model.SupplyCategory;

/** Fast van for medical and food supplies; food loads use 70% of the payload (cold-chain space). */
public final class MedicalVan extends Vehicle {

    /**
     * @param id            vehicle id
     * @param homeBaseId    home base
     * @param currentBaseId current base
     * @param capacityKg    payload in kg
     * @param speedKmph     speed in km/h
     * @param status        initial status
     * @throws ValidationException if invalid
     */
    public MedicalVan(String id, String homeBaseId, String currentBaseId, int capacityKg, int speedKmph,
            VehicleStatus status) throws ValidationException {
        super(id, homeBaseId, currentBaseId, capacityKg, speedKmph, status);
    }

    @Override
    public String typeName() {
        return "MedicalVan";
    }

    @Override
    public boolean canCarry(SupplyCategory category) {
        return category == SupplyCategory.MEDICAL || category == SupplyCategory.FOOD;
    }

    @Override
    public double capacityFor(SupplyCategory category) {
        return category == SupplyCategory.FOOD ? getCapacityKg() * 0.7 : getCapacityKg();
    }

    @Override
    public double loadingHours() {
        return 0.5;
    }
}
