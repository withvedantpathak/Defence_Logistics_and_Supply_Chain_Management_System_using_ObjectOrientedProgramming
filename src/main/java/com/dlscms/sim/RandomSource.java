package com.dlscms.sim;

import com.dlscms.model.SupplyRequest;
import com.dlscms.strategy.SourceCandidate;
import com.dlscms.strategy.SourceSelectionStrategy;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/** Baseline "uncoordinated" donor choice: any donor with stock, picked at random (seeded). */
public final class RandomSource implements SourceSelectionStrategy {
    private final Random rnd;

    /** @param seed random seed so runs are reproducible */
    public RandomSource(long seed) {
        this.rnd = new Random(seed);
    }

    @Override
    public String name() {
        return "RANDOM";
    }

    @Override
    public Optional<SourceCandidate> select(SupplyRequest request, List<SourceCandidate> candidates) {
        return candidates.isEmpty() ? Optional.empty() : Optional.of(candidates.get(rnd.nextInt(candidates.size())));
    }
}
