package com.dlscms.ui;

import com.dlscms.exception.LogisticsException;
import com.dlscms.model.Base;
import com.dlscms.model.Delivery;
import com.dlscms.model.DeliveryStatus;
import com.dlscms.model.InventoryItem;
import com.dlscms.model.Supply;
import com.dlscms.model.SupplyRequest;
import com.dlscms.model.vehicle.Vehicle;
import com.dlscms.service.LogisticsManager;
import com.dlscms.service.ReportService;
import com.dlscms.service.SimClock;
import com.dlscms.service.World;
import com.dlscms.util.Style;
import com.dlscms.util.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Pure renderers: each method turns the current model state into screen text. */
public final class Screens {
    private final LogisticsManager m;
    private final Terminal t;

    /**
     * @param m manager providing the data
     * @param t terminal used for styling
     */
    public Screens(LogisticsManager m, Terminal t) {
        this.m = m;
        this.t = t;
    }

    /**
     * @param s status text
     * @return the style for a status word
     */
    public static Style statusStyle(String s) {
        String u = s.toUpperCase();
        if (u.startsWith("SHORT") || u.equals("FAILED") || u.equals("MAINTENANCE") || u.equals("REJECTED")
                || u.equals("CANCELLED")) {
            return Style.BAD;
        }
        if (u.equals("AVAILABLE") || u.equals("DELIVERED") || u.equals("FULFILLED") || u.startsWith("OK")) {
            return Style.GOOD;
        }
        if (u.equals("NEW") || u.equals("APPROVED") || u.equals("FULFILLING") || u.equals("PLANNED")
                || u.equals("IN_TRANSIT") || u.equals("ASSIGNED")) {
            return Style.WARN;
        }
        return Style.NORMAL;
    }

    private String title(String s) {
        return " " + t.paint(s, Style.BOLD);
    }

    private String kv(String k, String v) {
        return "  " + t.paint(Terminal.padRight(k, 16), Style.DIM) + v;
    }

    private long used(Base b) {
        return m.world().inventory.totalUnits(b.getId());
    }

    // ---- dashboards and lists ----

