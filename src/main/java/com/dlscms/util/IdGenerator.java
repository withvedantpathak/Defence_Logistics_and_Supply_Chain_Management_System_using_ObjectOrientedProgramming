package com.dlscms.util;

/** Generates sequential ids such as R-0001. */
public final class IdGenerator {
    private final String prefix;
    private final int width;
    private int next = 1;

    /**
     * @param prefix text before the number, e.g. "R-"
     * @param width  zero-padded digits
     */
    public IdGenerator(String prefix, int width) {
        this.prefix = prefix;
        this.width = width;
    }

    /** @return the next id, advancing the counter */
    public String next() {
        return prefix + String.format("%0" + width + "d", next++);
    }

    /** @return the number the next id will use */
    public int peekNumber() {
        return next;
    }

    /** @param n the number the next id should use (must be at least 1) */
    public void setNext(int n) {
        this.next = Math.max(1, n);
    }
}
