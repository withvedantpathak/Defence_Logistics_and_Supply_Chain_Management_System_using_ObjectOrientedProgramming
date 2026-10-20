package com.dlscms.sim;

import com.dlscms.exception.LogisticsException;
import com.dlscms.persistence.DataStore;
import com.dlscms.service.LogisticsManager;
import com.dlscms.service.World;
import com.dlscms.strategy.SourceSelectionStrategy;
import com.dlscms.strategy.Strategies;
import com.dlscms.strategy.VehicleAssignmentStrategy;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Runs simulation grids and writes one CSV row per run. */
public final class Experiment {
    /** Donor strategies in the grid (the last is the uncoordinated baseline). */
    public static final List<String> SOURCES = List.of("NEAREST", "SURPLUS", "BALANCED", "RANDOM");
    /** Vehicle strategies in the grid (the last is the uncoordinated baseline). */
    public static final List<String> VEHICLES = List.of("SMALLEST", "FASTEST", "RANDOM");
    /** Scales in the grid. */
    public static final List<String> SCALES = List.of("small", "medium", "large");

    private Experiment() { }

    /**
     * Runs one configuration on a freshly loaded world.
     *
     * @param dir     dataset folder
     * @param scale   label
     * @param seed    dataset seed label
     * @param src     donor strategy name (NEAREST, SURPLUS, BALANCED or RANDOM)
     * @param veh     vehicle strategy name (SMALLEST, FASTEST or RANDOM)
     * @param rndSeed seed for the RANDOM baselines
     * @return the metrics
     * @throws LogisticsException on load errors or unknown strategy names
     */
    public static SimResult runOne(Path dir, String scale, long seed, String src, String veh, long rndSeed)
            throws LogisticsException {
        World w = DataStore.load(dir);
        SourceSelectionStrategy s = src.equals("RANDOM") ? new RandomSource(rndSeed) : Strategies.source(src);
        VehicleAssignmentStrategy v = veh.equals("RANDOM") ? new RandomVehicle(rndSeed + 1) : Strategies.vehicle(veh);
        LogisticsManager m = new LogisticsManager(w, s, v);
        return new Simulator(m, Scenario.load(dir)).run(scale, seed);
    }

    /**
     * Runs the full grid: every seed folder under {@code root/<seed>/<scale>} x every strategy pair.
     *
     * @param root  folder holding one sub-folder per seed, each with small/medium/large datasets
     * @param seeds seeds 1..seeds
     * @param out   CSV file to write
     * @param log   progress sink
     * @throws LogisticsException on any load or simulation error
     * @throws IOException        if the output cannot be written
     */
    public static void runGrid(Path root, int seeds, Path out, PrintStream log) throws LogisticsException, IOException {
        List<String> rows = new ArrayList<>();
        rows.add(SimResult.csvHeader());
        int n = 0;
        for (int seed = 1; seed <= seeds; seed++) {
            for (String scale : SCALES) {
                Path dir = root.resolve(String.valueOf(seed)).resolve(scale);
                for (String src : SOURCES) {
                    for (String veh : VEHICLES) {
                        rows.add(runOne(dir, scale, seed, src, veh, seed * 1000L + 7).toCsvRow());
                        n++;
                    }
                }
            }
            log.println("seed " + seed + "/" + seeds + " done (" + n + " runs)");
        }
        if (out.getParent() != null) {
            Files.createDirectories(out.getParent());
        }
        Files.write(out, (String.join("\n", rows) + "\n").getBytes(StandardCharsets.UTF_8));
    }
}