    /** @return the MON home dashboard body */
    public String dashboard() {
        World w = m.world();
        ReportService rs = m.reports();
        StringBuilder sb = new StringBuilder();
        Table sh = new Table("", "Base", "Supply", "Avail", "Reorder", "Gap");
        List<InventoryItem> shorts = new ArrayList<>();
        for (InventoryItem i : w.inventory.all()) {
            if (i.shortage() > 0) {
                shorts.add(i);
            }
        }
        shorts.sort((a, b) -> Double.compare((double) b.shortage() / Math.max(1, b.getReorderLevel()),
                (double) a.shortage() / Math.max(1, a.getReorderLevel())));
        for (int i = 0; i < Math.min(6, shorts.size()); i++) {
            InventoryItem it = shorts.get(i);
            sh.add(it.getBaseId(), it.getSupplyId(), it.available(), it.getReorderLevel(), -it.shortage());
        }
        sh.style(4, x -> Style.BAD);
        sb.append(title("SHORTAGES (below reorder level)  " + shorts.size() + " cells")).append('\n')
                .append(t.table(sh, 2)).append('\n').append(t.rule()).append('\n');

        List<SupplyRequest> open = m.requests().actionable();
        Table rq = new Table("", "ID", "Priority", "Base", "Supply", "Qty", "Remaining", "Status");
        for (int i = 0; i < Math.min(6, open.size()); i++) {
            SupplyRequest r = open.get(i);
            rq.add(r.getId(), r.getPriority(), r.getRequesterBaseId(), r.getSupplyId(), r.getQty(), r.remaining(),
                    r.getStatus());
        }
        rq.style(6, Screens::statusStyle);
        sb.append(title("OPEN REQUESTS  " + open.size())).append('\n').append(t.table(rq, 2)).append('\n')
                .append(t.rule()).append('\n');

        Table dl = new Table("", "ID", "Route", "Supply", "Qty", "Vehicle", "Status", "ETA");
        int shown = 0;
        int openCount = 0;
        for (Delivery d : w.deliveries.all()) {
            if (d.getStatus().isOpen()) {
                openCount++;
                if (shown++ < 6) {
                    dl.add(d.getId(), d.locationLabel(), d.getSupplyId(), d.getQty(), d.getVehicleId(),
                            d.getStatus(), d.getStatus() == DeliveryStatus.PLANNED ? "-" : SimClock.format(d.getEtaHour()));
                }
            }
        }
        dl.style(5, Screens::statusStyle);
        sb.append(title("ACTIVE DELIVERIES  " + openCount)).append('\n').append(t.table(dl, 2)).append('\n')
                .append(t.rule()).append('\n');

        Map<String, int[]> fleet = new TreeMap<>();
        for (Vehicle v : w.vehicles.all()) {
            int[] c = fleet.computeIfAbsent(v.typeName(), k -> new int[2]);
            c[1]++;
            if (v.getStatus() == com.dlscms.model.vehicle.VehicleStatus.AVAILABLE) {
                c[0]++;
            }
        }
        StringBuilder fl = new StringBuilder();
        for (Map.Entry<String, int[]> e : fleet.entrySet()) {
            fl.append(e.getKey()).append(' ').append(e.getValue()[0]).append('/').append(e.getValue()[1]).append("   ");
        }
        sb.append(title("FLEET (free/total)")).append("  ").append(fl.toString().trim()).append('\n');
        ReportService.Kpi k = rs.kpi();
        sb.append(title("KPI")).append(String.format("  fulfilment %.0f%%   avg lead %s   utilisation %.0f%%"
                + "   delivered %d  failed %d  open %d", k.fulfilmentRate() * 100,
                Double.isNaN(k.avgLeadTimeHours()) ? "n/a" : String.format("%.1f h", k.avgLeadTimeHours()),
                k.vehicleUtilisation() * 100, k.deliveriesDelivered(), k.deliveriesFailed(), k.deliveriesOpen()));
        return sb.toString();
    }

    /** @return the first shortage as an alert string, or null */
    public String firstAlert() {
        InventoryItem worst = null;
        for (InventoryItem i : m.world().inventory.all()) {
            if (i.shortage() > 0 && (worst == null || (double) i.shortage() / Math.max(1, i.getReorderLevel())
                    > (double) worst.shortage() / Math.max(1, worst.getReorderLevel()))) {
                worst = i;
            }
        }
        return worst == null ? null : worst.getBaseId() + ": " + worst.getSupplyId() + " below reorder level ("
                + worst.available() + " < " + worst.getReorderLevel() + ")";
    }

    /** @return the base list */
    public String bases() {
        Table x = new Table("BASES", "ID", "Name", "Lat", "Lon", "Capacity", "Stored", "Use%");
        for (Base b : m.world().bases.all()) {
            long u = used(b);
            x.add(b.getId(), b.getName(), String.format("%.2f", b.getLocation().lat()),
                    String.format("%.2f", b.getLocation().lon()), b.getCapacityUnits(), u,
                    String.format("%.0f%%", 100.0 * u / Math.max(1, b.getCapacityUnits())));
        }
        return title("BASES  " + x.size()) + '\n' + t.table(x, 2);
    }

    /**
     * @param id base id
     * @return base detail with its stock and vehicles
     * @throws LogisticsException if unknown
     */
    public String base(String id) throws LogisticsException {
        Base b = m.world().bases.get(id);
        StringBuilder sb = new StringBuilder();
        sb.append(title(b.getId() + "  " + b.getName())).append('\n')
                .append(kv("Location", String.format("%.4f N, %.4f E", b.getLocation().lat(), b.getLocation().lon())))
                .append('\n').append(kv("Storage", used(b) + " / " + b.getCapacityUnits() + " units")).append('\n');
        sb.append(inventoryOf(id)).append('\n');
        Table v = new Table("", "Vehicle", "Type", "Home", "Status");
        for (Vehicle x : m.world().vehicles.all()) {
            if (x.getCurrentBaseId().equals(b.getId())) {
                v.add(x.getId(), x.typeName(), x.getHomeBaseId(), x.getStatus());
            }
        }
        v.style(3, Screens::statusStyle);
        sb.append(title("VEHICLES AT BASE")).append('\n').append(t.table(v, 2));
        return sb.toString();
    }

