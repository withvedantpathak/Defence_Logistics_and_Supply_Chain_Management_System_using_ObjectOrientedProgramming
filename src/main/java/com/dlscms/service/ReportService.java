package com.dlscms.service;

import com.dlscms.model.Delivery;
import com.dlscms.model.DeliveryStatus;
import com.dlscms.model.InventoryItem;
import com.dlscms.model.Priority;
import com.dlscms.model.RequestStatus;
import com.dlscms.model.Supply;
import com.dlscms.model.SupplyRequest;
import com.dlscms.model.vehicle.Vehicle;
import com.dlscms.util.Style;
import com.dlscms.util.Table;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Builds the logistics reports as {@link Table}s and computes headline KPIs. */
public final class ReportService {
    private final World world;

    /** @param world the shared network state */
    public ReportService(World world) {
        this.world = world;
    }

    /**
     * Headline performance numbers (definitions in docs/design.md).
     *
     * @param approvedRequests     requests that were approved at some point (APPROVED, FULFILLING, FULFILLED)
     * @param fulfilledRequests    requests fully delivered
     * @param requestedUnits       units requested by approved requests
     * @param deliveredUnits       units of those requests already delivered
     * @param fulfilmentRate       deliveredUnits / requestedUnits (0..1; 1 if nothing requested)
     * @param avgLeadTimeHours     mean hours from creation to final delivery over FULFILLED requests (NaN if none)
     * @param leadTimeByPriority   same, per priority (NaN if none)
     * @param vehicleUtilisation   share of vehicle-hours spent dispatched (0..1)
     * @param deliveriesDelivered  delivered trips
     * @param deliveriesFailed     failed trips
     * @param deliveriesOpen       planned or in-transit trips
     * @param shortageCells        base/supply cells below reorder level
     */
    public record Kpi(int approvedRequests, int fulfilledRequests, long requestedUnits, long deliveredUnits,
                      double fulfilmentRate, double avgLeadTimeHours,
                      Map<Priority, Double> leadTimeByPriority, double vehicleUtilisation,
                      int deliveriesDelivered, int deliveriesFailed, int deliveriesOpen, int shortageCells) { }

    /** @return the KPIs for the current state of the world */
    public Kpi kpi() {
        int approved = 0;
        int fulfilled = 0;
        long requested = 0;
        long delivered = 0;
        Map<String, Long> lastDelivery = new HashMap<>();
        for (Delivery d : world.deliveries.all()) {
            if (d.getStatus() == DeliveryStatus.DELIVERED) {
                lastDelivery.merge(d.getRequestId(), d.getDeliveredAtHour(), Math::max);
            }
        }
        double leadSum = 0;
        int leadN = 0;
        Map<Priority, double[]> byPrio = new EnumMap<>(Priority.class);
        for (SupplyRequest r : world.requests.all()) {
            RequestStatus st = r.getStatus();
            if (st == RequestStatus.APPROVED || st == RequestStatus.FULFILLING || st == RequestStatus.FULFILLED) {
                approved++;
                requested += r.getQty();
                delivered += r.getFulfilledQty();
            }
            if (st == RequestStatus.FULFILLED) {
                fulfilled++;
                Long end = lastDelivery.get(r.getId());
                if (end != null) {
                    double lead = end - r.getCreatedAtHour();
                    leadSum += lead;
                    leadN++;
                    double[] acc = byPrio.computeIfAbsent(r.getPriority(), k -> new double[2]);
                    acc[0] += lead;
                    acc[1] += 1;
                }
            }
        }
        Map<Priority, Double> perPrio = new EnumMap<>(Priority.class);
        for (Priority p : Priority.values()) {
            double[] acc = byPrio.get(p);
            perPrio.put(p, acc == null ? Double.NaN : acc[0] / acc[1]);
        }
        int ok = 0;
        int failed = 0;
        int open = 0;
        double busyHours = 0;
        long now = world.clock.now();
        for (Delivery d : world.deliveries.all()) {
            switch (d.getStatus()) {
                case DELIVERED:
                    ok++;
                    busyHours += d.getDeliveredAtHour() - d.getDispatchedAtHour();
                    break;
                case FAILED:
                    failed++;
                    if (d.getDispatchedAtHour() >= 0) {
                        busyHours += d.getDeliveredAtHour() - d.getDispatchedAtHour();
                    }
                    break;
                case IN_TRANSIT:
                    open++;
                    busyHours += Math.max(0, now - d.getDispatchedAtHour());
                    break;
                default:
                    open++;
                    break;
            }
        }
        double capacityHours = (double) world.vehicles.size() * Math.max(1, now);
        int shortCells = 0;
        for (InventoryItem it : world.inventory.all()) {
            if (it.shortage() > 0) {
                shortCells++;
            }
        }
        return new Kpi(approved, fulfilled, requested, delivered,
                requested == 0 ? 1.0 : (double) delivered / requested,
                leadN == 0 ? Double.NaN : leadSum / leadN, perPrio,
                capacityHours == 0 ? 0 : Math.min(1.0, busyHours / capacityHours),
                ok, failed, open, shortCells);
    }

