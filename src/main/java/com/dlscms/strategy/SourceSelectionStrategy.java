package com.dlscms.strategy;

import com.dlscms.model.SupplyRequest;
import java.util.List;
import java.util.Optional;

/** Rule for choosing which donor base supplies a request (Strategy pattern; implementations are polymorphic). */
public interface SourceSelectionStrategy {
    /** @return short name used by the STRAT command and in reports */
    String name();

    /**
     * @param request    the request being served
     * @param candidates donors with spare stock (never contains the requester)
     * @return the chosen donor, or empty if none is acceptable
     */
    Optional<SourceCandidate> select(SupplyRequest request, List<SourceCandidate> candidates);
}