    /**
     * @param baseId base id
     * @return stock ledger for the base
     * @throws LogisticsException if unknown
     */
    public String inventoryOf(String baseId) throws LogisticsException {
        Base b = m.world().bases.get(baseId);
        Table x = new Table("", "Supply", "Name", "Category", "Qty", "Rsvd", "Avail", "Reorder", "Status");
        for (InventoryItem i : m.world().inventory.forBase(b.getId())) {
            Supply s = m.world().supplies.get(i.getSupplyId());
            x.add(i.getSupplyId(), s.getName(), s.getCategory(), i.getQty(), i.getReserved(), i.available(),
                    i.getReorderLevel(), (i.shortage() > 0 ? t.down() + " " : t.up() + " ") + ReportService.status(i));
        }
        x.style(8, Screens::statusStyle);
        return title("STOCK  " + b.getId() + " " + b.getName()) + '\n' + t.table(x, 2);
    }

    /** @return network stock per supply plus shortages */
    public String inventoryNetwork() {
        return title("NETWORK STOCK") + '\n' + t.table(m.reports().networkSummary(), 2) + '\n'
                + t.rule() + '\n' + title("SHORTAGES AND SURPLUS") + '\n'
                + t.table(m.reports().shortagesAndSurplus(), 2);
    }

    /** @return the supply catalogue */
    public String supplies() {
        Table x = new Table("", "ID", "Name", "Category", "Unit kg");
        for (Supply s : m.world().supplies.all()) {
            x.add(s.getId(), s.getName(), s.getCategory(), s.getUnitWeightKg());
        }
        return title("SUPPLY CATALOGUE") + '\n' + t.table(x, 2);
    }

    /** @return the request book */
    public String requests() {
        Table x = new Table("", "ID", "Priority", "Base", "Supply", "Qty", "Done", "Planned", "Status", "Age");
        for (SupplyRequest r : m.requests().sorted()) {
            x.add(r.getId(), r.getPriority(), r.getRequesterBaseId(), r.getSupplyId(), r.getQty(),
                    r.getFulfilledQty(), r.getPlannedQty(), r.getStatus(),
                    (m.world().clock.now() - r.getCreatedAtHour()) + "h");
        }
        x.style(7, Screens::statusStyle);
        return title("REQUEST BOOK  " + x.size() + "  (priority, then age)") + '\n' + t.table(x, 2);
    }

    /**
     * @param id request id
     * @return request detail with its deliveries
     * @throws LogisticsException if unknown
     */
    public String request(String id) throws LogisticsException {
        SupplyRequest r = m.requests().get(id);
        StringBuilder sb = new StringBuilder();
        sb.append(title("REQUEST " + r.getId())).append('\n')
                .append(kv("Destination", r.getRequesterBaseId())).append('\n')
                .append(kv("Supply", r.getSupplyId())).append('\n')
                .append(kv("Priority", r.getPriority().toString())).append('\n')
                .append(kv("Status", t.paint(r.getStatus().toString(), statusStyle(r.getStatus().toString()))))
                .append('\n')
                .append(kv("Quantity", r.getQty() + "  (delivered " + r.getFulfilledQty() + ", in plan "
                        + r.getPlannedQty() + ", still unplanned " + r.remaining() + ")")).append('\n')
                .append(kv("Created", SimClock.format(r.getCreatedAtHour()))).append('\n');
        sb.append(deliveryTable(m.deliveriesOf(r.getId())));
        return sb.toString();
    }

