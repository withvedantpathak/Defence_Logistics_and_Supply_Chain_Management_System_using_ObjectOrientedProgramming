package com.dlscms.exception;

/** Raised when an id does not refer to any known entity. */
public class InvalidIdException extends LogisticsException {
    private static final long serialVersionUID = 1L;

    /**
     * @param kind entity kind, e.g. "base" or "request"
     * @param id   the unknown id
     */
    public InvalidIdException(String kind, String id) {
        super("Unknown " + kind + " id " + id);
    }
}
