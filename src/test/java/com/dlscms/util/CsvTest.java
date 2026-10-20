package com.dlscms.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CsvTest {
    @Test
    public void roundTripWithCommasAndQuotes() {
        String line = Csv.join("a", "b,c", "say \"hi\"", 5, null);
        assertArrayEquals(new String[] {"a", "b,c", "say \"hi\"", "5", ""}, Csv.split(line));
    }

    @Test
    public void plainFieldsNotQuoted() {
        assertEquals("x,y,3", Csv.join("x", "y", 3));
    }

    @Test
    public void emptyLineGivesOneEmptyField() {
        assertArrayEquals(new String[] {""}, Csv.split(""));
    }
}
