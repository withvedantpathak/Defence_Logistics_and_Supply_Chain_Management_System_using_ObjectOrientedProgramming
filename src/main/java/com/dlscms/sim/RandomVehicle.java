package com.dlscms.sim;

import com.dlscms.strategy.VehicleAssignmentStrategy;
import com.dlscms.strategy.VehicleOption;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/** Baseline vehicle choice: any eligible vehicle, picked at random (seeded). */
public final class RandomVehicle implements VehicleAssignmentStrategy {
    private final Random rnd;

    /** @param seed random seed so runs are reproducible */
    public RandomVehicle(long seed) {
        this.rnd = new Random(seed);
    }

    @Override
    public String name() {
        return "RANDOM";
    }

    @Override
    public Optional<VehicleOption> select(List<VehicleOption> options, int qtyWanted) {
        return options.isEmpty() ? Optional.empty() : Optional.of(options.get(rnd.nextInt(options.size())));
    }
}
