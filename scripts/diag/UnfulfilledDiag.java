import com.dlscms.model.RequestStatus;
import com.dlscms.model.SupplyRequest;
import com.dlscms.model.vehicle.Vehicle;
import com.dlscms.persistence.DataStore;
import com.dlscms.service.LogisticsManager;
import com.dlscms.service.World;
import com.dlscms.sim.RandomSource;
import com.dlscms.sim.RandomVehicle;
import com.dlscms.sim.Scenario;
import com.dlscms.sim.Simulator;
import com.dlscms.strategy.Strategies;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

/**
 * Diagnostic (not part of the application): after one simulation run, classifies every unfulfilled request by supply
 * category and by whether any available vehicle exists. Output: one CSV line per (category, reason) with counts.
 * Usage: java -cp dist/dlscms.jar:out/diag UnfulfilledDiag <dataset-dir> <scale> <seed> <SRC> <VEH> [replan]
 */
public final class UnfulfilledDiag {
    private UnfulfilledDiag() { }

    public static void main(String[] a) throws Exception {
        Path dir = Path.of(a[0]);
        long seed = Long.parseLong(a[2]);
        World w = DataStore.load(dir);
        LogisticsManager m = new LogisticsManager(w,
                a[3].equals("RANDOM") ? new RandomSource(seed * 1000L + 7) : Strategies.source(a[3]),
                a[4].equals("RANDOM") ? new RandomVehicle(seed * 1000L + 8) : Strategies.vehicle(a[4]));
        new Simulator(m, Scenario.load(dir)).run(a[1], seed);
        if (a.length > 5 && a[5].equals("replan")) {
            m.fulfilAll();   // one extra planning pass at the horizon end, to test the end-of-horizon explanation
        }
        Map<String, Integer> counts = new TreeMap<>();
        for (SupplyRequest q : w.requests.all()) {
            if (q.getStatus() == RequestStatus.FULFILLED) {
                continue;
            }
            int spare = 0;
            for (var c : m.inventory().candidates(w.supplies.get(q.getSupplyId()),
                    w.bases.get(q.getRequesterBaseId()), q.getPriority())) {
                spare += c.surplus();
            }
            String reason;
            if (q.remaining() > spare) {
                reason = "stock-limited";
            } else if (q.getStatus() == RequestStatus.APPROVED) {
                boolean idleCapable = false;
                for (var c : m.inventory().candidates(w.supplies.get(q.getSupplyId()),
                        w.bases.get(q.getRequesterBaseId()), q.getPriority())) {
                    if (!m.fleet().options(w.supplies.get(q.getSupplyId()), c.base(),
                            w.bases.get(q.getRequesterBaseId())).isEmpty()) {
                        idleCapable = true;
                    }
                }
                reason = idleCapable ? "waiting-despite-idle-vehicle" : "waiting-for-vehicle";
            } else {
                reason = "in-flight";
            }
            counts.merge(w.supplies.get(q.getSupplyId()).getCategory() + "," + reason, 1, Integer::sum);
        }
        for (var e : counts.entrySet()) {
            System.out.println(a[1] + "," + seed + "," + a[3] + "," + a[4] + "," + e.getKey() + "," + e.getValue());
        }
        int fuelTrucks = 0;
        for (Vehicle v : w.vehicles.all()) {
            if (v.getClass().getSimpleName().equals("FuelTruck")) {
                fuelTrucks++;
            }
        }
        System.out.println(a[1] + "," + seed + "," + a[3] + "," + a[4] + ",FLEET,fuel-trucks," + fuelTrucks);
    }
}
