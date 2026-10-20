package com.dlscms.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dlscms.exception.ValidationException;
import org.junit.Test;

public class BaseAndSupplyTest {
    @Test
    public void baseNormalisesIdAndRoundTripsCsv() throws Exception {
        Base b = new Base("pun", "Pune Logistics Node", new Location("Pune", 18.52, 73.86), 5500);
        assertEquals("PUN", b.getId());
        Base again = Base.fromCsv(com.dlscms.util.Csv.split(b.toCsvRow()));
        assertEquals(b.toCsvRow(), again.toCsvRow());
    }

    @Test(expected = ValidationException.class)
    public void baseRejectsBadLatitude() throws Exception {
        new Base("X", "X", new Location("X", 95, 10), 100);
    }

    @Test(expected = ValidationException.class)
    public void baseRejectsZeroCapacity() throws Exception {
        new Base("X", "X", new Location("X", 10, 10), 0);
    }

    @Test
    public void distanceBetweenBases() throws Exception {
        Base p = new Base("PUN", "P", new Location("Pune", 18.52, 73.86), 100);
        Base m = new Base("MUM", "M", new Location("Mumbai", 19.08, 72.88), 100);
        assertEquals(120, p.distanceKm(m), 3);
        assertEquals(p.distanceKm(m), m.distanceKm(p), 1e-9);
    }

    @Test
    public void supplyRoundTripAndValidation() throws Exception {
        Supply s = new Supply("med-kit", "Medical Kit", SupplyCategory.MEDICAL, 2.5);
        assertEquals("MED-KIT", s.getId());
        assertEquals(s.toCsvRow(), Supply.fromCsv(com.dlscms.util.Csv.split(s.toCsvRow())).toCsvRow());
        try {
            Supply.fromCsv(new String[] {"X", "x", "WEAPONS", "1"});
            org.junit.Assert.fail("unknown category must be rejected");
        } catch (ValidationException expected) {
            assertTrue(expected.getMessage().contains("category"));
        }
    }

    @Test(expected = ValidationException.class)
    public void supplyRejectsNegativeWeight() throws Exception {
        new Supply("A", "a", SupplyCategory.FOOD, -1);
    }
}
