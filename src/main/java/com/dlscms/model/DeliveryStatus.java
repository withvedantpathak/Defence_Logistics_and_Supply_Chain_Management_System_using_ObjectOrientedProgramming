package com.dlscms.model;

/** Lifecycle of a delivery. */
public enum DeliveryStatus {
    PLANNED, IN_TRANSIT, DELIVERED, FAILED;

    /** @return true while the delivery still holds stock or a vehicle */
    public boolean isOpen() {
        return this == PLANNED || this == IN_TRANSIT;
    }
}