    private static Style stockStyle(String s) {
        return s.startsWith("SHORT") ? Style.BAD : s.startsWith("OK") ? Style.GOOD : Style.NORMAL;
    }

    private static Style deliveryStyle(String s) {
        switch (s) {
            case "DELIVERED":
                return Style.GOOD;
            case "FAILED":
                return Style.BAD;
            case "PLANNED":
            case "IN_TRANSIT":
                return Style.WARN;
            default:
                return Style.NORMAL;
        }
    }

    /** @return stock of every base and supply */
    public Table stock() {
        Table t = new Table("STOCK", "Base", "Supply", "Qty", "Reserved", "Avail", "Reorder", "Status");
        for (InventoryItem it : world.inventory.all()) {
            t.add(it.getBaseId(), it.getSupplyId(), it.getQty(), it.getReserved(), it.available(),
                    it.getReorderLevel(), status(it));
        }
        t.style(6, ReportService::stockStyle);
        return t;
    }

    /**
     * @param it a stock cell
     * @return "SHORT -80" or "OK +700"
     */
    public static String status(InventoryItem it) {
        int diff = it.available() - it.getReorderLevel();
        return diff < 0 ? "SHORT " + diff : "OK +" + diff;
    }

    /** @return shortages first (largest relative gap first), then spare stock by amount */
    public Table shortagesAndSurplus() {
        List<InventoryItem> shortage = new ArrayList<>();
        List<InventoryItem> surplus = new ArrayList<>();
        for (InventoryItem it : world.inventory.all()) {
            if (it.shortage() > 0) {
                shortage.add(it);
            } else if (it.surplus() > 0) {
                surplus.add(it);
            }
        }
        shortage.sort(Comparator.comparingDouble((InventoryItem i) ->
                -(double) i.shortage() / Math.max(1, i.getReorderLevel())));
        surplus.sort(Comparator.comparingInt(InventoryItem::surplus).reversed());
        Table t = new Table("SHORTAGES AND SURPLUS", "Kind", "Base", "Supply", "Avail", "Reorder", "Gap");
        for (InventoryItem i : shortage) {
            t.add("SHORT", i.getBaseId(), i.getSupplyId(), i.available(), i.getReorderLevel(), -i.shortage());
        }
        for (InventoryItem i : surplus) {
            t.add("SURPLUS", i.getBaseId(), i.getSupplyId(), i.available(), i.getReorderLevel(), "+" + i.surplus());
        }
        t.style(0, k -> k.equals("SHORT") ? Style.BAD : Style.GOOD);
        return t;
    }

