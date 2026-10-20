package com.dlscms.strategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dlscms.TestData;
import com.dlscms.model.Base;
import com.dlscms.model.Priority;
import com.dlscms.model.Supply;
import com.dlscms.model.SupplyRequest;
import com.dlscms.service.LogisticsManager;
import com.dlscms.service.World;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

/** Exercises the two strategy interfaces polymorphically (any implementation, same contract). */
public class StrategyInterfaceTest {
    @Test
    public void everySourceStrategyHonoursTheContract() throws Exception {
        LogisticsManager m = TestData.manager("small");
        World w = m.world();
        SupplyRequest req = m.createRequest("PUN", "MED-KIT", 100, Priority.URGENT);
        Supply kit = w.supplies.get("MED-KIT");
        Base pun = w.bases.get("PUN");
        List<SourceCandidate> cands = m.inventory().candidates(kit, pun, Priority.URGENT);
        assertTrue(cands.size() >= 2);
        for (String name : Strategies.SOURCE_NAMES) {
            SourceSelectionStrategy s = Strategies.source(name);
            assertEquals(name, s.name());
            Optional<SourceCandidate> pick = s.select(req, cands);
            assertTrue(name, pick.isPresent());
            assertTrue(cands.contains(pick.get()));
            assertFalse(s.select(req, new ArrayList<>()).isPresent());
        }
    }

    @Test
    public void everyVehicleStrategyHonoursTheContract() throws Exception {
        LogisticsManager m = TestData.manager("small");
        World w = m.world();
        List<VehicleOption> opts = m.fleet().options(w.supplies.get("MED-KIT"), w.bases.get("MUM"), w.bases.get("PUN"));
        assertTrue(opts.size() >= 2);
        for (String name : Strategies.VEHICLE_NAMES) {
            VehicleAssignmentStrategy s = Strategies.vehicle(name);
            assertEquals(name, s.name());
            Optional<VehicleOption> pick = s.select(opts, 100);
            assertTrue(name, pick.isPresent());
            assertTrue(opts.contains(pick.get()));
            assertFalse(s.select(new ArrayList<>(), 100).isPresent());
        }
        VehicleOption fast = Strategies.vehicle("FASTEST").select(opts, 100).get();
        for (VehicleOption o : opts) {
            assertTrue(fast.etaHours() <= o.etaHours() + 1e-9);
        }
    }

    @Test
    public void strategiesChangeOutcomesOnMediumData() throws Exception {
        // same scenario, different donor rule -> different donor picked at least once across requests
        String first = null;
        boolean differs = false;
        for (String name : Strategies.SOURCE_NAMES) {
            LogisticsManager m = TestData.manager("medium");
            m.useSourceStrategy(name);
            SupplyRequest r = m.createRequest("PUN", "MED-KIT", 300, Priority.URGENT);
            m.requests().approve(r.getId());
            String donors = m.fulfil(r.getId()).stream().map(d -> d.getSourceBaseId()).sorted().toList().toString();
            if (first == null) {
                first = donors;
            } else if (!first.equals(donors)) {
                differs = true;
            }
        }
        assertTrue("source strategies should not all behave identically", differs);
    }
}
