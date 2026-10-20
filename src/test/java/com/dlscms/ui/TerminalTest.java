package com.dlscms.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dlscms.Main;
import com.dlscms.util.Style;
import com.dlscms.util.Table;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

public class TerminalTest {
    private static String runMain(String stdin, String... args) throws Exception {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int code = Main.run(args, new ByteArrayInputStream(stdin.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(buf, true, StandardCharsets.UTF_8));
        String s = buf.toString(StandardCharsets.UTF_8);
        assertTrue(s, code == 0 || s.contains("ERR"));
        return s;
    }

    private static String script(String... lines) throws Exception {
        Path f = Files.createTempFile("dlscms", ".txt");
        Files.writeString(f, String.join("\n", lines) + "\n");
        try {
            return runMain("", "--plain", "--script", f.toString());
        } finally {
            Files.deleteIfExists(f);
        }
    }

    @Test
    public void editDistance() {
        assertEquals(0, TerminalApp.distance("INV", "INV"));
        assertEquals(1, TerminalApp.distance("INVV", "INV"));
        assertEquals(2, TerminalApp.distance("RPRT", "RPT") + 1);
    }

    @Test
    public void paintIsNoOpWhenPlain() {
        assertEquals("abc", new Terminal(false).paint("abc", Style.BAD));
        assertTrue(new Terminal(true).paint("abc", Style.BAD).contains("\u001b["));
        assertFalse(new Terminal(false).rule().contains("─"));
        assertEquals("ab ", Terminal.padRight("ab", 3));
        assertEquals(" ab", Terminal.padLeft("ab", 3));
    }

    @Test
    public void tableAlignsAndEmptyShowsNotice() {
        Terminal t = new Terminal(false);
        Table x = new Table("T", "ID", "Qty").add("A", 5).add("BB", 120);
        String s = t.table(x, 1);
        assertTrue(s, s.contains("A     5"));
        assertTrue(t.table(new Table("T", "ID"), 0).contains("(none)"));
    }

    @Test
    public void demoRunsToCompletion() throws Exception {
        String out = runMain("", "--plain", "--demo");
        assertTrue(out.contains("DEMO 11/11"));
        assertTrue(out.contains("FULFILLED"));
        assertTrue(out.contains("DELIVERED"));
        assertFalse(out.contains("\u001b["));
    }

    @Test
    public void errorsAreShownNotThrown() throws Exception {
        String out = script("FOO", "REQ NEW PUN MED-KIT -5", "REQ FULFIL R-0099", "INV NOWHERE", "TICK abc",
                "DLV DISPATCH D-0001", "BASE ADD X", "FLT AVAIL ROCKETS", "EXIT");
        assertTrue(out, out.contains("Unknown function 'FOO'"));
        assertTrue(out.contains("quantity must be a positive whole number"));
        assertTrue(out.contains("Unknown request id R-0099"));
        assertTrue(out.contains("Unknown base id NOWHERE"));
        assertTrue(out.contains("Missing argument. Usage: BASE ADD"));
        assertTrue(out.contains("Unknown category 'ROCKETS'"));
        assertFalse(out.contains("Exception"));
        assertFalse(out.contains("\tat "));
    }

    @Test
    public void everyScreenRenders() throws Exception {
        String out = script("MON", "BASE", "BASE PUN", "INV", "INV PUN", "SUP", "REQ", "FLT", "FLT AVAIL", "DLV",
                "STRAT", "RPT STOCK", "RPT SHORT", "RPT DLV", "RPT KPI", "HELP", "?", "REQ ?", "", "EXIT");
        for (String s : new String[] {"[MON]", "[BASE]", "[BASE PUN]", "[INV]", "[INV PUN]", "[SUP]", "[REQ]",
            "[FLT]", "[FLT AVAIL]", "[DLV]", "[STRAT]", "[RPT STOCK]", "[RPT SHORT]", "[RPT DLV]", "[RPT KPI]",
            "[HELP]", "COMMANDS", "Usage: REQ"}) {
            assertTrue("missing " + s, out.contains(s));
        }
        assertFalse(out.contains("Unexpected problem"));
    }

    @Test
    public void fullWorkflowThroughCommands() throws Exception {
        String out = script("REQ NEW PUN MED-KIT 50 URGENT", "REQ APPROVE R-0001", "AUTO ON", "REQ FULFIL R-0001",
                "TICK 24", "REQ R-0001", "DLV FAIL D-0001 test", "EXIT");
        assertTrue(out.contains("Request R-0001 is now APPROVED"));
        assertTrue(out.contains("Auto-dispatch ON"));
        assertTrue(out.contains("and dispatched"));
        assertTrue(out.contains("FULFILLED"));
        assertTrue(out.contains("ERR > ") && out.contains("DELIVERED"));       // failing a delivered trip is refused
    }

    @Test
    public void simClockInHeaderMovesOnlyWithTickAndSurvivesSaveLoad() throws Exception {
        Path dir = Files.createTempDirectory("dlscms-clock");
        String out = script("REQ NEW PUN MED-KIT 10", "TICK 5", "TICK 30", "TICK 0", "SAVE " + dir, "LOAD " + dir, "MON");
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("SIM (DAY \\d+ \\d\\d:00)").matcher(out);
        java.util.List<String> clocks = new java.util.ArrayList<>();
        while (m.find()) {
            clocks.add(m.group(1));
        }
        assertEquals("DAY 00 00:00", clocks.get(0));                       // start
        assertTrue(clocks.contains("DAY 00 05:00"));                       // after TICK 5
        assertTrue(clocks.contains("DAY 01 11:00"));                       // after TICK 30 (5 + 30 h)
        assertEquals("DAY 01 11:00", clocks.get(clocks.size() - 1));       // unchanged by a refused TICK, SAVE, LOAD
        for (java.io.File f : dir.toFile().listFiles()) {
            f.delete();
        }
        Files.deleteIfExists(dir);
    }

    @Test
    public void badOptionsAreReported() throws Exception {
        assertTrue(runMain("", "--bogus").contains("Unknown option"));
        assertTrue(runMain("", "--data", "no/such/folder").contains("ERR"));
        assertTrue(runMain("", "--version").contains("v1.0.0"));
    }

    @Test
    public void interactiveSessionWithAnsiAndExit() throws Exception {
        String out = runMain("HELP\nEXIT\n");
        assertTrue(out.contains("\u001b["));
        assertTrue(out.contains("Goodbye"));
    }

    @Test
    public void saveAndLoadRoundTrip() throws Exception {
        Path dir = Files.createTempDirectory("dlscms-save");
        String out = script("REQ NEW PUN MED-KIT 10", "SAVE " + dir, "LOAD " + dir, "REQ", "EXIT");
        assertTrue(out, out.contains("Saved to"));
        assertTrue(out.contains("Loaded"));
        assertTrue(out.contains("R-0001"));
        try (var st = Files.list(dir)) {
            st.forEach(p -> p.toFile().delete());
        }
        Files.deleteIfExists(dir);
    }
}
