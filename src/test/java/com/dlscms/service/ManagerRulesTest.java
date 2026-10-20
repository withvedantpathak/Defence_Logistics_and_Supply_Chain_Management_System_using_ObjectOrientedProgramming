package com.dlscms.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dlscms.TestData;
import com.dlscms.exception.InsufficientStockException;
import com.dlscms.exception.InvalidIdException;
import com.dlscms.exception.InvalidStateTransitionException;
import com.dlscms.exception.NoVehicleAvailableException;
import com.dlscms.exception.ValidationException;
import com.dlscms.model.Delivery;
import com.dlscms.model.DeliveryStatus;
import com.dlscms.model.InventoryItem;
import com.dlscms.model.Priority;
import com.dlscms.model.RequestStatus;
import com.dlscms.model.SupplyRequest;
import com.dlscms.model.vehicle.Vehicle;
import com.dlscms.model.vehicle.VehicleStatus;
import java.util.List;
import org.junit.Test;

public class ManagerRulesTest {
    private SupplyRequest approved(LogisticsManager m, String base, String supply, int qty, Priority p)
            throws Exception {
        SupplyRequest r = m.createRequest(base, supply, qty, p);
        m.requests().approve(r.getId());
        return r;
    }

    private void assertNothingReserved(World w) {
        for (InventoryItem it : w.inventory.all()) {
            assertEquals(it.getBaseId() + "/" + it.getSupplyId(), 0, it.getReserved());
        }
        for (Vehicle v : w.vehicles.all()) {
            assertEquals(VehicleStatus.AVAILABLE, v.getStatus());
        }
    }

    @Test
    public void unknownIdsAreRejected() throws Exception {
        LogisticsManager m = TestData.manager("small");
        try {
            m.createRequest("XXX", "MED-KIT", 5, Priority.ROUTINE);
            fail();
        } catch (InvalidIdException e) {
            assertTrue(e.getMessage().contains("base"));
        }
        try {
            m.createRequest("PUN", "NOPE", 5, Priority.ROUTINE);
            fail();
        } catch (InvalidIdException e) {
            assertTrue(e.getMessage().contains("supply"));
        }
        try {
            m.fulfil("R-9999");
            fail();
        } catch (InvalidIdException e) {
            assertEquals("Unknown request id R-9999", e.getMessage());
        }
        try {
            m.dispatch("D-9999");
            fail();
        } catch (InvalidIdException e) {
            assertTrue(e.getMessage().contains("delivery"));
        }
    }

    @Test
    public void invalidQuantityRejected() throws Exception {
        LogisticsManager m = TestData.manager("small");
        for (int bad : new int[] {0, -5}) {
            try {
                m.createRequest("PUN", "MED-KIT", bad, Priority.ROUTINE);
                fail();
            } catch (ValidationException e) {
                assertTrue(e.getMessage().contains("positive"));
            }
        }
        assertEquals(0, m.world().requests.size());
    }

    @Test
    public void refusedRequestsDoNotBurnIds() throws Exception {
        // regression: found by the stdin smoke test (R-0001 was skipped after a rejected quantity)
        LogisticsManager m = TestData.manager("small");
        try {
            m.createRequest("PUN", "MED-KIT", 0, Priority.URGENT);
            fail();
        } catch (ValidationException expected) {
            // ok
        }
        assertEquals("R-0001", m.createRequest("PUN", "MED-KIT", 5, Priority.URGENT).getId());
    }

    @Test
    public void cannotFulfilUnapprovedOrFinishedRequests() throws Exception {
        LogisticsManager m = TestData.manager("small");
        SupplyRequest r = m.createRequest("PUN", "MED-KIT", 50, Priority.ROUTINE);
        try {
            m.fulfil(r.getId());
            fail();
        } catch (InvalidStateTransitionException e) {
            assertTrue(e.getMessage().contains("NEW"));
        }
        m.requests().approve(r.getId());
        m.fulfil(r.getId());
        try {
            m.fulfil(r.getId());       // already fully planned
            fail();
        } catch (InvalidStateTransitionException e) {
            assertTrue(e.getMessage().contains("fully planned"));
        }
    }

    @Test
    public void notEnoughStockInNetworkChangesNothing() throws Exception {
        LogisticsManager m = TestData.manager("small");
        // ask for far more than every other base can spare
        SupplyRequest r = approved(m, "PUN", "MED-KIT", 100_000, Priority.ROUTINE);
        List<Delivery> planned = m.fulfil(r.getId());     // partial planning is allowed ...
        assertTrue(r.remaining() > 0);
        assertEquals(RequestStatus.FULFILLING, r.getStatus());
        assertFalse(planned.isEmpty());
        // ... and a second attempt finds nothing left
        try {
            m.fulfil(r.getId());
            fail();
        } catch (InsufficientStockException e) {
            assertTrue(e.getMessage().startsWith("Insufficient stock"));
        }
    }

