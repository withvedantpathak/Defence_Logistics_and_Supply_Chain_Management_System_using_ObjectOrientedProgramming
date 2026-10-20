package com.dlscms.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dlscms.TestData;
import com.dlscms.exception.LogisticsException;
import com.dlscms.model.Delivery;
import com.dlscms.model.DeliveryStatus;
import com.dlscms.model.InventoryItem;
import com.dlscms.model.Priority;
import com.dlscms.model.Supply;
import com.dlscms.model.SupplyRequest;
import com.dlscms.model.vehicle.Vehicle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.Test;

/** Seeded random operation sequences must never create or destroy stock or corrupt reservations. */
public class ConservationTest {
    private void check(LogisticsManager m, Map<String, Integer> expectedTotals) {
        World w = m.world();
        for (Supply s : w.supplies.all()) {
            assertEquals("total of " + s.getId(), (int) expectedTotals.get(s.getId()), m.totalUnits(s.getId()));
        }
        for (InventoryItem it : w.inventory.all()) {
            assertTrue("non-negative " + it.getBaseId() + "/" + it.getSupplyId(), it.getQty() >= 0);
            assertTrue("reserved <= qty", it.getReserved() >= 0 && it.getReserved() <= it.getQty());
        }
        // reserved stock equals the sum of PLANNED deliveries per cell
        Map<String, Integer> planned = new HashMap<>();
        for (Delivery d : w.deliveries.all()) {
            if (d.getStatus() == DeliveryStatus.PLANNED) {
                planned.merge(d.getSourceBaseId() + "/" + d.getSupplyId(), d.getQty(), Integer::sum);
            }
        }
        for (InventoryItem it : w.inventory.all()) {
            assertEquals(it.getBaseId() + "/" + it.getSupplyId() + " reserved",
                    planned.getOrDefault(it.getBaseId() + "/" + it.getSupplyId(), 0).intValue(), it.getReserved());
        }
        // requests: fulfilled + planned <= qty ; planned equals open deliveries
        Map<String, Integer> open = new HashMap<>();
        for (Delivery d : w.deliveries.all()) {
            if (d.getStatus().isOpen()) {
                open.merge(d.getRequestId(), d.getQty(), Integer::sum);
            }
        }
        for (SupplyRequest r : w.requests.all()) {
            assertEquals(r.getId() + " planned", open.getOrDefault(r.getId(), 0).intValue(), r.getPlannedQty());
            assertTrue(r.getFulfilledQty() + r.getPlannedQty() <= r.getQty());
        }
        // a vehicle is busy iff it has exactly one open delivery
        Map<String, Integer> busy = new HashMap<>();
        for (Delivery d : w.deliveries.all()) {
            if (d.getStatus().isOpen()) {
                busy.merge(d.getVehicleId(), 1, Integer::sum);
            }
        }
        for (Vehicle v : w.vehicles.all()) {
            int n = busy.getOrDefault(v.getId(), 0);
            assertTrue(v.getId() + " has " + n + " open deliveries", n <= 1);
        }
    }

    private void run(String scale, long seed, int steps) throws Exception {
        LogisticsManager m = TestData.manager(scale);
        World w = m.world();
        Map<String, Integer> totals = new HashMap<>();
        for (Supply s : w.supplies.all()) {
            totals.put(s.getId(), m.totalUnits(s.getId()));
        }
        Random rnd = new Random(seed);
        String[] bases = w.bases.all().stream().map(b -> b.getId()).toArray(String[]::new);
        String[] supplies = w.supplies.all().stream().map(s -> s.getId()).toArray(String[]::new);
        Priority[] prios = Priority.values();
        for (int i = 0; i < steps; i++) {
            try {
                switch (rnd.nextInt(7)) {
                    case 0:
                    case 1: {
                        SupplyRequest r = m.createRequest(bases[rnd.nextInt(bases.length)],
                                supplies[rnd.nextInt(supplies.length)], 1 + rnd.nextInt(80), prios[rnd.nextInt(3)]);
                        m.requests().approve(r.getId());
                        break;
                    }
                    case 2:
                        m.fulfilAll();
                        break;
                    case 3: {
                        List<Delivery> planned = w.deliveries.filter(d -> d.getStatus() == DeliveryStatus.PLANNED);
                        if (!planned.isEmpty()) {
                            m.dispatch(planned.get(rnd.nextInt(planned.size())).getId());
                        }
                        break;
                    }
                    case 4:
                        m.tick(1 + rnd.nextInt(12));
                        break;
                    case 5: {
                        List<Delivery> open = w.deliveries.filter(d -> d.getStatus().isOpen());
                        if (!open.isEmpty()) {
                            m.fail(open.get(rnd.nextInt(open.size())).getId(), "random failure");
                        }
                        break;
                    }
                    default: {
                        List<Delivery> transit = w.deliveries.filter(d -> d.getStatus() == DeliveryStatus.IN_TRANSIT);
                        if (!transit.isEmpty()) {
                            m.complete(transit.get(rnd.nextInt(transit.size())).getId());
                        }
                        break;
                    }
                }
            } catch (LogisticsException expectedDomainRefusal) {
                // refused operations must leave invariants intact - checked below
            }
            check(m, totals);
        }
    }

    @Test
    public void smallDatasetRandomOperations() throws Exception {
        for (long seed = 1; seed <= 5; seed++) {
            run("small", seed, 250);
        }
    }

    @Test
    public void mediumDatasetRandomOperations() throws Exception {
        run("medium", 99, 400);
    }
}
