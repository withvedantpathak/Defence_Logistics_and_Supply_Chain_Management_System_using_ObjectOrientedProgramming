package com.dlscms.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dlscms.exception.DataFormatException;
import com.dlscms.model.Base;
import com.dlscms.model.Location;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class CsvRepositoryTest {
    private static CsvRepository<Base> repo() {
        RowParser<Base> parser = Base::fromCsv;
        return new CsvRepository<>("bases.csv", Base.HEADER, parser);
    }

    private static Path tmp() throws Exception {
        return Files.createTempDirectory("dlscms-repo");
    }

    @Test
    public void roundTripKeepsOrderAndLeavesNoTempFile() throws Exception {
        Path dir = tmp();
        List<Base> in = new ArrayList<>();
        in.add(new Base("AAA", "Alpha, Base", new Location("City", 12.5, 70.25), 900));     // comma in name is escaped
        in.add(new Base("BBB", "Bravo", new Location("City", -5, 170), 100));
        repo().save(dir, in);
        assertFalse(Files.exists(dir.resolve("bases.csv.tmp")));
        List<Base> out = repo().load(dir, false);
        assertEquals(2, out.size());
        assertEquals("Alpha, Base", out.get(0).getName());
        assertEquals(in.get(1).toCsvRow(), out.get(1).toCsvRow());
        repo().save(dir, out);          // overwrite works
        assertEquals(2, repo().load(dir, false).size());
    }

    @Test
    public void optionalMissingFileGivesEmptyListRequiredThrows() throws Exception {
        Path dir = tmp();
        assertTrue(repo().load(dir, true).isEmpty());
        try {
            repo().load(dir, false);
            fail();
        } catch (DataFormatException e) {
            assertTrue(e.getMessage().startsWith("Missing data file bases.csv"));
        }
    }

    @Test
    public void badHeaderColumnsAndValuesNameTheLine() throws Exception {
        Path dir = tmp();
        Files.writeString(dir.resolve("bases.csv"), "wrong,header\nA,B\n");
        try {
            repo().load(dir, false);
            fail();
        } catch (DataFormatException e) {
            assertTrue(e.getMessage().startsWith("Bad header in bases.csv"));
        }
        Files.writeString(dir.resolve("bases.csv"), Base.HEADER + "\nAAA,Alpha,Pune\n");
        try {
            repo().load(dir, false);
            fail();
        } catch (DataFormatException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("line 2") && e.getMessage().contains("columns"));
        }
        Files.writeString(dir.resolve("bases.csv"), Base.HEADER + "\nAAA,Alpha,Pune,95.0,70.0,500\n");
        try {
            repo().load(dir, false);
            fail();
        } catch (DataFormatException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("bases.csv line 2"));
        }
    }

    @Test
    public void blankLinesAndBomAreTolerated() throws Exception {
        Path dir = tmp();
        Files.writeString(dir.resolve("bases.csv"), "﻿" + Base.HEADER + "\n\nAAA,Alpha,Pune,18.5,73.8,500\n\n");
        assertEquals(1, repo().load(dir, false).size());
    }

    @Test
    public void rowParserIsAFunctionalInterface() throws Exception {
        RowParser<String> p = f -> f[0] + "!";
        assertEquals("x!", p.parse(new String[] {"x"}));
    }
}
