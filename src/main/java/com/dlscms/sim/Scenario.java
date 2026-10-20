package com.dlscms.sim;

import com.dlscms.exception.DataFormatException;
import com.dlscms.exception.LogisticsException;
import com.dlscms.model.Priority;
import com.dlscms.util.Csv;
import com.dlscms.util.Validator;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** A time-ordered stream of requests loaded from {@code scenario_requests.csv}. */
public final class Scenario {
    private static final String HEADER = "id,requester_base,supply,qty,priority,day,hour";
    private final List<ScenarioRequest> requests;

    private Scenario(List<ScenarioRequest> requests) {
        this.requests = requests;
    }

    /**
     * Loads the scenario stored in a dataset folder.
     *
     * @param dir dataset folder containing scenario_requests.csv
     * @return the scenario sorted by arrival hour (ties keep file order)
     * @throws DataFormatException if the file is missing or malformed (message names the line)
     */
    public static Scenario load(Path dir) throws DataFormatException {
        Path f = dir.resolve("scenario_requests.csv");
        List<String> lines;
        try {
            lines = Files.readAllLines(f, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DataFormatException("Cannot read " + f + ": " + e.getMessage(), e);
        }
        if (lines.isEmpty() || !lines.get(0).trim().equals(HEADER)) {
            throw new DataFormatException("Bad header in scenario_requests.csv (expected: " + HEADER + ")");
        }
        List<ScenarioRequest> out = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).isBlank()) {
                continue;
            }
            String[] c = Csv.split(lines.get(i));
            try {
                if (c.length != 7) {
                    throw new DataFormatException("expected 7 columns, found " + c.length);
                }
                long hour = Validator.nonNegative(Validator.parseInt(c[5], "day"), "day") * 24L
                        + Validator.nonNegative(Validator.parseInt(c[6], "hour"), "hour");
                out.add(new ScenarioRequest(c[0], c[1].toUpperCase(), c[2].toUpperCase(),
                        Validator.positive(Validator.parseInt(c[3], "qty"), "qty"), Priority.parse(c[4]), hour));
            } catch (LogisticsException e) {
                throw new DataFormatException("Data format error in scenario_requests.csv line " + (i + 1) + ": "
                        + e.getMessage(), e);
            }
        }
        out.sort(Comparator.comparingLong(ScenarioRequest::hour));
        return new Scenario(out);
    }

    /** @return the requests in arrival order */
    public List<ScenarioRequest> requests() {
        return requests;
    }

    /** @return the arrival hour of the last request (0 if empty) */
    public long lastHour() {
        return requests.isEmpty() ? 0 : requests.get(requests.size() - 1).hour();
    }
}