    /** @return the vehicle list */
    public String fleet(List<Vehicle> list, String heading) {
        Table x = new Table("", "ID", "Type", "Home", "At", "Cap kg", "km/h", "Status");
        for (Vehicle v : list) {
            x.add(v.getId(), v.typeName(), v.getHomeBaseId(), v.getCurrentBaseId(), v.getCapacityKg(),
                    v.getSpeedKmph(), v.getStatus());
        }
        x.style(6, Screens::statusStyle);
        return title(heading + "  " + x.size()) + '\n' + t.table(x, 2);
    }

    /**
     * @param id vehicle id
     * @return vehicle detail
     * @throws LogisticsException if unknown
     */
    public String vehicle(String id) throws LogisticsException {
        Vehicle v = m.fleet().get(id);
        return title("VEHICLE " + v.getId() + "  (" + v.typeName() + ")") + '\n'
                + kv("Home base", v.getHomeBaseId()) + '\n' + kv("Currently at", v.getCurrentBaseId()) + '\n'
                + kv("Capacity", v.getCapacityKg() + " kg") + '\n' + kv("Cruise speed", v.getSpeedKmph() + " km/h")
                + '\n' + kv("Loading time", String.format("%.1f h", v.loadingHours())) + '\n'
                + kv("Status", t.paint(v.getStatus().toString(), statusStyle(v.getStatus().toString())));
    }

    private String deliveryTable(List<Delivery> list) {
        Table x = new Table("", "ID", "Request", "Route", "Supply", "Qty", "Vehicle", "Status", "Planned",
                "ETA/Done");
        for (Delivery d : list) {
            boolean ended = d.getStatus() == DeliveryStatus.DELIVERED || d.getStatus() == DeliveryStatus.FAILED;
            x.add(d.getId(), d.getRequestId(), d.locationLabel(), d.getSupplyId(), d.getQty(), d.getVehicleId(),
                    d.getStatus(), SimClock.format(d.getPlannedAtHour()),
                    SimClock.format(ended ? d.getDeliveredAtHour() : d.getEtaHour()));
        }
        x.style(6, Screens::statusStyle);
        return title("DELIVERIES  " + x.size()) + '\n' + t.table(x, 2);
    }

    /** @return the delivery list */
    public String deliveries() {
        return deliveryTable(m.world().deliveries.all());
    }

    /**
     * @param id delivery id
     * @return delivery detail with timeline
     * @throws LogisticsException if unknown
     */
    public String delivery(String id) throws LogisticsException {
        Delivery d = m.deliveries().get(id);
        Vehicle v = m.fleet().get(d.getVehicleId());
        DeliveryStatus st = d.getStatus();
        StringBuilder sb = new StringBuilder();
        sb.append(title("DELIVERY " + d.getId())).append('\n')
                .append(kv("Request", d.getRequestId())).append('\n')
                .append(kv("Route", d.getSourceBaseId() + " " + t.arrow() + " " + d.getDestBaseId() + String.format(
                        "   (%.0f km)", d.getDistanceKm()))).append('\n')
                .append(kv("Cargo", d.getQty() + " x " + d.getSupplyId())).append('\n')
                .append(kv("Vehicle", v.getId() + " (" + v.typeName() + ")")).append('\n')
                .append(kv("Status", t.paint(st.toString(), statusStyle(st.toString())))).append('\n')
                .append(title("TIMELINE")).append('\n')
                .append(kv("Planned", SimClock.format(d.getPlannedAtHour()))).append('\n')
                .append(kv("Dispatched", st == DeliveryStatus.PLANNED ? "-" : SimClock.format(d.getDispatchedAtHour())))
                .append('\n')
                .append(kv("Estimated arrival", SimClock.format(d.getEtaHour()))).append('\n')
                .append(kv("Finished", st == DeliveryStatus.DELIVERED || st == DeliveryStatus.FAILED
                        ? SimClock.format(d.getDeliveredAtHour()) : "-"));
        if (st == DeliveryStatus.FAILED) {
            sb.append('\n').append(kv("Reason", String.valueOf(d.getFailureReason())));
        }
        return sb.toString();
    }

