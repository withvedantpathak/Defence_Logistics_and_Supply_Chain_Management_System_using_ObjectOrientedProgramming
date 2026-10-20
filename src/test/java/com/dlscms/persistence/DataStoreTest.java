package com.dlscms.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dlscms.TestData;
import com.dlscms.exception.DataFormatException;
import com.dlscms.service.World;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.junit.Test;

public class DataStoreTest {
    private static Path copyOfSmall() throws Exception {
        Path tmp = Files.createTempDirectory("dlscms-test");
        try (Stream<Path> s = Files.list(TestData.dir("small"))) {
            for (Path p : (Iterable<Path>) s::iterator) {
                Files.copy(p, tmp.resolve(p.getFileName()));
            }
        }
        return tmp;
    }

    private static void deleteTree(Path p) throws Exception {
        try (Stream<Path> s = Files.walk(p)) {
            s.sorted(Comparator.reverseOrder()).forEach(x -> x.toFile().delete());
        }
    }

    @Test
    public void loadsSmallDataset() throws Exception {
        World w = TestData.world("small");
        assertEquals(6, w.bases.size());
        assertEquals(8, w.supplies.size());
        assertEquals(12, w.vehicles.size());
        assertEquals(48, w.inventory.all().size());
        assertEquals(0, w.requests.size());
        assertFalse(w.isDirty());
        assertEquals("NEAREST", w.getSourceStrategyName());
        assertEquals("42", w.extraMeta.get("seed"));
    }

    @Test
    public void saveThenLoadRoundTripsEveryFile() throws Exception {
        Path tmp = Files.createTempDirectory("dlscms-rt");
        try {
            World w = TestData.world("small");
            DataStore.save(w, tmp);
            for (String f : new String[] {"bases.csv", "supplies.csv", "inventory.csv", "vehicles.csv",
                    "requests.csv", "deliveries.csv", "meta.csv"}) {
                // row order may differ (registries are sorted by id), content must be identical
                List<String> a = new ArrayList<>(Files.readAllLines(TestData.dir("small").resolve(f)));
                List<String> b = new ArrayList<>(Files.readAllLines(tmp.resolve(f)));
                Collections.sort(a);
                Collections.sort(b);
                assertEquals(f, a, b);
            }
            World again = DataStore.load(tmp);
            assertEquals(w.bases.size(), again.bases.size());
        } finally {
            deleteTree(tmp);
        }
    }

    @Test
    public void missingFolderIsReported() {
        try {
            DataStore.load(java.nio.file.Paths.get("data", "does-not-exist"));
            fail("expected DataFormatException");
        } catch (DataFormatException e) {
            assertTrue(e.getMessage().contains("not found"));
        }
    }

    @Test
    public void malformedRowReportsFileAndLine() throws Exception {
        Path tmp = copyOfSmall();
        try {
            Path inv = tmp.resolve("inventory.csv");
            String text = Files.readString(inv).replaceFirst("PUN,MED-KIT,40,0,120", "PUN,MED-KIT,abc,0,120");
            Files.write(inv, text.getBytes(StandardCharsets.UTF_8));
            try {
                DataStore.load(tmp);
                fail("expected DataFormatException");
            } catch (DataFormatException e) {
                assertTrue(e.getMessage(), e.getMessage().contains("inventory.csv line"));
            }
        } finally {
            deleteTree(tmp);
        }
    }

    @Test
    public void wrongColumnCountAndBadHeaderAndDanglingReference() throws Exception {
        Path tmp = copyOfSmall();
        try {
            Path veh = tmp.resolve("vehicles.csv");
            String good = Files.readString(veh);
            Files.write(veh, good.replaceFirst("(?m)^(FT-01),FuelTruck,", "$1,").getBytes(StandardCharsets.UTF_8));
            try {
                DataStore.load(tmp);
                fail("column count");
            } catch (DataFormatException e) {
                assertTrue(e.getMessage(), e.getMessage().contains("columns"));
            }
            Files.write(veh, good.replace("id,type", "ident,type").getBytes(StandardCharsets.UTF_8));
            try {
                DataStore.load(tmp);
                fail("bad header");
            } catch (DataFormatException e) {
                assertTrue(e.getMessage(), e.getMessage().contains("Bad header"));
            }
            Files.write(veh, good.replaceFirst("(?m)^(FT-01,FuelTruck,)[A-Z]+,[A-Z]+", "$1XXX,XXX")
                    .getBytes(StandardCharsets.UTF_8));
            try {
                DataStore.load(tmp);
                fail("dangling base reference");
            } catch (DataFormatException e) {
                assertTrue(e.getMessage(), e.getMessage().contains("XXX"));
            }
        } finally {
            deleteTree(tmp);
        }
    }

    @Test
    public void missingRequiredFileIsReported() throws Exception {
        Path tmp = copyOfSmall();
        try {
            Files.delete(tmp.resolve("bases.csv"));
            try {
                DataStore.load(tmp);
                fail("expected DataFormatException");
            } catch (DataFormatException e) {
                assertTrue(e.getMessage().contains("bases.csv"));
            }
        } finally {
            deleteTree(tmp);
        }
    }
}
