package com.dlscms.strategy;

import java.util.List;
import java.util.Optional;

/** Rule for choosing which free vehicle makes a trip (Strategy pattern). */
public interface VehicleAssignmentStrategy {
    /** @return short name used by the STRAT command and in reports */
    String name();

    /**
     * @param options   free vehicles able to carry the supply (each can carry at least one unit)
     * @param qtyWanted units we would like to move in this trip
     * @return the chosen option, or empty if the list is empty
     */
    Optional<VehicleOption> select(List<VehicleOption> options, int qtyWanted);
}
