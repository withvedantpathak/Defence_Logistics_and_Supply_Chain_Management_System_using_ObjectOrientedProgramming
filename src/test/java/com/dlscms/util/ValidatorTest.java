package com.dlscms.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.dlscms.exception.ValidationException;
import org.junit.Test;

public class ValidatorTest {
    @Test
    public void idIsTrimmedAndUpperCased() throws Exception {
        assertEquals("PUN", Validator.id("  pun ", "id"));
    }

    @Test
    public void idRejectsBlankAndSpaces() {
        for (String bad : new String[] {"", "  ", null, "A B", "A,B"}) {
            try {
                Validator.id(bad, "id");
                fail("expected ValidationException for '" + bad + "'");
            } catch (ValidationException expected) {
                // ok
            }
        }
    }

    @Test(expected = ValidationException.class)
    public void positiveRejectsZero() throws Exception {
        Validator.positive(0, "qty");
    }

    @Test(expected = ValidationException.class)
    public void parseIntRejectsText() throws Exception {
        Validator.parseInt("12x", "qty");
    }

    @Test
    public void rangeAcceptsBounds() throws Exception {
        assertEquals(90.0, Validator.range(90, -90, 90, "lat"), 0.0);
    }

    @Test(expected = ValidationException.class)
    public void rangeRejectsNaN() throws Exception {
        Validator.range(Double.NaN, -90, 90, "lat");
    }
}
