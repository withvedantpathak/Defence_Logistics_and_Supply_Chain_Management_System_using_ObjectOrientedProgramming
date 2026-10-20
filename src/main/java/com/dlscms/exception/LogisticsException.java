package com.dlscms.exception;

/** Checked base type of every domain error raised by the logistics system. */
public class LogisticsException extends Exception {
    private static final long serialVersionUID = 1L;

    /** @param message human-readable explanation shown to the operator */
    public LogisticsException(String message) {
        super(message);
    }

    /**
     * @param message human-readable explanation
     * @param cause   underlying exception
     */
    public LogisticsException(String message, Throwable cause) {
        super(message, cause);
    }
}
