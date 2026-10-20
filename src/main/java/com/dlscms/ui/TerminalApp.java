package com.dlscms.ui;

import com.dlscms.exception.LogisticsException;
import com.dlscms.exception.ValidationException;
import com.dlscms.model.Delivery;
import com.dlscms.model.Priority;
import com.dlscms.model.SupplyCategory;
import com.dlscms.model.SupplyRequest;
import com.dlscms.model.vehicle.Vehicle;
import com.dlscms.persistence.DataStore;
import com.dlscms.service.LogisticsManager;
import com.dlscms.util.Style;
import com.dlscms.util.Validator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/** Bloomberg-style command-line terminal: reads function codes, runs them on the manager, redraws the screen. */
public final class TerminalApp {
    private static final List<String> CODES = List.of("MON", "BASE", "INV", "SUP", "REQ", "FLT", "DLV", "TICK",
            "RPT", "STRAT", "AUTO", "SAVE", "LOAD", "DEMO", "HELP", "EXIT", "QUIT");

    private LogisticsManager m;
    private final Terminal term;
    private final BufferedReader in;
    private final PrintStream out;
    private final boolean interactive;
    private Path dataDir;
    private String screenCode = "MON";
    private String body = "";
    private String status = "";
    private boolean statusIsError;
    private boolean running = true;
    private String lastRequestId = "";
    private String lastDeliveryId = "";

    /**
     * @param m           manager over the loaded world
     * @param dataDir     folder used by SAVE/LOAD/DEMO
     * @param term        terminal styling
     * @param in          command source
     * @param out         screen sink
     * @param interactive true when a person types (screen is cleared, confirmations are asked)
     */
    public TerminalApp(LogisticsManager m, Path dataDir, Terminal term, BufferedReader in, PrintStream out,
                       boolean interactive) {
        this.m = m;
        this.dataDir = dataDir;
        this.term = term;
        this.in = in;
        this.out = out;
        this.interactive = interactive;
    }

    private Screens screens() {
        return new Screens(m, term);
    }

    // ---- main loop ----

    /**
     * Runs the read-evaluate-print loop until EXIT or end of input.
     *
     * @throws IOException on input failure
     */
    public void run() throws IOException {
        execute("MON");
        draw();
        while (running) {
            out.print(prompt());
            out.flush();
            String line = in.readLine();
            if (line == null) {
                break;
            }
            if (!interactive) {
                out.println(line);
            }
            execute(line);
            if (running) {
                draw();
            }
        }
        out.print(term.end());
        out.flush();
    }

    private String prompt() {
        return term.paint(" DLSCMS " + term.arrow() + " ", Style.ID);
    }

    private void draw() {
        if (interactive) {
            out.print(term.clear());
        }
        out.println(header());
        out.println(term.rule());
        out.println(body);
        out.println(term.rule());
        String line;
        if (status.isEmpty()) {
            String alert = screens().firstAlert();
            line = alert == null ? "" : term.paint(" ALERT " + term.arrow() + " " + alert, Style.WARN);
        } else if (statusIsError) {
            line = term.paint(" ERR " + term.arrow() + " " + status, Style.BAD);
        } else {
            line = term.paint(" OK  " + term.arrow() + " " + status, Style.GOOD);
        }
        out.println(line);
        status = "";
        statusIsError = false;
        if (!interactive) {
            out.println();
        }
    }

    private String header() {
        String left = " DLSCMS " + term.arrow() + " DEFENCE LOGISTICS TERMINAL";
        String code = "[" + screenCode + "]";
        String right = "SIM " + m.world().clock.text() + "   SRC:" + m.sourceStrategy().name() + "  VEH:"
                + m.vehicleStrategy().name() + (m.world().isDirty() ? "  *" : "");
        int gap1 = Math.max(2, (Terminal.WIDTH - left.length() - code.length() - right.length()) / 2);
        int gap2 = Math.max(2, Terminal.WIDTH - left.length() - code.length() - right.length() - gap1);
        return term.paint(left, Style.BOLD) + " ".repeat(gap1) + term.paint(code, Style.ID) + " ".repeat(gap2)
                + right;
    }

    // ---- command dispatch ----

