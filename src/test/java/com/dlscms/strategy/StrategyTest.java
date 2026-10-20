package com.dlscms.strategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.dlscms.exception.ValidationException;
import com.dlscms.model.Base;
import com.dlscms.model.Location;
import com.dlscms.model.Priority;
import com.dlscms.model.RequestStatus;
import com.dlscms.model.SupplyRequest;
import com.dlscms.model.vehicle.FuelTruck;
import com.dlscms.model.vehicle.Helicopter;
import com.dlscms.model.vehicle.MedicalVan;
import com.dlscms.model.vehicle.Truck;
import com.dlscms.model.vehicle.VehicleStatus;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

public class StrategyTest {
    private SourceCandidate cand(String id, int surplus, double km) throws Exception {
        return new SourceCandidate(new Base(id, id, new Location(id, 10, 10), 1000), surplus, km);
    }

    private SupplyRequest req() throws Exception {
        return new SupplyRequest("R-1", "PUN", "MED-KIT", 100, 0, Priority.URGENT, RequestStatus.APPROVED, 0);
    }

    private List<SourceCandidate> donors() throws Exception {
        return List.of(cand("AAA", 50, 100), cand("BBB", 400, 600), cand("CCC", 200, 300));
    }

    @Test
    public void sourceStrategiesChooseDifferently() throws Exception {
        assertEquals("AAA", new NearestFirst().select(req(), donors()).get().base().getId());
        assertEquals("BBB", new MostSurplus().select(req(), donors()).get().base().getId());
        // balanced: AAA 0.5*(1-100/600)+0.5*(50/400)=0.479 ; BBB 0.5*0+0.5*1=0.5 ; CCC 0.5*0.5+0.5*0.5=0.5
        // BBB and CCC tie on score -> reverse id order picks the lexicographically smaller id (BBB)
        assertEquals("BBB", new Balanced().select(req(), donors()).get().base().getId());
    }

    @Test
    public void emptyCandidateListGivesEmpty() throws Exception {
        assertTrue(new NearestFirst().select(req(), List.of()).isEmpty());
        assertTrue(new MostSurplus().select(req(), List.of()).isEmpty());
        assertTrue(new Balanced().select(req(), List.of()).isEmpty());
    }

    @Test
    public void tiesAreBrokenDeterministically() throws Exception {
        List<SourceCandidate> tie = List.of(cand("ZZZ", 10, 50), cand("MMM", 10, 50));
        assertEquals("MMM", new NearestFirst().select(req(), tie).get().base().getId());
        assertEquals("MMM", new MostSurplus().select(req(), tie).get().base().getId());
    }

    private List<VehicleOption> options() throws Exception {
        VehicleStatus a = VehicleStatus.AVAILABLE;
        return List.of(
                new VehicleOption(new Truck("TR-01", "A", "A", 5000, 45, a), 0, 100, 2000, 4.0),
                new VehicleOption(new MedicalVan("MV-01", "A", "A", 1000, 60, a), 50, 100, 400, 3.0),
                new VehicleOption(new Helicopter("HE-01", "A", "A", 500, 180, a), 0, 100, 200, 1.0),
                new VehicleOption(new FuelTruck("FT-01", "A", "A", 4000, 40, a), 0, 100, 1600, 5.0));
    }

    @Test
    public void vehicleStrategiesChooseDifferently() throws Exception {
        assertEquals("HE-01", new FastestFit().select(options(), 100).get().vehicle().getId());
        // smallest vehicle that carries 100 units: heli (200)
        assertEquals("HE-01", new SmallestFit().select(options(), 100).get().vehicle().getId());
        // 300 units: heli too small, van (400) is the smallest that fits
        assertEquals("MV-01", new SmallestFit().select(options(), 300).get().vehicle().getId());
        // 5000 units: nothing fits -> largest
        assertEquals("TR-01", new SmallestFit().select(options(), 5000).get().vehicle().getId());
        assertTrue(new FastestFit().select(List.of(), 1).isEmpty());
        assertTrue(new SmallestFit().select(List.of(), 1).isEmpty());
    }

    @Test
    public void factoryResolvesNamesAndRejectsUnknown() throws Exception {
        for (String n : Strategies.SOURCE_NAMES) {
            assertEquals(n, Strategies.source(n.toLowerCase()).name());
        }
        for (String n : Strategies.VEHICLE_NAMES) {
            assertEquals(n, Strategies.vehicle(n).name());
        }
        try {
            Strategies.source("RANDOM");
            org.junit.Assert.fail();
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("NEAREST"));
        }
        try {
            Optional<VehicleOption> none = Strategies.vehicle("ZIGZAG").select(List.of(), 1);
            org.junit.Assert.fail("unreachable " + none);
        } catch (ValidationException e) {
            assertTrue(e.getMessage().contains("FASTEST"));
        }
    }
}
