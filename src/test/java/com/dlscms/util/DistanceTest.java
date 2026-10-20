package com.dlscms.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DistanceTest {
    @Test
    public void puneToMumbaiIsAbout120Km() {
        double km = Distance.haversineKm(18.52, 73.86, 19.08, 72.88);
        assertEquals(120, km, 3);
    }

    @Test
    public void sameLocationIsZero() {
        assertEquals(0, Distance.haversineKm(10, 10, 10, 10), 1e-9);
    }
}
