package com.dlscms.model.vehicle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dlscms.exception.InvalidStateTransitionException;
import com.dlscms.exception.ValidationException;
import com.dlscms.model.SupplyCategory;
import java.util.List;
import org.junit.Test;

public class VehicleTest {
    private List<Vehicle> fleet() throws Exception {
        return List.of(
                new Truck("TR-01", "PUN", "PUN", 5000, 45, VehicleStatus.AVAILABLE),
                new FuelTruck("FT-01", "PUN", "PUN", 6000, 40, VehicleStatus.AVAILABLE),
                new MedicalVan("MV-01", "MUM", "MUM", 1000, 60, VehicleStatus.AVAILABLE),
                new Helicopter("HE-01", "MUM", "MUM", 800, 180, VehicleStatus.AVAILABLE));
    }

    @Test
    public void carryRulesDifferByType() throws Exception {
        List<Vehicle> f = fleet();
        Vehicle truck = f.get(0);
        Vehicle tanker = f.get(1);
        Vehicle van = f.get(2);
        Vehicle heli = f.get(3);
        assertTrue(truck.canCarry(SupplyCategory.MEDICAL));
        assertFalse(truck.canCarry(SupplyCategory.FUEL));
        assertTrue(tanker.canCarry(SupplyCategory.FUEL));
        assertFalse(tanker.canCarry(SupplyCategory.FOOD));
        assertTrue(van.canCarry(SupplyCategory.FOOD));
        assertFalse(van.canCarry(SupplyCategory.SPARES));
        assertFalse(heli.canCarry(SupplyCategory.FUEL));
        assertTrue(heli.canCarry(SupplyCategory.SHELTER));
    }

    @Test
    public void polymorphicCapacityAndTime() throws Exception {
        List<Vehicle> f = fleet();
        assertEquals(4000.0, f.get(0).capacityFor(SupplyCategory.SHELTER), 1e-9); // truck: 80%
        assertEquals(5000.0, f.get(0).capacityFor(SupplyCategory.FOOD), 1e-9);
        assertEquals(700.0, f.get(2).capacityFor(SupplyCategory.FOOD), 1e-9);      // van food: 70%
        // same 120 km trip, different vehicle behaviour
        double truckH = f.get(0).estimateHours(120, SupplyCategory.MEDICAL);
        double vanH = f.get(2).estimateHours(120, SupplyCategory.MEDICAL);
        double heliH = f.get(3).estimateHours(120, SupplyCategory.MEDICAL);
        assertEquals(120 * 1.25 / 45 + 1.0, truckH, 1e-9);
        assertEquals(120 * 1.25 / 60 + 0.5, vanH, 1e-9);
        assertEquals(120 * 1.0 / 180 + 0.25, heliH, 1e-9);
        assertTrue(heliH < vanH && vanH < truckH);
    }

    @Test
    public void unitsPerTripUsesCapacityAndWeight() throws Exception {
        Vehicle van = fleet().get(2);
        assertEquals(400, van.unitsPerTrip(SupplyCategory.MEDICAL, 2.5));
        assertEquals(0, van.unitsPerTrip(SupplyCategory.FUEL, 2.5));
        assertEquals(0, van.unitsPerTrip(SupplyCategory.MEDICAL, 5000));
    }

    @Test
    public void statusLifecycle() throws Exception {
        Vehicle v = fleet().get(2);
        v.assign();
        assertEquals(VehicleStatus.ASSIGNED, v.getStatus());
        v.depart();
        assertEquals(VehicleStatus.IN_TRANSIT, v.getStatus());
        v.release("PUN");
        assertEquals(VehicleStatus.AVAILABLE, v.getStatus());
        assertEquals("PUN", v.getCurrentBaseId());
        assertEquals("MUM", v.getHomeBaseId());
    }

    @Test
    public void illegalStatusChangesRefused() throws Exception {
        Vehicle v = fleet().get(0);
        try {
            v.depart();
            fail("cannot depart before assignment");
        } catch (InvalidStateTransitionException expected) {
            // ok
        }
        v.assign();
        try {
            v.assign();
            fail("cannot assign twice");
        } catch (InvalidStateTransitionException expected) {
            // ok
        }
        try {
            v.setMaintenance(true);
            fail("cannot service a busy vehicle");
        } catch (InvalidStateTransitionException expected) {
            // ok
        }
    }

    @Test
    public void maintenanceToggle() throws Exception {
        Vehicle v = fleet().get(0);
        v.setMaintenance(true);
        assertEquals(VehicleStatus.MAINTENANCE, v.getStatus());
        v.setMaintenance(false);
        assertEquals(VehicleStatus.AVAILABLE, v.getStatus());
    }

    @Test
    public void factoryBuildsRightSubclassFromCsv() throws Exception {
        for (Vehicle v : fleet()) {
            Vehicle back = Vehicle.fromCsv(com.dlscms.util.Csv.split(v.toCsvRow()));
            assertEquals(v.getClass(), back.getClass());
            assertEquals(v.toCsvRow(), back.toCsvRow());
        }
    }

    @Test(expected = ValidationException.class)
    public void factoryRejectsUnknownType() throws Exception {
        Vehicle.fromCsv(new String[] {"X-1", "Submarine", "PUN", "PUN", "100", "10", "AVAILABLE"});
    }
}
