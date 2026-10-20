package com.dlscms.exception;

/** Raised when a stock operation would take quantity below zero or below what is reserved. */
public class InsufficientStockException extends LogisticsException {
    private static final long serialVersionUID = 1L;

    /** @param message explanation shown to the operator */
    public InsufficientStockException(String message) {
        super(message);
    }
}
