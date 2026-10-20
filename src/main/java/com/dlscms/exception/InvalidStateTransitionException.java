package com.dlscms.exception;

/** Raised when a lifecycle change (request, delivery, vehicle) is not allowed from the current state. */
public class InvalidStateTransitionException extends LogisticsException {
    private static final long serialVersionUID = 1L;

    /** @param message explanation shown to the operator */
    public InvalidStateTransitionException(String message) {
        super(message);
    }
}
