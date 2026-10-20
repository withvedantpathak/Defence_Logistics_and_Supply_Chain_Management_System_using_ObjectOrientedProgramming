package com.dlscms.strategy;

import com.dlscms.model.SupplyRequest;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Chooses the closest donor (ties broken by base id) - favours short delivery times. */
public final class NearestFirst implements SourceSelectionStrategy {
    @Override
    public String name() {
        return "NEAREST";
    }

    @Override
    public Optional<SourceCandidate> select(SupplyRequest request, List<SourceCandidate> candidates) {
        return candidates.stream().min(Comparator.comparingDouble(SourceCandidate::distanceKm)
                .thenComparing(c -> c.base().getId()));
    }
}
