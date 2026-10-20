package com.dlscms;

import com.dlscms.persistence.DataStore;
import com.dlscms.service.LogisticsManager;
import com.dlscms.service.World;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Shared test helpers (not a test class itself). */
public final class TestData {
    private TestData() { }

    /** @return path of a generated dataset folder, e.g. data/small */
    public static Path dir(String scale) {
        return Paths.get("data", scale);
    }

    /** @return a freshly loaded world of the given scale */
    public static World world(String scale) throws Exception {
        return DataStore.load(dir(scale));
    }

    /** @return a manager (NEAREST / FASTEST) over a freshly loaded world */
    public static LogisticsManager manager(String scale) throws Exception {
        return LogisticsManager.forWorld(world(scale));
    }
}