    private static void assertFalse(boolean b) {
        org.junit.Assert.assertFalse(b);
    }

    @Test
    public void noSpareStockAnywhereThrowsAndLeavesNoReservation() throws Exception {
        LogisticsManager m = TestData.manager("small");
        for (InventoryItem it : m.world().inventory.forSupply("TENT-6P")) {
            it.set(it.getQty(), it.getQty() + 1000);       // nobody has surplus
        }
        SupplyRequest r = approved(m, "PUN", "TENT-6P", 10, Priority.ROUTINE);
        try {
            m.fulfil(r.getId());
            fail();
        } catch (InsufficientStockException expected) {
            // ok
        }
        assertNothingReserved(m.world());
        assertEquals(RequestStatus.APPROVED, r.getStatus());
        assertEquals(10, r.remaining());
    }

    @Test
    public void noVehicleThrowsAndLeavesNoReservation() throws Exception {
        LogisticsManager m = TestData.manager("small");
        for (Vehicle v : m.world().vehicles.all()) {
            v.setMaintenance(true);
        }
        SupplyRequest r = approved(m, "PUN", "MED-KIT", 50, Priority.URGENT);
        try {
            m.fulfil(r.getId());
            fail();
        } catch (NoVehicleAvailableException e) {
            assertTrue(e.getMessage().contains("No vehicle"));
        }
        for (InventoryItem it : m.world().inventory.all()) {
            assertEquals(0, it.getReserved());
        }
        assertEquals(RequestStatus.APPROVED, r.getStatus());
    }

    @Test
    public void criticalMayUseStockRoutineCannot() throws Exception {
        LogisticsManager routine = TestData.manager("small");
        LogisticsManager critical = TestData.manager("small");
        for (LogisticsManager m : List.of(routine, critical)) {
            for (InventoryItem it : m.world().inventory.forSupply("BLANKET")) {
                if (it.getBaseId().equals("PUN")) {
                    it.set(0, it.getReorderLevel());
                } else {
                    it.set(it.getReorderLevel() + 5, it.getReorderLevel());     // only 5 spare each
                }
            }
        }
        // set donor stock exactly at reorder level: routine has nothing to take, critical may dip to half
        for (LogisticsManager m : List.of(routine, critical)) {
            for (InventoryItem it : m.world().inventory.forSupply("BLANKET")) {
                if (!it.getBaseId().equals("PUN")) {
                    it.set(it.getReorderLevel(), it.getReorderLevel());
                }
            }
        }
        SupplyRequest a = approved(routine, "PUN", "BLANKET", 20, Priority.ROUTINE);
        try {
            routine.fulfil(a.getId());
            fail("routine must not dip below reorder levels");
        } catch (InsufficientStockException expected) {
            // ok
        }
        SupplyRequest b = approved(critical, "PUN", "BLANKET", 20, Priority.CRITICAL);
        assertFalse(critical.fulfil(b.getId()).isEmpty());
    }

    @Test
    public void requestsServedMostUrgentFirst() throws Exception {
        LogisticsManager m = TestData.manager("small");
        SupplyRequest low = approved(m, "PUN", "TENT-6P", 10, Priority.ROUTINE);
        SupplyRequest high = approved(m, "NAG", "TENT-6P", 10, Priority.CRITICAL);
        List<SupplyRequest> order = m.requests().actionable();
        assertEquals(high.getId(), order.get(0).getId());
        assertEquals(low.getId(), order.get(1).getId());
        assertTrue(m.fulfilAll() >= 2);
    }

    @Test
    public void failPlannedDeliveryReleasesEverything() throws Exception {
        LogisticsManager m = TestData.manager("small");
        SupplyRequest r = approved(m, "PUN", "MED-KIT", 100, Priority.URGENT);
        List<Delivery> ds = m.fulfil(r.getId());
        for (Delivery d : ds) {
            m.fail(d.getId(), "cancelled by operator");
            assertEquals(DeliveryStatus.FAILED, d.getStatus());
        }
        assertNothingReserved(m.world());
        assertEquals(RequestStatus.APPROVED, r.getStatus());
        assertEquals(100, r.remaining());
        // can be planned again
        assertFalse(m.fulfil(r.getId()).isEmpty());
    }

