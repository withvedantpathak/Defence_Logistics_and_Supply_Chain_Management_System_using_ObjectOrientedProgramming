package com.dlscms.strategy;

import com.dlscms.model.SupplyRequest;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Chooses the donor with the most spare stock (ties: nearer, then id) - protects small donors. */
public final class MostSurplus implements SourceSelectionStrategy {
    @Override
    public String name() {
        return "SURPLUS";
    }

    @Override
    public Optional<SourceCandidate> select(SupplyRequest request, List<SourceCandidate> candidates) {
        return candidates.stream().max(Comparator.comparingInt(SourceCandidate::surplus)
                .thenComparing(Comparator.comparingDouble(SourceCandidate::distanceKm).reversed())
                .thenComparing(c -> c.base().getId(), Comparator.reverseOrder()));
    }
}
