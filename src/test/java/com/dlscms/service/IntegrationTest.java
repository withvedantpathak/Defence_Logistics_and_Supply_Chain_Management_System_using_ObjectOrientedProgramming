package com.dlscms.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dlscms.TestData;
import com.dlscms.model.Delivery;
import com.dlscms.model.DeliveryStatus;
import com.dlscms.model.Priority;
import com.dlscms.model.RequestStatus;
import com.dlscms.model.SupplyRequest;
import com.dlscms.persistence.DataStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

/** Multi-step scenarios across services, persistence and restart. */
public class IntegrationTest {
    @Test
    public void contendingRequestsServeTheMostUrgentFirst() throws Exception {
        LogisticsManager m = TestData.manager("small");
        // make exactly 60 tents donatable network-wide, then two bases ask for 50 each
        for (var it : m.world().inventory.forSupply("TENT-6P")) {
            it.set(it.getReorderLevel(), it.getReorderLevel());
        }
        var donor = m.world().inventory.find("MUM", "TENT-6P").get();
        donor.set(donor.getReorderLevel() + 60, donor.getReorderLevel());
        SupplyRequest low = m.createRequest("NAG", "TENT-6P", 50, Priority.ROUTINE);
        SupplyRequest high = m.createRequest("DEL", "TENT-6P", 50, Priority.URGENT);
        m.requests().approve(low.getId());
        m.requests().approve(high.getId());
        m.fulfilAll();
        assertEquals(50, high.getPlannedQty());               // urgent request got everything it needed
        assertEquals(10, low.getPlannedQty());                // the other got the 10 left over
        assertEquals(RequestStatus.FULFILLING, low.getStatus());
        assertEquals(40, low.remaining());
    }

    @Test
    public void largeRequestSplitsAcrossDonorsAndVehicles() throws Exception {
        LogisticsManager m = TestData.manager("medium");
        SupplyRequest r = m.createRequest("PUN", "RATION", 3000, Priority.URGENT);
        m.requests().approve(r.getId());
        List<Delivery> ds = m.fulfil(r.getId());
        int sum = ds.stream().mapToInt(Delivery::getQty).sum();
        assertEquals(sum, r.getPlannedQty());
        assertTrue("expected several trips for 3000 rations", ds.size() > 1);
        Set<String> vehicles = new HashSet<>();
        for (Delivery d : ds) {
            assertTrue("a vehicle is used for one trip at a time", vehicles.add(d.getVehicleId()));
        }
        m.dispatchAllPlanned();
        m.tick(500);
        assertEquals(sum, r.getFulfilledQty());
        for (Delivery d : ds) {
            assertEquals(DeliveryStatus.DELIVERED, d.getStatus());
        }
    }

    @Test
    public void stateSurvivesSaveAndRestartMidScenario() throws Exception {
        Path dir = Files.createTempDirectory("dlscms-restart");
        LogisticsManager a = TestData.manager("small");
        SupplyRequest r = a.createRequest("PUN", "MED-KIT", 200, Priority.URGENT);
        a.requests().approve(r.getId());
        Delivery d = a.fulfil(r.getId()).get(0);
        a.dispatch(d.getId());                                  // in transit, stock already left the donor
        DataStore.save(a.world(), dir);

        LogisticsManager b = LogisticsManager.forWorld(DataStore.load(dir));      // "restart"
        assertEquals(a.world().clock.now(), b.world().clock.now());
        assertEquals(DeliveryStatus.IN_TRANSIT, b.deliveries().get(d.getId()).getStatus());
        assertEquals(r.getPlannedQty(), b.requests().get(r.getId()).getPlannedQty());
        b.tick(24);
        assertEquals(RequestStatus.FULFILLED, b.requests().get(r.getId()).getStatus());
        assertEquals(240, b.world().inventory.find("PUN", "MED-KIT").get().getQty());
        // the original, un-restarted world reaches the same end state
        a.tick(24);
        assertEquals(a.world().inventory.find("PUN", "MED-KIT").get().getQty(),
                b.world().inventory.find("PUN", "MED-KIT").get().getQty());
        // ids keep counting after restart
        SupplyRequest next = b.createRequest("NAG", "RATION", 5, Priority.ROUTINE);
        assertEquals("R-0002", next.getId());
    }

    @Test
    public void everyDeliveredRequestHasMatchingDeliveries() throws Exception {
        LogisticsManager m = TestData.manager("small");
        for (String base : new String[] {"PUN", "NAG", "JAI"}) {
            SupplyRequest r = m.createRequest(base, "RATION", 100, Priority.URGENT);
            m.requests().approve(r.getId());
        }
        m.fulfilAll();
        m.dispatchAllPlanned();
        m.tick(200);
        for (SupplyRequest r : m.world().requests.all()) {
            if (r.getStatus() == RequestStatus.FULFILLED) {
                int delivered = m.deliveriesOf(r.getId()).stream()
                        .filter(d -> d.getStatus() == DeliveryStatus.DELIVERED).mapToInt(Delivery::getQty).sum();
                assertEquals(r.getQty(), delivered);
            }
        }
        assertFalse(m.hasOpenDeliveries());
    }
}
