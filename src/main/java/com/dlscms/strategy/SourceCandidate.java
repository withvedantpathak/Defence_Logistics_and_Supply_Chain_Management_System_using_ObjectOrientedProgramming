package com.dlscms.strategy;

import com.dlscms.model.Base;

/**
 * A base that could donate stock for a request.
 *
 * @param base       the donor base
 * @param surplus    units it may give (depends on request priority)
 * @param distanceKm straight-line distance to the requesting base
 */
public record SourceCandidate(Base base, int surplus, double distanceKm) { }
