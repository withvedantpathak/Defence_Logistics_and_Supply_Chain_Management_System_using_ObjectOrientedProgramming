package com.dlscms.util;

import com.dlscms.exception.ValidationException;

/** Input and invariant checks that throw {@link ValidationException}. */
public final class Validator {
    private Validator() { }

    /**
     * Requires a non-blank identifier without spaces or commas; normalises to upper case.
     *
     * @param v     raw value
     * @param field field name for the error message
     * @return trimmed upper-case id
     * @throws ValidationException if invalid
     */
    public static String id(String v, String field) throws ValidationException {
        if (v == null || v.isBlank()) {
            throw new ValidationException(field + " must not be blank");
        }
        String t = v.trim().toUpperCase();
        if (t.matches(".*[\\s,\"].*")) {
            throw new ValidationException(field + " must not contain spaces, commas or quotes: " + v);
        }
        return t;
    }

    /**
     * Requires non-blank text.
     *
     * @param v     raw value
     * @param field field name
     * @return trimmed text
     * @throws ValidationException if blank
     */
    public static String text(String v, String field) throws ValidationException {
        if (v == null || v.isBlank()) {
            throw new ValidationException(field + " must not be blank");
        }
        return v.trim();
    }

    /**
     * @param v     value
     * @param field field name
     * @return v if greater than zero
     * @throws ValidationException otherwise
     */
    public static int positive(int v, String field) throws ValidationException {
        if (v <= 0) {
            throw new ValidationException(field + " must be a positive whole number, got " + v);
        }
        return v;
    }

    /**
     * @param v     value
     * @param field field name
     * @return v if zero or more
     * @throws ValidationException otherwise
     */
    public static int nonNegative(int v, String field) throws ValidationException {
        if (v < 0) {
            throw new ValidationException(field + " must not be negative, got " + v);
        }
        return v;
    }

    /**
     * @param v     value
     * @param field field name
     * @return v if greater than zero
     * @throws ValidationException otherwise
     */
    public static double positive(double v, String field) throws ValidationException {
        if (!(v > 0)) {
            throw new ValidationException(field + " must be positive, got " + v);
        }
        return v;
    }

    /**
     * @param v     value
     * @param lo    inclusive lower bound
     * @param hi    inclusive upper bound
     * @param field field name
     * @return v if within range
     * @throws ValidationException otherwise
     */
    public static double range(double v, double lo, double hi, String field) throws ValidationException {
        if (Double.isNaN(v) || v < lo || v > hi) {
            throw new ValidationException(field + " must be between " + lo + " and " + hi + ", got " + v);
        }
        return v;
    }

    /**
     * Parses a whole number.
     *
     * @param s     text
     * @param field field name
     * @return the number
     * @throws ValidationException if not a number
     */
    public static int parseInt(String s, String field) throws ValidationException {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException | NullPointerException e) {
            throw new ValidationException(field + " must be a whole number, got '" + s + "'");
        }
    }

    /**
     * Parses a decimal number.
     *
     * @param s     text
     * @param field field name
     * @return the number
     * @throws ValidationException if not a number
     */
    public static double parseDouble(String s, String field) throws ValidationException {
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException | NullPointerException e) {
            throw new ValidationException(field + " must be a number, got '" + s + "'");
        }
    }
}
