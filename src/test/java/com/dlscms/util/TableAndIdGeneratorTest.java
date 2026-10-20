package com.dlscms.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TableAndIdGeneratorTest {
    @Test
    public void idsAreSequentialAndPadded() {
        IdGenerator g = new IdGenerator("R-", 4);
        assertEquals("R-0001", g.next());
        assertEquals("R-0002", g.next());
        g.setNext(10);
        assertEquals("R-0010", g.next());
    }

    @Test
    public void tableExportsCsvAndStyles() {
        Table t = new Table("demo", "a", "b").add("x", 1).add("y,z", 2);
        t.style(1, s -> s.equals("2") ? Style.BAD : Style.GOOD);
        assertEquals("a,b\nx,1\n\"y,z\",2\n", t.toCsv());
        assertEquals(Style.BAD, t.styleOf(1, "2"));
        assertEquals(2, t.size());
        assertTrue(Table.isNumeric("1,200"));
        assertTrue(Table.isNumeric("82%"));
    }
}
