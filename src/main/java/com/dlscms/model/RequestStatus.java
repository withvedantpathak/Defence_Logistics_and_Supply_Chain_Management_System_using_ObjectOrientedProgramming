package com.dlscms.model;

/** Lifecycle of a supply request. */
public enum RequestStatus {
    NEW, APPROVED, FULFILLING, FULFILLED, REJECTED, CANCELLED;

    /** @return true if no further change is possible */
    public boolean isFinal() {
        return this == FULFILLED || this == REJECTED || this == CANCELLED;
    }
}
