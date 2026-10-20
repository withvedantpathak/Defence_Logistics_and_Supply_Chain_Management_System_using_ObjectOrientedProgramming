package com.dlscms.strategy;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Picks the smallest vehicle that carries the whole load; if none can, the biggest one available. */
public final class SmallestFit implements VehicleAssignmentStrategy {
    @Override
    public String name() {
        return "SMALLEST";
    }

    @Override
    public Optional<VehicleOption> select(List<VehicleOption> options, int qtyWanted) {
        Comparator<VehicleOption> tie = Comparator.comparingDouble(VehicleOption::etaHours)
                .thenComparing(o -> o.vehicle().getId());
        Optional<VehicleOption> fit = options.stream().filter(o -> o.unitsPerTrip() >= qtyWanted)
                .min(Comparator.comparingInt(VehicleOption::unitsPerTrip).thenComparing(tie));
        if (fit.isPresent()) {
            return fit;
        }
        return options.stream().max(Comparator.comparingInt(VehicleOption::unitsPerTrip)
                .thenComparing(tie.reversed()));
    }
}
