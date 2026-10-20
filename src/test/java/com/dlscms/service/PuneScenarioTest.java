package com.dlscms.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dlscms.TestData;
import com.dlscms.model.Delivery;
import com.dlscms.model.DeliveryStatus;
import com.dlscms.model.Priority;
import com.dlscms.model.RequestStatus;
import com.dlscms.model.SupplyRequest;
import com.dlscms.model.vehicle.VehicleStatus;
import java.util.List;
import org.junit.Test;

/** The brief's example: Pune needs medical kits -> check donors, create request, assign vehicle, update stock, record delivery. */
public class PuneScenarioTest {
    @Test
    public void puneMedicalKitsEndToEnd() throws Exception {
        LogisticsManager m = TestData.manager("small");
        World w = m.world();
        int puneBefore = w.inventory.find("PUN", "MED-KIT").get().getQty();
        int mumBefore = w.inventory.find("MUM", "MED-KIT").get().getQty();
        int totalBefore = m.totalUnits("MED-KIT");
        assertEquals(40, puneBefore);

        SupplyRequest r = m.createRequest("pun", "med-kit", 200, Priority.URGENT);
        assertEquals("R-0001", r.getId());
        assertEquals(RequestStatus.NEW, r.getStatus());
        m.requests().approve(r.getId());

        List<Delivery> planned = m.fulfil(r.getId());
        assertEquals(RequestStatus.FULFILLING, r.getStatus());
        int sum = planned.stream().mapToInt(Delivery::getQty).sum();
        assertEquals(200, sum);
        assertEquals(0, r.remaining());
        Delivery first = planned.get(0);
        assertEquals("MUM", first.getSourceBaseId());     // nearest donor with spare kits
        assertEquals("PUN", first.getDestBaseId());
        assertEquals(DeliveryStatus.PLANNED, first.getStatus());
        // planning reserves stock but does not move it
        assertEquals(mumBefore, w.inventory.find("MUM", "MED-KIT").get().getQty());
        assertEquals(200, w.inventory.find("MUM", "MED-KIT").get().getReserved());
        assertEquals(VehicleStatus.ASSIGNED, w.vehicles.get(first.getVehicleId()).getStatus());
        assertEquals(totalBefore, m.totalUnits("MED-KIT"));

        for (Delivery d : planned) {
            m.dispatch(d.getId());
        }
        assertEquals(mumBefore - 200, w.inventory.find("MUM", "MED-KIT").get().getQty());
        assertEquals(0, w.inventory.find("MUM", "MED-KIT").get().getReserved());
        assertEquals(puneBefore, w.inventory.find("PUN", "MED-KIT").get().getQty());
        assertEquals(totalBefore, m.totalUnits("MED-KIT"));          // 200 units are in transit
        assertEquals(VehicleStatus.IN_TRANSIT, w.vehicles.get(first.getVehicleId()).getStatus());

        long maxEta = planned.stream().mapToLong(Delivery::getEtaHour).max().getAsLong();
        List<Delivery> arrived = m.tick(maxEta - w.clock.now());
        assertEquals(planned.size(), arrived.size());
        assertEquals(puneBefore + 200, w.inventory.find("PUN", "MED-KIT").get().getQty());
        assertEquals(RequestStatus.FULFILLED, r.getStatus());
        assertEquals(200, r.getFulfilledQty());
        assertEquals(totalBefore, m.totalUnits("MED-KIT"));
        assertEquals(VehicleStatus.AVAILABLE, w.vehicles.get(first.getVehicleId()).getStatus());
        assertEquals("PUN", w.vehicles.get(first.getVehicleId()).getCurrentBaseId());
        assertTrue(w.isDirty());
        // delivered at ETA, not at tick end
        assertEquals(first.getEtaHour(), first.getDeliveredAtHour());
    }

    @Test
    public void kpisAfterScenario() throws Exception {
        LogisticsManager m = TestData.manager("small");
        SupplyRequest r = m.createRequest("PUN", "MED-KIT", 80, Priority.CRITICAL);
        m.requests().approve(r.getId());
        m.setAutoDispatch(true);
        List<Delivery> ds = m.fulfil(r.getId());
        m.tick(ds.stream().mapToLong(Delivery::getEtaHour).max().getAsLong());
        ReportService.Kpi k = m.reports().kpi();
        assertEquals(1, k.approvedRequests());
        assertEquals(1, k.fulfilledRequests());
        assertEquals(1.0, k.fulfilmentRate(), 1e-9);
        assertTrue(k.avgLeadTimeHours() > 0);
        assertEquals(ds.size(), k.deliveriesDelivered());
        assertTrue(k.vehicleUtilisation() > 0 && k.vehicleUtilisation() <= 1.0);
        assertTrue(m.reports().kpiTable().size() > 5);
    }
}
