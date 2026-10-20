package com.dlscms.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dlscms.exception.InvalidStateTransitionException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import org.junit.Test;

public class RequestAndDeliveryTest {
    private SupplyRequest req(String id, Priority p, long hour) throws Exception {
        return new SupplyRequest(id, "PUN", "MED-KIT", 200, 0, p, RequestStatus.NEW, hour);
    }

    private Delivery delivery() throws Exception {
        return new Delivery("D-0001", "R-0001", "MUM", "PUN", "MED-KIT", 200, "MV-01",
                DeliveryStatus.PLANNED, 10, -1, 14, -1, 120.0, "");
    }

    @Test
    public void requestHappyPath() throws Exception {
        SupplyRequest r = req("R-0001", Priority.URGENT, 0);
        r.approve();
        r.onPlanned(120);
        assertEquals(RequestStatus.FULFILLING, r.getStatus());
        assertEquals(80, r.remaining());
        r.onDelivered(120);
        assertEquals(RequestStatus.APPROVED, r.getStatus()); // partly delivered, nothing in flight: needs more planning
        r.onPlanned(80);
        r.onDelivered(80);
        assertEquals(RequestStatus.FULFILLED, r.getStatus());
    }

    @Test
    public void invalidTransitionsAreRefused() throws Exception {
        SupplyRequest r = req("R-0001", Priority.ROUTINE, 0);
        try {
            r.onPlanned(10);
            fail("cannot plan before approval");
        } catch (InvalidStateTransitionException expected) {
            assertTrue(expected.getMessage().contains("NEW"));
        }
        r.approve();
        try {
            r.approve();
            fail("cannot approve twice");
        } catch (InvalidStateTransitionException expected) {
            // ok
        }
        r.onPlanned(10);
        try {
            r.cancel();
            fail("cannot cancel with planned deliveries");
        } catch (InvalidStateTransitionException expected) {
            // ok
        }
    }

    @Test
    public void failedDeliveryReturnsRequestToApproved() throws Exception {
        SupplyRequest r = req("R-0001", Priority.ROUTINE, 0);
        r.approve();
        r.onPlanned(50);
        r.onFailed(50);
        assertEquals(RequestStatus.APPROVED, r.getStatus());
        assertEquals(200, r.remaining());
    }

    @Test
    public void rejectAndCancelFromNew() throws Exception {
        SupplyRequest a = req("R-1", Priority.ROUTINE, 0);
        a.reject();
        assertTrue(a.getStatus().isFinal());
        SupplyRequest b = req("R-2", Priority.ROUTINE, 0);
        b.cancel();
        assertEquals(RequestStatus.CANCELLED, b.getStatus());
    }

    @Test
    public void orderingIsPriorityThenAgeThenId() throws Exception {
        List<SupplyRequest> list = new ArrayList<>();
        list.add(req("R-0003", Priority.ROUTINE, 1));
        list.add(req("R-0001", Priority.CRITICAL, 5));
        list.add(req("R-0002", Priority.ROUTINE, 0));
        list.add(req("R-0004", Priority.URGENT, 9));
        Collections.sort(list);
        assertEquals("R-0001", list.get(0).getId());
        assertEquals("R-0004", list.get(1).getId());
        assertEquals("R-0002", list.get(2).getId());
        PriorityQueue<SupplyRequest> q = new PriorityQueue<>(list);
        assertEquals("R-0001", q.poll().getId());
    }

    @Test
    public void deliveryLifecycleKeepsPlannedDuration() throws Exception {
        Delivery d = delivery();
        assertEquals(4, d.durationHours());
        d.dispatch(12);
        assertEquals(DeliveryStatus.IN_TRANSIT, d.getStatus());
        assertEquals(16, d.getEtaHour());
        d.complete(16);
        assertEquals(DeliveryStatus.DELIVERED, d.getStatus());
        assertEquals(16, d.getDeliveredAtHour());
        assertEquals("MUM->PUN", d.locationLabel());
    }

    @Test
    public void deliveryRejectsIllegalTransitions() throws Exception {
        Delivery d = delivery();
        try {
            d.complete(11);
            fail("cannot complete before dispatch");
        } catch (InvalidStateTransitionException expected) {
            assertTrue(expected.getMessage().contains("PLANNED"));
        }
        d.dispatch(10);
        try {
            d.dispatch(10);
            fail("cannot dispatch twice");
        } catch (InvalidStateTransitionException expected) {
            // ok
        }
        d.fail("breakdown", 11);
        assertEquals(DeliveryStatus.FAILED, d.getStatus());
        try {
            d.fail("again", 12);
            fail("cannot fail twice");
        } catch (InvalidStateTransitionException expected) {
            // ok
        }
    }

    @Test
    public void csvRoundTrips() throws Exception {
        Delivery d = delivery();
        assertEquals(d.toCsvRow(), Delivery.fromCsv(com.dlscms.util.Csv.split(d.toCsvRow())).toCsvRow());
        SupplyRequest r = req("R-0001", Priority.URGENT, 3);
        assertEquals(r.toCsvRow(), SupplyRequest.fromCsv(com.dlscms.util.Csv.split(r.toCsvRow())).toCsvRow());
    }
}
