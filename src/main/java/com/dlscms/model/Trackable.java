package com.dlscms.model;

/** Something whose identity, status and position the operator can follow (vehicles, deliveries). */
public interface Trackable {
    /** @return unique id */
    String id();

    /** @return short status text, e.g. IN_TRANSIT */
    String statusLabel();

    /** @return where it is or where it is going, as text */
    String locationLabel();
}
