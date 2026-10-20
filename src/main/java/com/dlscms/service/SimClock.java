package com.dlscms.service;

/** Simulated clock counted in whole hours since the start of the scenario. */
public final class SimClock {
    private long hour;

    /** @return current simulated hour */
    public long now() {
        return hour;
    }

    /** @param h hour to jump to (not below zero) */
    public void set(long h) {
        hour = Math.max(0, h);
    }

    /** @param hours hours to advance (negative values are ignored) */
    public void advance(long hours) {
        if (hours > 0) {
            hour += hours;
        }
    }

    /**
     * @param h simulated hour
     * @return text such as "DAY 03 14:00"
     */
    public static String format(long h) {
        return String.format("DAY %02d %02d:00", h / 24, h % 24);
    }

    /** @return the current time formatted */
    public String text() {
        return format(hour);
    }
}
