package com.dlscms.strategy;

import com.dlscms.model.vehicle.Vehicle;

/**
 * A free vehicle evaluated for one planned trip.
 *
 * @param vehicle       the vehicle
 * @param positioningKm distance from where the vehicle is to the donor base
 * @param haulKm        distance donor to destination
 * @param unitsPerTrip  units it can carry in one trip
 * @param etaHours      total hours from now until arrival (positioning + haul + loading)
 */
public record VehicleOption(Vehicle vehicle, double positioningKm, double haulKm, int unitsPerTrip,
                            double etaHours) { }
