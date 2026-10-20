package com.dlscms.exception;

/** Raised when user input or a model invariant is violated (blank id, negative quantity, bad coordinates...). */
public class ValidationException extends LogisticsException {
    private static final long serialVersionUID = 1L;

    /** @param message explanation shown to the operator */
    public ValidationException(String message) {
        super(message);
    }
}
