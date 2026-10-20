package com.dlscms.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dlscms.TestData;
import com.dlscms.exception.InsufficientStockException;
import com.dlscms.exception.InvalidIdException;
import com.dlscms.exception.InvalidStateTransitionException;
import com.dlscms.exception.ValidationException;
import com.dlscms.model.Base;
import com.dlscms.model.Deliverable;
import com.dlscms.model.Delivery;
import com.dlscms.model.Location;
import com.dlscms.model.Persistable;
import com.dlscms.model.Priority;
import com.dlscms.model.RequestStatus;
import com.dlscms.model.Supply;
import com.dlscms.model.SupplyCategory;
import com.dlscms.model.SupplyRequest;
import com.dlscms.model.Trackable;
import com.dlscms.model.vehicle.Vehicle;
import com.dlscms.strategy.SourceCandidate;
import com.dlscms.strategy.VehicleOption;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class ServicesTest {
    // ---- Registry ----
    @Test
    public void registryIsCaseInsensitiveSortedAndRejectsDuplicates() throws Exception {
        Registry<Base> r = new Registry<>("base", Base::getId);
        r.add(new Base("ZED", "Zed", new Location("City", 10, 10), 100));
        r.add(new Base("ABC", "Abc", new Location("City", 11, 11), 100));
        assertEquals("ABC", r.all().get(0).getId());
        assertEquals("Zed", r.get(" zed ").getName());
        assertTrue(r.contains("abc"));
        assertFalse(r.contains("nope"));
        assertEquals(1, r.filter(b -> b.getName().startsWith("A")).size());
        try {
            r.add(new Base("abc", "Again", new Location("City", 12, 12), 100));
            fail();
        } catch (ValidationException e) {
            assertEquals("Duplicate base id ABC", e.getMessage());
        }
        try {
            r.get("QQQ");
            fail();
        } catch (InvalidIdException e) {
            assertEquals("Unknown base id QQQ", e.getMessage());
        }
        assertEquals("ZED", r.remove("zed").getId());
        assertEquals(1, r.size());
        try {
            r.remove("zed");
            fail();
        } catch (InvalidIdException expected) {
            // ok
        }
    }

    // ---- SimClock ----
    @Test
    public void simClockAdvancesAndFormats() {
        SimClock c = new SimClock();
        assertEquals("DAY 00 00:00", c.text());
        c.advance(51);
        assertEquals(51, c.now());
        assertEquals("DAY 02 03:00", c.text());
        c.advance(-10);
        c.advance(0);
        assertEquals(51, c.now());
        c.set(-4);
        assertEquals(0, c.now());
        assertEquals("DAY 10 23:00", SimClock.format(263));
    }

    // ---- FleetService ----
    @Test
    public void fleetFiltersByCategoryAndMaintenance() throws Exception {
        LogisticsManager m = TestData.manager("small");
        FleetService f = m.fleet();
        int all = f.available(null).size();
        assertEquals(12, all);
        for (Vehicle v : f.available(SupplyCategory.MEDICAL)) {
            assertTrue(v.canCarry(SupplyCategory.MEDICAL));
            assertFalse(v.typeName().equals("FuelTruck"));
        }
        for (Vehicle v : f.available(SupplyCategory.FUEL)) {
            assertTrue(v.canCarry(SupplyCategory.FUEL));
        }
        f.setMaintenance("TR-01", true);
        assertEquals(all - 1, f.available(null).size());
        assertEquals(1, FleetService.withStatus(f.all(), "maintenance").size());
        f.setMaintenance("TR-01", false);
        assertEquals(all, f.available(null).size());
        try {
            f.get("XX-99");
            fail();
        } catch (InvalidIdException expected) {
            // ok
        }
    }

    @Test
    public void vehicleOptionsIncludePositioningAndCapacity() throws Exception {
        LogisticsManager m = TestData.manager("small");
        World w = m.world();
        Supply kit = w.supplies.get("MED-KIT");
        List<VehicleOption> opts = m.fleet().options(kit, w.bases.get("MUM"), w.bases.get("PUN"));
        assertFalse(opts.isEmpty());
        for (VehicleOption o : opts) {
            assertTrue(o.unitsPerTrip() >= 1);
            assertTrue(o.etaHours() > 0);
            assertTrue(o.positioningKm() >= 0);
            assertTrue(o.haulKm() > 100);
        }
    }

    // ---- InventoryService ----
    @Test
    public void candidatesExcludeDestinationAndRespectPriority() throws Exception {
        LogisticsManager m = TestData.manager("small");
        World w = m.world();
        Supply kit = w.supplies.get("MED-KIT");
        Base pun = w.bases.get("PUN");
        List<SourceCandidate> routine = m.inventory().candidates(kit, pun, Priority.ROUTINE);
        List<SourceCandidate> critical = m.inventory().candidates(kit, pun, Priority.CRITICAL);
        assertFalse(routine.isEmpty());
        for (SourceCandidate c : routine) {
            assertFalse(c.base().getId().equals("PUN"));
            assertTrue(c.surplus() > 0);
        }
        int r = routine.stream().mapToInt(SourceCandidate::surplus).sum();
        int c = critical.stream().mapToInt(SourceCandidate::surplus).sum();
        assertTrue("critical may use more stock than routine", c >= r);
    }

    @Test
    public void inventoryServiceGuards() throws Exception {
        LogisticsManager m = TestData.manager("small");
        InventoryService inv = m.inventory();
        int total = inv.totalOnHand("MED-KIT");
        assertEquals(m.totalUnits("MED-KIT"), total);
        try {
            inv.requireAvailable("PUN", "MED-KIT", 1_000_000);
            fail();
        } catch (InsufficientStockException e) {
            assertTrue(e.getMessage().contains("PUN has 40 MED-KIT available"));
        }
        inv.requireAvailable("PUN", "MED-KIT", 40);
        try {
            inv.forBase("NOWHERE");
            fail();
        } catch (InvalidIdException expected) {
            // ok
        }
        try {
            inv.cell("PUN", "NOPE");
            fail();
        } catch (InvalidIdException expected) {
            // ok
        }
    }

    // ---- RequestService ----
    @Test
    public void requestLifecycleAndOrdering() throws Exception {
        LogisticsManager m = TestData.manager("small");
        RequestService rs = m.requests();
        SupplyRequest a = rs.create("PUN", "TENT-6P", 10, Priority.ROUTINE);
        SupplyRequest b = rs.create("NAG", "TENT-6P", 10, Priority.CRITICAL);
        SupplyRequest c = rs.create("JAI", "TENT-6P", 10, Priority.URGENT);
        assertEquals(RequestStatus.NEW, a.getStatus());
        assertTrue(rs.actionable().isEmpty());          // nothing approved yet
        for (SupplyRequest r : List.of(a, b, c)) {
            rs.approve(r.getId());
        }
        List<SupplyRequest> order = rs.actionable();
        assertEquals(List.of(b.getId(), c.getId(), a.getId()), List.of(order.get(0).getId(),
                order.get(1).getId(), order.get(2).getId()));
        assertEquals(3, rs.sorted().size());
        rs.cancel(a.getId());
        assertEquals(RequestStatus.CANCELLED, a.getStatus());
        assertEquals(2, rs.actionable().size());
        try {
            rs.approve(a.getId());          // cancelled requests cannot come back
            fail();
        } catch (InvalidStateTransitionException expected) {
            // ok
        }
        SupplyRequest d = rs.create("DEL", "RATION", 5, Priority.ROUTINE);
        rs.reject(d.getId());
        assertEquals(RequestStatus.REJECTED, d.getStatus());
        try {
            rs.reject(d.getId());
            fail();
        } catch (InvalidStateTransitionException expected) {
            // ok
        }
        try {
            rs.get("R-4242");
            fail();
        } catch (InvalidIdException expected) {
            // ok
        }
    }

    // ---- DeliveryService ----
    @Test
    public void deliveryServiceQueries() throws Exception {
        LogisticsManager m = TestData.manager("small");
        assertTrue(m.deliveries().baseIsIdle("PUN"));
        SupplyRequest r = m.createRequest("PUN", "MED-KIT", 100, Priority.URGENT);
        m.requests().approve(r.getId());
        Delivery d = m.fulfil(r.getId()).get(0);
        assertFalse(m.deliveries().baseIsIdle("PUN"));
        assertFalse(m.deliveries().baseIsIdle(d.getSourceBaseId()));
        assertEquals(0, m.deliveries().inTransitQty("MED-KIT"));
        m.dispatch(d.getId());
        assertEquals(d.getQty(), m.deliveries().inTransitQty("MED-KIT"));
        m.tick(100);
        assertTrue(m.deliveries().baseIsIdle("PUN"));
        assertEquals(0, m.deliveries().inTransitQty("MED-KIT"));
        assertEquals(d.getId(), m.deliveries().get(d.getId().toLowerCase()).getId());
        try {
            m.deliveries().get("D-9999");
            fail();
        } catch (InvalidIdException expected) {
            // ok
        }
    }

    // ---- interfaces used polymorphically ----
    @Test
    public void interfacesWorkAcrossUnrelatedClasses() throws Exception {
        LogisticsManager m = TestData.manager("small");
        SupplyRequest r = m.createRequest("PUN", "MED-KIT", 50, Priority.URGENT);
        m.requests().approve(r.getId());
        Delivery d = m.fulfil(r.getId()).get(0);
        List<Trackable> tracked = new ArrayList<>();
        tracked.addAll(m.world().vehicles.all());
        tracked.add(d);
        for (Trackable t : tracked) {
            assertFalse(t.id().isEmpty());
            assertFalse(t.statusLabel().isEmpty());
            assertFalse(t.locationLabel().isEmpty());
        }
        List<Persistable> rows = new ArrayList<>();
        rows.addAll(m.world().bases.all());
        rows.addAll(m.world().supplies.all());
        rows.addAll(m.world().vehicles.all());
        rows.addAll(m.world().inventory.all());
        rows.add(r);
        rows.add(d);
        for (Persistable p : rows) {
            assertEquals(p.csvHeader().split(",").length, com.dlscms.util.Csv.split(p.toCsvRow()).length);
        }
        Deliverable dv = d;
        dv.dispatch(m.world().clock.now());
        dv.complete(m.world().clock.now() + 5);
        try {
            dv.dispatch(0);
            fail();
        } catch (InvalidStateTransitionException expected) {
            // ok
        }
    }
}
