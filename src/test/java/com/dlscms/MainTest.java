package com.dlscms;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MainTest {
    @Test
    public void nameMentionsLogistics() {
        assertTrue(Main.NAME.contains("Logistics"));
    }
}
