package com.dlscms.exception;

/** Raised when stock exists but no suitable vehicle is free. */
public class NoVehicleAvailableException extends LogisticsException {
    private static final long serialVersionUID = 1L;

    /** @param message explanation shown to the operator */
    public NoVehicleAvailableException(String message) {
        super(message);
    }
}