    @Test
    public void failInTransitReturnsStockToDonor() throws Exception {
        LogisticsManager m = TestData.manager("small");
        int mumBefore = m.world().inventory.find("MUM", "MED-KIT").get().getQty();
        int total = m.totalUnits("MED-KIT");
        SupplyRequest r = approved(m, "PUN", "MED-KIT", 100, Priority.URGENT);
        Delivery d = m.fulfil(r.getId()).get(0);
        m.dispatch(d.getId());
        assertEquals(mumBefore - d.getQty(), m.world().inventory.find(d.getSourceBaseId(), "MED-KIT").get().getQty()
                + (d.getSourceBaseId().equals("MUM") ? 0 : mumBefore - mumBefore + d.getQty() - d.getQty()) );
        m.fail(d.getId(), "vehicle breakdown");
        assertEquals(total, m.totalUnits("MED-KIT"));
        assertEquals(VehicleStatus.AVAILABLE, m.world().vehicles.get(d.getVehicleId()).getStatus());
        assertEquals(d.getSourceBaseId(), m.world().vehicles.get(d.getVehicleId()).getCurrentBaseId());
    }

    @Test
    public void illegalDeliveryTransitionsRefusedAndHarmless() throws Exception {
        LogisticsManager m = TestData.manager("small");
        SupplyRequest r = approved(m, "PUN", "MED-KIT", 50, Priority.URGENT);
        Delivery d = m.fulfil(r.getId()).get(0);
        try {
            m.complete(d.getId());
            fail("cannot complete a PLANNED delivery");
        } catch (InvalidStateTransitionException e) {
            assertTrue(e.getMessage().contains("PLANNED"));
        }
        m.dispatch(d.getId());
        int total = m.totalUnits("MED-KIT");
        try {
            m.dispatch(d.getId());
            fail("cannot dispatch twice");
        } catch (InvalidStateTransitionException e) {
            assertTrue(e.getMessage().contains("IN_TRANSIT"));
        }
        assertEquals(total, m.totalUnits("MED-KIT"));       // refused operation changed nothing
    }

    @Test
    public void manualVehicleOverride() throws Exception {
        LogisticsManager m = TestData.manager("small");
        SupplyRequest r = approved(m, "PUN", "MED-KIT", 30, Priority.URGENT);
        Vehicle heli = m.world().vehicles.all().stream().filter(v -> v.typeName().equals("Helicopter"))
                .findFirst().get();
        Delivery d = m.planOne(r.getId(), heli.getId());
        assertEquals(heli.getId(), d.getVehicleId());
        // a fuel truck cannot carry medical supplies
        Vehicle tanker = m.world().vehicles.all().stream().filter(v -> v.typeName().equals("FuelTruck"))
                .findFirst().get();
        SupplyRequest r2 = approved(m, "NAG", "MED-KIT", 30, Priority.URGENT);
        try {
            m.planOne(r2.getId(), tanker.getId());
            fail();
        } catch (NoVehicleAvailableException e) {
            assertTrue(e.getMessage().contains(tanker.getId()));
        }
    }

    @Test
    public void strategiesCanBeSwappedAtRuntime() throws Exception {
        LogisticsManager m = TestData.manager("medium");
        m.useSourceStrategy("surplus");
        m.useVehicleStrategy("smallest");
        assertEquals("SURPLUS", m.world().getSourceStrategyName());
        assertEquals("SMALLEST", m.sourceStrategy().name().equals("SURPLUS") ? "SMALLEST" : "?");
        assertEquals("SMALLEST", m.vehicleStrategy().name());
        try {
            m.useSourceStrategy("COIN-FLIP");
            fail();
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("NEAREST"));
        }
    }

    @Test
    public void basesCanBeAddedAndSafelyRemoved() throws Exception {
        LogisticsManager m = TestData.manager("small");
        m.addBase("SRI", "Srinagar Node", 34.08, 74.80, 4000);
        assertEquals(7, m.world().bases.size());
        try {
            m.addBase("sri", "Duplicate", 10, 10, 100);
            fail();
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("Duplicate"));
        }
        try {
            m.addBase("BAD", "Bad", 123, 10, 100);
            fail();
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("latitude"));
        }
        m.removeBase("SRI");
        assertEquals(6, m.world().bases.size());
        try {
            m.removeBase("PUN");           // holds stock
            fail();
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("holds stock") || e.getMessage().contains("stock"));
        }
    }

    @Test
    public void inventoryAdjustRules() throws Exception {
        LogisticsManager m = TestData.manager("small");
        InventoryItem it = m.inventory().adjust("PUN", "MED-KIT", -15);
        assertEquals(25, it.getQty());
        try {
            m.inventory().adjust("PUN", "MED-KIT", -9999);
            fail();
        } catch (InsufficientStockException expected) {
            // ok
        }
        assertEquals(25, m.world().inventory.find("PUN", "MED-KIT").get().getQty());
        try {
            m.inventory().adjust("PUN", "MED-KIT", 10_000_000);
            fail();
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("capacity"));
        }
        assertEquals(60, m.inventory().set("PUN", "MED-KIT", 60, -1).getQty());
        assertEquals(120, m.world().inventory.find("PUN", "MED-KIT").get().getReorderLevel());
    }
}
