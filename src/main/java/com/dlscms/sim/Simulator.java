package com.dlscms.sim;

import com.dlscms.exception.LogisticsException;
import com.dlscms.model.Delivery;
import com.dlscms.model.DeliveryStatus;
import com.dlscms.model.Priority;
import com.dlscms.model.RequestStatus;
import com.dlscms.model.SupplyRequest;
import com.dlscms.service.LogisticsManager;
import com.dlscms.service.ReportService;
import com.dlscms.service.World;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Replays a {@link Scenario} against a world, hour by hour, with automatic approval, planning and dispatch.
 *
 * <p>Each simulated hour: (1) requests arriving this hour are created and approved, (2) every approved request is
 * planned in priority order and its deliveries dispatched at once, (3) the clock advances one hour and arrivals
 * complete. The run stops {@value #DRAIN_HOURS} hours after the last arrival.
 */
public final class Simulator {
    /** Hours the simulation keeps running after the last request arrives. */
    public static final long DRAIN_HOURS = 168;

    private final LogisticsManager manager;
    private final Scenario scenario;

    /**
     * @param manager  manager over the world to simulate (its strategies are used as configured)
     * @param scenario the request stream
     */
    public Simulator(LogisticsManager manager, Scenario scenario) {
        this.manager = manager;
        this.scenario = scenario;
        manager.setAutoDispatch(true);
    }

    /**
     * Runs the whole scenario.
     *
     * @param scale dataset scale label for the result row
     * @param seed  dataset seed label for the result row
     * @return the metrics
     * @throws LogisticsException if an unexpected domain error occurs (domain refusals inside planning are normal
     *                            and handled by the manager)
     */
    public SimResult run(String scale, long seed) throws LogisticsException {
        long t0 = System.nanoTime();
        World w = manager.world();
        List<ScenarioRequest> reqs = scenario.requests();
        long end = scenario.lastHour() + DRAIN_HOURS;
        int next = 0;
        Map<String, Long> arrival = new HashMap<>();
        for (long h = 0; h < end; h++) {
            while (next < reqs.size() && reqs.get(next).hour() <= h) {
                ScenarioRequest s = reqs.get(next++);
                SupplyRequest r = manager.createRequest(s.baseId(), s.supplyId(), s.qty(), s.priority());
                manager.requests().approve(r.getId());
                arrival.put(r.getId(), h);
            }
            manager.fulfilAll();
            manager.tick(1);
        }
        long wall = (System.nanoTime() - t0) / 1_000_000;

        ReportService.Kpi k = manager.reports().kpi();
        int total = w.requests.size();
        int fulfilled = 0;
        int stockLimited = 0;
        int vehicleLimited = 0;
        int inFlight = 0;
        int critical = 0;
        int critOk = 0;
        long unitsReq = 0;
        long unitsDel = 0;
        double critLeadSum = 0;
        int critLeadN = 0;
        Map<String, Long> done = new HashMap<>();
        double km = 0;
        int trips = 0;
        for (Delivery d : w.deliveries.all()) {
            if (d.getStatus() == DeliveryStatus.DELIVERED) {
                done.merge(d.getRequestId(), d.getDeliveredAtHour(), Math::max);
                unitsDel += d.getQty();
                km += d.getDistanceKm();
                trips++;
            }
        }
        for (SupplyRequest r : w.requests.all()) {
            unitsReq += r.getQty();
            boolean ok = r.getStatus() == RequestStatus.FULFILLED;
            if (ok) {
                fulfilled++;
            } else if (r.getStatus() == RequestStatus.APPROVED || r.getStatus() == RequestStatus.FULFILLING) {
                if (r.remaining() > spareElsewhere(w, r)) {
                    stockLimited++;
                } else if (r.getStatus() == RequestStatus.APPROVED) {
                    vehicleLimited++;
                } else {
                    inFlight++;
                }
            }
            if (r.getPriority() == Priority.CRITICAL) {
                critical++;
                if (ok) {
                    long lead = done.get(r.getId()) - r.getCreatedAtHour();
                    critLeadSum += lead;
                    critLeadN++;
                    if (lead <= 24) {
                        critOk++;
                    }
                }
            }
        }
        Runtime rt = Runtime.getRuntime();
        long mb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        return new SimResult(scale, seed, manager.sourceStrategy().name(), manager.vehicleStrategy().name(), total,
                fulfilled, stockLimited, vehicleLimited, inFlight, total == 0 ? Double.NaN : (double) fulfilled / total,
                unitsReq == 0 ? Double.NaN : (double) unitsDel / unitsReq, k.avgLeadTimeHours(),
                critLeadN == 0 ? Double.NaN : critLeadSum / critLeadN,
                critical == 0 ? Double.NaN : (double) critOk / critical, k.vehicleUtilisation(), trips, km,
                w.clock.now(), wall, mb);
    }

    /** Units that other bases could still donate to this request right now (respecting reorder floors). */
    private int spareElsewhere(World w, SupplyRequest r) throws LogisticsException {
        int sum = 0;
        for (var c : manager.inventory().candidates(w.supplies.get(r.getSupplyId()),
                w.bases.get(r.getRequesterBaseId()), r.getPriority())) {
            sum += c.surplus();
        }
        return sum;
    }
}
