package com.dlscms.sim;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dlscms.TestData;
import com.dlscms.exception.DataFormatException;
import com.dlscms.model.Priority;
import com.dlscms.model.SupplyRequest;
import com.dlscms.model.Supply;
import com.dlscms.service.LogisticsManager;
import com.dlscms.strategy.SourceCandidate;
import com.dlscms.strategy.VehicleOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

public class SimulatorTest {
    private static String stable(SimResult r) {
        // drop the two timing/memory columns, which legitimately differ between runs
        String[] c = r.toCsvRow().split(",");
        return String.join(",", java.util.Arrays.copyOf(c, c.length - 2));
    }

    @Test
    public void scenarioLoadsSortedAndValidates() throws Exception {
        Scenario s = Scenario.load(TestData.dir("small"));
        assertEquals(40, s.requests().size());
        long prev = -1;
        for (ScenarioRequest r : s.requests()) {
            assertTrue(r.hour() >= prev);
            prev = r.hour();
            assertTrue(r.qty() > 0);
        }
        assertEquals(prev, s.lastHour());
        Path bad = Files.createTempDirectory("dlscms-scn");
        Files.writeString(bad.resolve("scenario_requests.csv"),
                "id,requester_base,supply,qty,priority,day,hour\nS-1,PUN,MED-KIT,x,URGENT,0,1\n");
        try {
            Scenario.load(bad);
            fail();
        } catch (DataFormatException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("line 2"));
        }
        Files.writeString(bad.resolve("scenario_requests.csv"), "wrong\n");
        try {
            Scenario.load(bad);
            fail();
        } catch (DataFormatException e) {
            assertTrue(e.getMessage().startsWith("Bad header"));
        }
    }

    @Test
    public void runIsDeterministicAndConservesStock() throws Exception {
        SimResult a = Experiment.runOne(TestData.dir("small"), "small", 42, "NEAREST", "FASTEST", 7);
        SimResult b = Experiment.runOne(TestData.dir("small"), "small", 42, "NEAREST", "FASTEST", 7);
        assertEquals(stable(a), stable(b));
        assertEquals(40, a.requests());
        assertTrue(a.fulfilmentRate() > 0.9);
        assertTrue(a.avgLeadHours() > 0);
        assertTrue(a.utilisation() > 0 && a.utilisation() <= 1);
        assertEquals(Simulator.DRAIN_HOURS + Scenario.load(TestData.dir("small")).lastHour(), a.endHour());
    }

    @Test
    public void stockIsConservedAcrossAWholeRun() throws Exception {
        LogisticsManager m = TestData.manager("small");
        java.util.Map<String, Integer> before = new java.util.HashMap<>();
        for (Supply s : m.world().supplies.all()) {
            before.put(s.getId(), m.totalUnits(s.getId()));
        }
        new Simulator(m, Scenario.load(TestData.dir("small"))).run("small", 42);
        // every delivery finished by the end, so stock is the same as at the start
        assertTrue(!m.hasOpenDeliveries());
        for (Supply s : m.world().supplies.all()) {
            assertEquals(s.getId(), (int) before.get(s.getId()), m.totalUnits(s.getId()));
        }
    }

    @Test
    public void strategiesChangeResultsAndRandomBaselinesAreSeeded() throws Exception {
        SimResult near = Experiment.runOne(TestData.dir("medium"), "medium", 42, "NEAREST", "FASTEST", 7);
        SimResult rnd1 = Experiment.runOne(TestData.dir("medium"), "medium", 42, "RANDOM", "RANDOM", 7);
        SimResult rnd2 = Experiment.runOne(TestData.dir("medium"), "medium", 42, "RANDOM", "RANDOM", 7);
        SimResult rnd3 = Experiment.runOne(TestData.dir("medium"), "medium", 42, "RANDOM", "RANDOM", 8);
        assertEquals(stable(rnd1), stable(rnd2));
        assertTrue(!stable(rnd1).equals(stable(rnd3)));
        assertTrue("coordination should not lose to a random baseline on total distance",
                near.totalKm() < rnd1.totalKm());
    }

    @Test
    public void randomStrategiesPickFromTheOptionsGiven() throws Exception {
        RandomSource rs = new RandomSource(1);
        RandomVehicle rv = new RandomVehicle(1);
        assertEquals("RANDOM", rs.name());
        assertEquals("RANDOM", rv.name());
        assertTrue(rs.select(null, new ArrayList<SourceCandidate>()).isEmpty());
        assertTrue(rv.select(new ArrayList<VehicleOption>(), 5).isEmpty());
        LogisticsManager m = TestData.manager("small");
        SupplyRequest req = m.createRequest("PUN", "MED-KIT", 10, Priority.URGENT);
        List<SourceCandidate> c = m.inventory().candidates(m.world().supplies.get("MED-KIT"),
                m.world().bases.get("PUN"), Priority.URGENT);
        Optional<SourceCandidate> pick = rs.select(req, c);
        assertTrue(c.contains(pick.get()));
    }

    @Test
    public void resultCsvMatchesHeader() throws Exception {
        SimResult r = Experiment.runOne(TestData.dir("small"), "small", 42, "SURPLUS", "SMALLEST", 7);
        assertEquals(SimResult.csvHeader().split(",").length, r.toCsvRow().split(",", -1).length);
    }

    @Test
    public void unfulfilledReasonsPartitionTheUnfulfilled() throws Exception {
        for (String sc : new String[] {"small", "medium"}) {
            SimResult r = Experiment.runOne(TestData.dir(sc), sc, 42, "NEAREST", "FASTEST", 7);
            assertTrue(r.stockLimited() >= 0);
            assertEquals(0, r.requests() - r.fulfilled() - r.stockLimited() - r.vehicleLimited() - r.inFlight());
        }
    }
}
