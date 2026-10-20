package com.dlscms.exception;

/** Raised when a CSV data file is missing, unreadable or malformed. */
public class DataFormatException extends LogisticsException {
    private static final long serialVersionUID = 1L;

    /** @param message explanation including file name and line number */
    public DataFormatException(String message) {
        super(message);
    }

    /**
     * @param message explanation including file name and line number
     * @param cause   underlying exception
     */
    public DataFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