    /** @return network-wide stock per supply (on hand, reserved, in transit, reorder, short bases) */
    public Table networkSummary() {
        Table t = new Table("NETWORK STOCK", "Supply", "Name", "On hand", "Reserved", "In transit",
                "Reorder total", "Short bases");
        Map<String, Integer> transit = new TreeMap<>();
        for (Delivery d : world.deliveries.all()) {
            if (d.getStatus() == DeliveryStatus.IN_TRANSIT) {
                transit.merge(d.getSupplyId(), d.getQty(), Integer::sum);
            }
        }
        for (Supply s : world.supplies.all()) {
            int onHand = 0;
            int reserved = 0;
            int reorder = 0;
            int shortBases = 0;
            for (InventoryItem it : world.inventory.forSupply(s.getId())) {
                onHand += it.getQty();
                reserved += it.getReserved();
                reorder += it.getReorderLevel();
                if (it.shortage() > 0) {
                    shortBases++;
                }
            }
            t.add(s.getId(), s.getName(), onHand, reserved, transit.getOrDefault(s.getId(), 0), reorder, shortBases);
        }
        t.style(6, v -> v.equals("0") ? Style.GOOD : Style.BAD);
        return t;
    }

    /** @return every delivery with route, vehicle, status and timing */
    public Table deliveries() {
        Table t = new Table("DELIVERIES", "ID", "Request", "Route", "Supply", "Qty", "Vehicle", "Status",
                "Planned", "ETA/Done");
        for (Delivery d : world.deliveries.all()) {
            long end = d.getStatus() == DeliveryStatus.DELIVERED || d.getStatus() == DeliveryStatus.FAILED
                    ? d.getDeliveredAtHour() : d.getEtaHour();
            t.add(d.getId(), d.getRequestId(), d.locationLabel(), d.getSupplyId(), d.getQty(), d.getVehicleId(),
                    d.getStatus(), SimClock.format(d.getPlannedAtHour()), SimClock.format(end));
        }
        t.style(6, ReportService::deliveryStyle);
        return t;
    }

    /** @return counts of deliveries by status and trips/units per vehicle type */
    public Table deliverySummary() {
        Map<String, int[]> byType = new TreeMap<>();
        Map<DeliveryStatus, Integer> byStatus = new EnumMap<>(DeliveryStatus.class);
        for (Delivery d : world.deliveries.all()) {
            byStatus.merge(d.getStatus(), 1, Integer::sum);
            Vehicle v = world.vehicles.filter(x -> x.getId().equals(d.getVehicleId())).stream().findFirst().orElse(null);
            String type = v == null ? "?" : v.typeName();
            int[] acc = byType.computeIfAbsent(type, k -> new int[2]);
            acc[0]++;
            acc[1] += d.getQty();
        }
        Table t = new Table("DELIVERY SUMMARY", "Group", "Item", "Count", "Units");
        for (DeliveryStatus s : DeliveryStatus.values()) {
            t.add("status", s, byStatus.getOrDefault(s, 0), "");
        }
        for (Map.Entry<String, int[]> e : byType.entrySet()) {
            t.add("vehicle type", e.getKey(), e.getValue()[0], e.getValue()[1]);
        }
        return t;
    }

    /** @return KPI table for screens and CSV export */
    public Table kpiTable() {
        Kpi k = kpi();
        Table t = new Table("KPI", "Metric", "Value");
        t.add("Approved requests", k.approvedRequests());
        t.add("Fulfilled requests", k.fulfilledRequests());
        t.add("Units requested (approved)", k.requestedUnits());
        t.add("Units delivered", k.deliveredUnits());
        t.add("Fulfilment rate", String.format("%.1f%%", 100 * k.fulfilmentRate()));
        t.add("Avg lead time (all)", fmtHours(k.avgLeadTimeHours()));
        for (Priority p : Priority.values()) {
            t.add("Avg lead time " + p, fmtHours(k.leadTimeByPriority().get(p)));
        }
        t.add("Vehicle utilisation", String.format("%.1f%%", 100 * k.vehicleUtilisation()));
        t.add("Deliveries delivered", k.deliveriesDelivered());
        t.add("Deliveries failed", k.deliveriesFailed());
        t.add("Deliveries open", k.deliveriesOpen());
        t.add("Shortage cells now", k.shortageCells());
        return t;
    }

    private static String fmtHours(double h) {
        return Double.isNaN(h) ? "n/a" : String.format("%.1f h", h);
    }
}
