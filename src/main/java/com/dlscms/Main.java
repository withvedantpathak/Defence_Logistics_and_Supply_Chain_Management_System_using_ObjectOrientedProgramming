package com.dlscms;

import com.dlscms.exception.LogisticsException;
import com.dlscms.persistence.DataStore;
import com.dlscms.service.LogisticsManager;
import com.dlscms.ui.Terminal;
import com.dlscms.ui.TerminalApp;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Entry point of the Defence Logistics and Supply Chain Management System (DLSCMS).
 *
 * <p>Options: {@code --plain} (no colours), {@code --demo} (run the scripted scenario and exit),
 * {@code --script <file>} (run commands from a file), {@code --data <folder>} (default data/small).
 */
public final class Main {

    /** Application name shown in the banner. */
    public static final String NAME = "DLSCMS - Defence Logistics Terminal";

    private Main() { }

    /**
     * Program entry.
     *
     * @param args command-line options
     */
    public static void main(String[] args) {
        System.exit(run(args, System.in, new PrintStream(System.out, true, StandardCharsets.UTF_8)));
    }

    /**
     * Runs the application.
     *
     * @param args command-line options
     * @param in   standard input
     * @param out  output stream
     * @return process exit code (0 ok, 1 error, 2 bad usage)
     */
    public static int run(String[] args, java.io.InputStream in, PrintStream out) {
        boolean plain = false;
        boolean demo = false;
        String script = null;
        boolean sim = false;
        boolean grid = false;
        String src = "NEAREST";
        String veh = "FASTEST";
        Path root = Paths.get("results", "tmp", "seeds");
        Path outFile = null;
        int seeds = 10;
        Path data = Paths.get("data", "small");
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--plain": plain = true; break;
                case "--demo": demo = true; break;
                case "--script":
                    if (++i >= args.length) {
                        return usage(out, "--script needs a file");
                    }
                    script = args[i];
                    break;
                case "--data":
                    if (++i >= args.length) {
                        return usage(out, "--data needs a folder");
                    }
                    data = Paths.get(args[i]);
                    break;
                case "--sim": sim = true; break;
                case "--sim-grid": grid = true; break;
                case "--src":
                    if (++i >= args.length) {
                        return usage(out, "--src needs a strategy name");
                    }
                    src = args[i].toUpperCase();
                    break;
                case "--veh":
                    if (++i >= args.length) {
                        return usage(out, "--veh needs a strategy name");
                    }
                    veh = args[i].toUpperCase();
                    break;
                case "--seeds":
                    if (++i >= args.length || !args[i].matches("[0-9]{1,3}")) {
                        return usage(out, "--seeds needs a number");
                    }
                    seeds = Integer.parseInt(args[i]);
                    break;
                case "--data-root":
                    if (++i >= args.length) {
                        return usage(out, "--data-root needs a folder");
                    }
                    root = Paths.get(args[i]);
                    break;
                case "--out":
                    if (++i >= args.length) {
                        return usage(out, "--out needs a file");
                    }
                    outFile = Paths.get(args[i]);
                    break;
                case "--version":
                    out.println(NAME + " v1.0.0");
                    return 0;
                case "--help":
                case "-h":
                    return usage(out, null);
                default:
                    return usage(out, "Unknown option " + args[i]);
            }
        }
        try {
            if (grid) {
                com.dlscms.sim.Experiment.runGrid(root, seeds, outFile == null ? Paths.get("results", "raw", "runs.csv")
                        : outFile, out);
                return 0;
            }
            if (sim) {
                com.dlscms.sim.SimResult r = com.dlscms.sim.Experiment.runOne(data, data.getFileName().toString(), 42,
                        src, veh, 1007);
                out.println(com.dlscms.sim.SimResult.csvHeader());
                out.println(r.toCsvRow());
                return 0;
            }
            LogisticsManager m = LogisticsManager.forWorld(DataStore.load(data));
            Terminal term = new Terminal(!plain && !demo && script == null);
            BufferedReader reader;
            boolean interactive;
            if (script != null) {
                reader = Files.newBufferedReader(Paths.get(script), StandardCharsets.UTF_8);
                interactive = false;
            } else if (demo) {
                reader = new BufferedReader(new java.io.StringReader("DEMO\nEXIT\n"));
                interactive = false;
            } else {
                reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                interactive = true;
            }
            new TerminalApp(m, data, term, reader, out, interactive).run();
            return 0;
        } catch (LogisticsException e) {
            out.println("ERR > " + e.getMessage());
            return 1;
        } catch (IOException e) {
            out.println("ERR > cannot read input: " + e.getMessage());
            return 1;
        }
    }

    private static int usage(PrintStream out, String problem) {
        if (problem != null) {
            out.println("ERR > " + problem);
        }
        out.println(NAME + "\nUsage: java -jar dlscms.jar [--plain] [--demo] [--script <file>] [--data <folder>]\n       java -jar dlscms.jar --sim [--data <folder>] [--src NEAREST|SURPLUS|BALANCED|RANDOM] [--veh SMALLEST|FASTEST|RANDOM]\n       java -jar dlscms.jar --sim-grid [--data-root <folder>] [--seeds N] [--out <file.csv>]");
        return problem == null ? 0 : 2;
    }
}
