package com.dlscms.strategy;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Picks the vehicle that would arrive soonest (ties: larger load, then id). */
public final class FastestFit implements VehicleAssignmentStrategy {
    @Override
    public String name() {
        return "FASTEST";
    }

    @Override
    public Optional<VehicleOption> select(List<VehicleOption> options, int qtyWanted) {
        return options.stream().min(Comparator.comparingDouble(VehicleOption::etaHours)
                .thenComparing(Comparator.comparingInt(VehicleOption::unitsPerTrip).reversed())
                .thenComparing(o -> o.vehicle().getId()));
    }
}