    /**
     * Executes one command line, updating screen body and status line. Never throws for domain errors.
     *
     * @param line the command text
     */
    public void execute(String line) {
        String text = line == null ? "" : line.trim();
        if (text.isEmpty()) {
            refresh();
            return;
        }
        String[] a = text.split("\\s+");
        String code = a[0].toUpperCase();
        try {
            if (a.length == 2 && a[1].equals("?")) {
                usage(code);
                return;
            }
            if (code.equals("?")) {
                code = "HELP";
            }
            switch (code) {
                case "MON": show("MON", screens().dashboard()); break;
                case "HELP": show("HELP", screens().help()); break;
                case "SUP": show("SUP", screens().supplies()); break;
                case "BASE": base(a); break;
                case "INV": inv(a); break;
                case "REQ": req(a); break;
                case "FLT": flt(a); break;
                case "DLV": dlv(a); break;
                case "TICK": tick(a); break;
                case "RPT": rpt(a); break;
                case "STRAT": strat(a); break;
                case "AUTO": auto(a); break;
                case "SAVE": save(a); break;
                case "LOAD": load(a); break;
                case "DEMO": demo(); break;
                case "EXIT":
                case "QUIT": exit(); break;
                default: unknown(code); break;
            }
        } catch (LogisticsException e) {
            error(e.getMessage());
        } catch (RuntimeException e) {
            error("Unexpected problem (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
    }

    private void refresh() {
        try {
            switch (screenCode) {
                case "MON": show("MON", screens().dashboard()); break;
                default: break;       // other screens simply redraw as they are
            }
        } catch (RuntimeException e) {
            error(e.getMessage());
        }
    }

    private void show(String code, String text) {
        this.screenCode = code;
        this.body = text;
    }

    private void ok(String msg) {
        this.status = msg;
        this.statusIsError = false;
    }

    private void error(String msg) {
        this.status = msg;
        this.statusIsError = true;
    }

    private void touch() {
        m.world().setDirty(true);
    }

    private void unknown(String code) {
        String best = null;
        int bestD = code.length() <= 3 ? 2 : 3;      // shorter typos must be closer to count as a hint
        for (String c : CODES) {
            int d = distance(code, c);
            if (d < bestD) {
                bestD = d;
                best = c;
            }
        }
        error("Unknown function '" + code + "'" + (best != null ? " - did you mean " + best + "?" : ".")
                + " Type HELP for the list.");
    }

    static int distance(String a, String b) {
        int[][] d = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) {
            d[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            d[0][j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int c = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + c);
            }
        }
        return d[a.length()][b.length()];
    }

    private void usage(String code) {
        String u;
        switch (code) {
            case "BASE": u = "BASE | BASE <id> | BASE ADD <id> <name_with_underscores> <lat> <lon> [capacity] | BASE REMOVE <id>"; break;
            case "INV": u = "INV | INV <base> | INV ADJ <base> <supply> <+-qty> | INV SET <base> <supply> <qty> [reorder]"; break;
            case "REQ": u = "REQ | REQ <id> | REQ NEW <base> <supply> <qty> [ROUTINE|URGENT|CRITICAL] | REQ APPROVE|REJECT|CANCEL|FULFIL <id>"; break;
            case "FLT": u = "FLT | FLT AVAIL [category] | FLT <id> | FLT MAINT <id> ON|OFF"; break;
            case "DLV": u = "DLV | DLV <id> | DLV PLAN <req> [VEH <vehicle>] | DLV DISPATCH <id|ALL> | DLV COMPLETE <id> | DLV FAIL <id> [reason]"; break;
            case "TICK": u = "TICK <hours>"; break;
            case "RPT": u = "RPT STOCK|SHORT|DLV|KPI [EXPORT]"; break;
            case "STRAT": u = "STRAT | STRAT SRC NEAREST|SURPLUS|BALANCED | STRAT VEH SMALLEST|FASTEST"; break;
            case "AUTO": u = "AUTO ON|OFF  (dispatch deliveries automatically when planned)"; break;
            case "SAVE": u = "SAVE [folder]"; break;
            case "LOAD": u = "LOAD [folder]"; break;
            default: u = "Type HELP for all commands"; break;
        }
        ok("Usage: " + u);
    }

    // ---- handlers ----

    private static String up(String[] a, int i) {
        return a.length > i ? a[i].toUpperCase() : "";
    }

    private static void need(String[] a, int n, String usage) throws ValidationException {
        if (a.length < n) {
            throw new ValidationException("Missing argument. Usage: " + usage);
        }
    }

    private void base(String[] a) throws LogisticsException {
        if (a.length == 1) {
            show("BASE", screens().bases());
        } else if (up(a, 1).equals("ADD")) {
            need(a, 6, "BASE ADD <id> <name_with_underscores> <lat> <lon> [capacity]");
            double lat = Validator.parseDouble(a[4], "latitude");
            double lon = Validator.parseDouble(a[5], "longitude");
            int cap = a.length > 6 ? Validator.parseInt(a[6], "capacity") : 5000;
            m.addBase(up(a, 2), a[3].replace('_', ' '), lat, lon, cap);
            touch();
            show("BASE", screens().bases());
            ok("Base " + up(a, 2) + " added");
        } else if (up(a, 1).equals("REMOVE")) {
            need(a, 3, "BASE REMOVE <id>");
            if (!confirm("Remove base " + up(a, 2) + "?")) {
                ok("Cancelled");
                return;
            }
            m.removeBase(up(a, 2));
            touch();
            show("BASE", screens().bases());
            ok("Base " + up(a, 2) + " removed");
        } else {
            show("BASE " + up(a, 1), screens().base(up(a, 1)));
        }
    }

    private void inv(String[] a) throws LogisticsException {
        if (a.length == 1) {
            show("INV", screens().inventoryNetwork());
        } else if (up(a, 1).equals("ADJ")) {
            need(a, 5, "INV ADJ <base> <supply> <+-qty>");
            m.inventory().adjust(up(a, 2), up(a, 3), Validator.parseInt(a[4], "quantity"));
            touch();
            show("INV " + up(a, 2), screens().inventoryOf(up(a, 2)));
            ok("Stock of " + up(a, 3) + " at " + up(a, 2) + " changed by " + a[4]);
        } else if (up(a, 1).equals("SET")) {
            need(a, 5, "INV SET <base> <supply> <qty> [reorder]");
            int reorder = a.length > 5 ? Validator.parseInt(a[5], "reorder level") : -1;
            m.inventory().set(up(a, 2), up(a, 3), Validator.parseInt(a[4], "quantity"), reorder);
            touch();
            show("INV " + up(a, 2), screens().inventoryOf(up(a, 2)));
            ok("Stock of " + up(a, 3) + " at " + up(a, 2) + " set to " + a[4]);
        } else {
            show("INV " + up(a, 1), screens().inventoryOf(up(a, 1)));
        }
    }

    private void req(String[] a) throws LogisticsException {
        String sub = up(a, 1);
        if (a.length == 1) {
            show("REQ", screens().requests());
        } else if (sub.equals("NEW")) {
            need(a, 5, "REQ NEW <base> <supply> <qty> [ROUTINE|URGENT|CRITICAL]");
            Priority p = a.length > 5 ? Priority.parse(a[5]) : Priority.ROUTINE;
            SupplyRequest r = m.createRequest(up(a, 2), up(a, 3), Validator.parseInt(a[4], "quantity"), p);
            lastRequestId = r.getId();
            touch();
            show("REQ " + r.getId(), screens().request(r.getId()));
            ok("Request " + r.getId() + " created (NEW). Next: REQ APPROVE " + r.getId());
        } else if (sub.equals("APPROVE") || sub.equals("REJECT") || sub.equals("CANCEL")) {
            need(a, 3, "REQ " + sub + " <id>");
            SupplyRequest r = sub.equals("APPROVE") ? m.requests().approve(up(a, 2))
                    : sub.equals("REJECT") ? m.requests().reject(up(a, 2)) : m.requests().cancel(up(a, 2));
            touch();
            show("REQ " + r.getId(), screens().request(r.getId()));
            ok("Request " + r.getId() + " is now " + r.getStatus()
                    + (sub.equals("APPROVE") ? ". Next: REQ FULFIL " + r.getId() : ""));
        } else if (sub.equals("FULFIL") || sub.equals("FULFILL")) {
            need(a, 3, "REQ FULFIL <id>");
            List<Delivery> ds = m.fulfil(up(a, 2));
            touch();
            if (!ds.isEmpty()) {
                lastDeliveryId = ds.get(0).getId();
            }
            show("REQ " + up(a, 2), screens().request(up(a, 2)));
            ok(ds.size() + " delivery(ies) planned" + (m.isAutoDispatch() ? " and dispatched"
                    : ". Next: DLV DISPATCH ALL"));
        } else {
            show("REQ " + sub, screens().request(sub));
        }
    }

    private void flt(String[] a) throws LogisticsException {
        String sub = up(a, 1);
        if (a.length == 1) {
            show("FLT", screens().fleet(m.fleet().all(), "FLEET"));
        } else if (sub.equals("AVAIL")) {
            List<Vehicle> list;
            if (a.length > 2) {
                SupplyCategory c;
                try {
                    c = SupplyCategory.valueOf(up(a, 2));
                } catch (IllegalArgumentException e) {
                    throw new ValidationException("Unknown category '" + a[2] + "' (use "
                            + Arrays.toString(SupplyCategory.values()) + ")");
                }
                list = m.fleet().available(c);
            } else {
                list = m.fleet().all().stream()
                        .filter(v -> v.getStatus() == com.dlscms.model.vehicle.VehicleStatus.AVAILABLE).toList();
            }
            show("FLT AVAIL", screens().fleet(list, "AVAILABLE VEHICLES"));
        } else if (sub.equals("MAINT")) {
            need(a, 4, "FLT MAINT <id> ON|OFF");
            boolean on = up(a, 3).equals("ON");
            if (!on && !up(a, 3).equals("OFF")) {
                throw new ValidationException("Use ON or OFF");
            }
            m.fleet().setMaintenance(up(a, 2), on);
            touch();
            show("FLT", screens().fleet(m.fleet().all(), "FLEET"));
            ok("Vehicle " + up(a, 2) + (on ? " in maintenance" : " back in service"));
        } else {
            show("FLT " + sub, screens().vehicle(sub));
        }
    }

    private void dlv(String[] a) throws LogisticsException {
        String sub = up(a, 1);
        if (a.length == 1) {
            show("DLV", screens().deliveries());
        } else if (sub.equals("PLAN")) {
            need(a, 3, "DLV PLAN <request> [VEH <vehicle>]");
            String veh = a.length > 4 && up(a, 3).equals("VEH") ? up(a, 4) : null;
            Delivery d = m.planOne(up(a, 2), veh);
            lastDeliveryId = d.getId();
            touch();
            show("DLV " + d.getId(), screens().delivery(d.getId()));
            ok("Delivery " + d.getId() + " planned. Next: DLV DISPATCH " + d.getId());
        } else if (sub.equals("DISPATCH")) {
            need(a, 3, "DLV DISPATCH <id|ALL>");
            if (up(a, 2).equals("ALL")) {
                int n = m.dispatchAllPlanned();
                touch();
                show("DLV", screens().deliveries());
                ok(n + " delivery(ies) dispatched. Time only moves with TICK <hours>");
            } else {
                Delivery d = m.dispatch(up(a, 2));
                touch();
                show("DLV " + d.getId(), screens().delivery(d.getId()));
                ok("Delivery " + d.getId() + " in transit, ETA " + com.dlscms.service.SimClock.format(d.getEtaHour())
                        + ". Time only moves with TICK <hours>");
            }
        } else if (sub.equals("COMPLETE")) {
            need(a, 3, "DLV COMPLETE <id>");
            Delivery d = m.complete(up(a, 2));
            touch();
            show("DLV " + d.getId(), screens().delivery(d.getId()));
            ok("Delivery " + d.getId() + " delivered");
        } else if (sub.equals("FAIL")) {
            need(a, 3, "DLV FAIL <id> [reason]");
            String why = a.length > 3 ? String.join(" ", Arrays.copyOfRange(a, 3, a.length)) : "marked failed";
            Delivery d = m.fail(up(a, 2), why);
            touch();
            show("DLV " + d.getId(), screens().delivery(d.getId()));
            ok("Delivery " + d.getId() + " failed; stock returned to " + d.getSourceBaseId());
        } else {
            show("DLV " + sub, screens().delivery(sub));
        }
    }

    private void tick(String[] a) throws LogisticsException {
        need(a, 2, "TICK <hours>");
        int h = Validator.parseInt(a[1], "hours");
        if (h <= 0) {
            throw new ValidationException("hours must be a positive whole number");
        }
        List<Delivery> done = m.tick(h);
        touch();
        show("DLV", screens().deliveries());
        ok("Clock advanced " + h + " h to " + m.world().clock.text() + "; " + done.size()
                + " delivery(ies) arrived");
    }

    private void rpt(String[] a) throws LogisticsException {
        need(a, 2, "RPT STOCK|SHORT|DLV|KPI [EXPORT]");
        String kind = up(a, 1);
        Screens s = screens();
        String text = s.report(kind);
        show("RPT " + kind, text);
        if (up(a, 2).equals("EXPORT")) {
            Path dir = Paths.get("reports");
            try {
                Files.createDirectories(dir);
                Path f = dir.resolve(kind.toLowerCase() + ".csv");
                Files.writeString(f, s.reportTable(kind).toCsv());
                ok("Exported " + f);
            } catch (IOException e) {
                throw new ValidationException("Could not write report: " + e.getMessage());
            }
        }
    }

    private void strat(String[] a) throws LogisticsException {
        if (a.length >= 3 && up(a, 1).equals("SRC")) {
            m.useSourceStrategy(up(a, 2));
            ok("Source strategy: " + m.sourceStrategy().name());
        } else if (a.length >= 3 && up(a, 1).equals("VEH")) {
            m.useVehicleStrategy(up(a, 2));
            ok("Vehicle strategy: " + m.vehicleStrategy().name());
        } else if (a.length > 1) {
            throw new ValidationException("Usage: STRAT SRC NEAREST|SURPLUS|BALANCED  or  STRAT VEH SMALLEST|FASTEST");
        }
        show("STRAT", screens().strategies());
    }

    private void auto(String[] a) throws LogisticsException {
        if (up(a, 1).equals("ON") || up(a, 1).equals("OFF")) {
            m.setAutoDispatch(up(a, 1).equals("ON"));
            ok("Auto-dispatch " + up(a, 1));
        } else {
            throw new ValidationException("Usage: AUTO ON|OFF");
        }
        show("STRAT", screens().strategies());
    }

    private void save(String[] a) throws LogisticsException {
        Path dir = a.length > 1 ? Paths.get(a[1]) : dataDir;
        DataStore.save(m.world(), dir);
        m.world().setDirty(false);
        dataDir = dir;
        ok("Saved to " + dir);
    }

    private void load(String[] a) throws LogisticsException {
        Path dir = a.length > 1 ? Paths.get(a[1]) : dataDir;
        if (m.world().isDirty() && !confirm("Unsaved changes will be lost. Load " + dir + "?")) {
            ok("Cancelled");
            return;
        }
        m = LogisticsManager.forWorld(DataStore.load(dir));
        dataDir = dir;
        show("MON", screens().dashboard());
        ok("Loaded " + dir);
    }

    private void exit() {
        if (m.world().isDirty() && interactive && confirm("Unsaved changes. Save before exit?")) {
            try {
                DataStore.save(m.world(), dataDir);
                m.world().setDirty(false);
            } catch (LogisticsException e) {
                error(e.getMessage());
                return;
            }
        }
        running = false;
        out.println(term.paint(" Goodbye.", Style.DIM));
    }

    /** Asks a Y/N question; scripted runs answer Y automatically. */
    private boolean confirm(String question) {
        if (!interactive) {
            out.println(" CONFIRM " + question + " (Y/N) Y  [script]");
            return true;
        }
        out.print(term.paint(" CONFIRM " + question + " (Y/N) ", Style.WARN));
        out.flush();
        try {
            String s = in.readLine();
            return s != null && s.trim().toUpperCase().startsWith("Y");
        } catch (IOException e) {
            return false;
        }
    }

    // ---- demo ----

    private void demo() throws LogisticsException {
        if (m.world().isDirty() && !confirm("DEMO reloads the data folder and drops unsaved changes. Continue?")) {
            ok("Cancelled");
            return;
        }
        m = LogisticsManager.forWorld(DataStore.load(dataDir));
        String[][] steps = {
            {"The Pune base (PUN) is short of medical kits - check the dashboard.", "MON"},
            {"Look at Pune's stock ledger.", "INV PUN"},
            {"Pune raises an URGENT request for 200 medical kits.", "REQ NEW PUN MED-KIT 200 URGENT"},
            {"The duty officer approves it.", "REQ APPROVE {R}"},
            {"The system finds donor bases and vehicles and plans deliveries.", "REQ FULFIL {R}"},
            {"Review the plan: donor stock is reserved but has not moved yet.", "DLV"},
            {"Dispatch everything.", "DLV DISPATCH ALL"},
            {"Advance the clock 12 hours - arrivals complete automatically.", "TICK 12"},
            {"Pune's stock after delivery.", "INV PUN"},
            {"Request status.", "REQ {R}"},
            {"Key performance indicators.", "RPT KPI"}};
        int n = 1;
        for (String[] st : steps) {
            String cmd = st[1].replace("{R}", lastRequestId);
            out.println(term.paint(" DEMO " + n + "/" + steps.length + " " + term.arrow() + " " + st[0], Style.WARN));
            out.println(term.paint(" DLSCMS " + term.arrow() + " " + cmd, Style.ID));
            if (interactive) {
                out.print(term.paint(" (press Enter) ", Style.DIM));
                out.flush();
                try {
                    in.readLine();
                } catch (IOException e) {
                    break;
                }
            }
            execute(cmd);
            draw();
            n++;
        }
        ok("Demo finished. The data is live: try your own commands, or LOAD to reset.");
    }

    /** @return the last status text (used by tests) */
    String lastStatus() {
        return status;
    }

    /** @return the current body (used by tests) */
    String body() {
        return body;
    }

    /** @return whether the last command failed (used by tests) */
    boolean lastWasError() {
        return statusIsError;
    }
}
