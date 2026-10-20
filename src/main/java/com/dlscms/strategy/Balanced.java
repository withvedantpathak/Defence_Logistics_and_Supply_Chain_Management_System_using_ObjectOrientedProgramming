package com.dlscms.strategy;

import com.dlscms.model.SupplyRequest;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Scores donors 50% on closeness and 50% on spare stock (both normalised to 0..1). */
public final class Balanced implements SourceSelectionStrategy {
    @Override
    public String name() {
        return "BALANCED";
    }

    @Override
    public Optional<SourceCandidate> select(SupplyRequest request, List<SourceCandidate> candidates) {
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        double maxDist = candidates.stream().mapToDouble(SourceCandidate::distanceKm).max().orElse(1);
        int maxSurplus = candidates.stream().mapToInt(SourceCandidate::surplus).max().orElse(1);
        double md = maxDist <= 0 ? 1 : maxDist;
        double ms = maxSurplus <= 0 ? 1 : maxSurplus;
        return candidates.stream().max(Comparator
                .comparingDouble((SourceCandidate c) -> 0.5 * (1 - c.distanceKm() / md) + 0.5 * (c.surplus() / ms))
                .thenComparing(c -> c.base().getId(), Comparator.reverseOrder()));
    }
}
