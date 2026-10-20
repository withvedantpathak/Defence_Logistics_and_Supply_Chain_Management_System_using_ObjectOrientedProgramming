package com.dlscms.model;

import com.dlscms.exception.ValidationException;

/** Urgency of a supply request. Higher rank is served first. */
public enum Priority {
    ROUTINE(1), URGENT(2), CRITICAL(3);

    private final int rank;

    Priority(int rank) {
        this.rank = rank;
    }

    /** @return numeric rank (3 = most urgent) */
    public int rank() {
        return rank;
    }

    /**
     * Parses a priority name, case-insensitively.
     *
     * @param s text
     * @return the priority
     * @throws ValidationException if unknown
     */
    public static Priority parse(String s) throws ValidationException {
        try {
            return valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ValidationException("Priority must be ROUTINE, URGENT or CRITICAL, got '" + s + "'");
        }
    }
}