    /**
     * @param kind STOCK, SHORT, DLV or KPI
     * @return the report table
     * @throws LogisticsException for an unknown kind
     */
    public Table reportTable(String kind) throws LogisticsException {
        switch (kind) {
            case "STOCK": return m.reports().stock();
            case "SHORT": return m.reports().shortagesAndSurplus();
            case "DLV": return m.reports().deliveries();
            case "KPI": return m.reports().kpiTable();
            default: throw new com.dlscms.exception.ValidationException(
                    "Unknown report '" + kind + "' (use STOCK, SHORT, DLV or KPI)");
        }
    }

    /**
     * @param kind report kind
     * @return rendered report
     * @throws LogisticsException for an unknown kind
     */
    public String report(String kind) throws LogisticsException {
        Table x = reportTable(kind);
        String out = title("REPORT " + kind + "  " + x.size() + " rows") + '\n' + t.table(x, 2);
        if (kind.equals("DLV")) {
            out += "\n" + t.rule() + "\n" + title("DELIVERY SUMMARY") + '\n' + t.table(m.reports().deliverySummary(), 2);
        }
        return out;
    }

    /** @return the strategy screen */
    public String strategies() {
        return title("STRATEGIES") + '\n'
                + kv("Source (donor)", m.sourceStrategy().name() + "  - " + describeSource(m.sourceStrategy().name())) + '\n'
                + kv("Vehicle", m.vehicleStrategy().name() + "  - " + describeVehicle(m.vehicleStrategy().name())) + '\n'
                + kv("Auto-dispatch", m.isAutoDispatch() ? "ON" : "OFF") + '\n'
                + kv("Switch", "STRAT SRC NEAREST|SURPLUS|BALANCED   STRAT VEH SMALLEST|FASTEST   AUTO ON|OFF");
    }

    private static String describeSource(String n) {
        switch (n) {
            case "NEAREST": return "closest donor base first";
            case "SURPLUS": return "donor with the most spare stock first";
            default: return "balance distance and spare stock";
        }
    }

    private static String describeVehicle(String n) {
        return n.equals("SMALLEST") ? "smallest vehicle that fits the load" : "fastest vehicle (shortest ETA)";
    }

    /** @return the help page */
    public String help() {
        String[][] rows = {
            {"MON", "dashboard (home)"},
            {"BASE [id] | ADD <id> <name_with_underscores> <lat> <lon> [cap] | REMOVE <id>", "bases"},
            {"INV [base] | ADJ <base> <supply> <+-qty> | SET <base> <supply> <qty> [reorder]", "inventory"},
            {"SUP", "supply catalogue"},
            {"REQ [id] | NEW <base> <supply> <qty> [prio] | APPROVE|REJECT|CANCEL|FULFIL <id>", "requests"},
            {"FLT [id] | AVAIL [category] | MAINT <id> ON|OFF", "fleet"},
            {"DLV [id] | PLAN <req> [VEH <v>] | DISPATCH <id|ALL> | COMPLETE <id> | FAIL <id> [why]", "deliveries"},
            {"TICK <hours>", "advance the clock"},
            {"RPT STOCK|SHORT|DLV|KPI [EXPORT]", "reports (EXPORT writes reports/*.csv)"},
            {"STRAT [SRC <name> | VEH <name>]   AUTO ON|OFF", "strategies"},
            {"SAVE [folder]   LOAD [folder]", "persistence"},
            {"DEMO", "guided Pune medical-kit scenario"},
            {"HELP   <code> ?   EXIT", "help / quit"}};
        StringBuilder sb = new StringBuilder(title("COMMANDS")).append('\n');
        for (String[] r : rows) {
            sb.append("  ").append(t.paint(Terminal.padRight(r[0], 82), Style.ID)).append(r[1]).append('\n');
        }
        sb.append(title("EXAMPLES")).append('\n')
                .append("  REQ NEW PUN MED-KIT 200 URGENT   |   REQ APPROVE R-0001   |   REQ FULFIL R-0001\n")
                .append("  DLV DISPATCH ALL   |   TICK 12   |   INV PUN   |   RPT KPI   |   SAVE\n")
                .append("  Press Enter on an empty line to refresh; MON returns home.");
        return sb.toString();
    }
}
