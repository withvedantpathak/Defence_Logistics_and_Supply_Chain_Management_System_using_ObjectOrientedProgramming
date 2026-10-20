package com.dlscms.model;

import com.dlscms.exception.InvalidStateTransitionException;

/** Something that moves through the dispatch, arrive, fail life cycle (deliveries). */
public interface Deliverable {
    /**
     * Starts the trip.
     *
     * @param hour simulated hour
     * @throws InvalidStateTransitionException if not PLANNED
     */
    void dispatch(long hour) throws InvalidStateTransitionException;

    /**
     * Marks arrival.
     *
     * @param hour simulated hour of arrival
     * @throws InvalidStateTransitionException if not IN_TRANSIT
     */
    void complete(long hour) throws InvalidStateTransitionException;

    /**
     * Marks failure.
     *
     * @param reason why it failed
     * @param hour   simulated hour
     * @throws InvalidStateTransitionException if already finished
     */
    void fail(String reason, long hour) throws InvalidStateTransitionException;
}
