package com.dlscms.persistence;

import com.dlscms.exception.DataFormatException;
import com.dlscms.exception.LogisticsException;
import com.dlscms.model.Base;
import com.dlscms.model.Delivery;
import com.dlscms.model.InventoryItem;
import com.dlscms.model.Persistable;
import com.dlscms.model.Supply;
import com.dlscms.model.SupplyRequest;
import com.dlscms.model.vehicle.Vehicle;
import com.dlscms.service.World;
import com.dlscms.util.Csv;
import com.dlscms.util.Validator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Loads and saves a whole {@link World} as a folder of CSV files. */
public final class DataStore {
    private static final String META_HEADER = "key,value";

    private static final CsvRepository<Base> BASES =
            new CsvRepository<>("bases.csv", Base.HEADER, Base::fromCsv);
    private static final CsvRepository<Supply> SUPPLIES =
            new CsvRepository<>("supplies.csv", Supply.HEADER, Supply::fromCsv);
    private static final CsvRepository<InventoryItem> INVENTORY =
            new CsvRepository<>("inventory.csv", InventoryItem.HEADER, InventoryItem::fromCsv);
    private static final CsvRepository<Vehicle> VEHICLES =
            new CsvRepository<>("vehicles.csv", Vehicle.HEADER, Vehicle::fromCsv);
    private static final CsvRepository<SupplyRequest> REQUESTS =
            new CsvRepository<>("requests.csv", SupplyRequest.HEADER, SupplyRequest::fromCsv);
    private static final CsvRepository<Delivery> DELIVERIES =
            new CsvRepository<>("deliveries.csv", Delivery.HEADER, Delivery::fromCsv);
    private static final CsvRepository<MetaRow> META =
            new CsvRepository<>("meta.csv", META_HEADER, f -> new MetaRow(f[0], f[1]));

    private DataStore() { }

    /** One key/value line of meta.csv. */
    private static final class MetaRow implements Persistable {
        private final String key;
        private final String value;

        MetaRow(String key, String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String csvHeader() {
            return META_HEADER;
        }

        @Override
        public String toCsvRow() {
            return Csv.join(key, value);
        }
    }

    /**
     * Loads a world from a folder and checks every cross-reference.
     *
     * @param dir data folder
     * @return the loaded world (not dirty)
     * @throws DataFormatException on any missing/malformed file or dangling reference
     */
    public static World load(Path dir) throws DataFormatException {
        if (!Files.isDirectory(dir)) {
            throw new DataFormatException("Data folder not found: " + dir);
        }
        World w = new World();
        try {
            for (Base b : BASES.load(dir, false)) {
                w.bases.add(b);
            }
            for (Supply s : SUPPLIES.load(dir, false)) {
                w.supplies.add(s);
            }
            for (InventoryItem it : INVENTORY.load(dir, false)) {
                w.bases.get(it.getBaseId());
                w.supplies.get(it.getSupplyId());
                w.inventory.put(it);
            }
            for (Vehicle v : VEHICLES.load(dir, false)) {
                w.bases.get(v.getHomeBaseId());
                w.bases.get(v.getCurrentBaseId());
                w.vehicles.add(v);
            }
            for (SupplyRequest r : REQUESTS.load(dir, true)) {
                w.bases.get(r.getRequesterBaseId());
                w.supplies.get(r.getSupplyId());
                w.requests.add(r);
            }
            for (Delivery d : DELIVERIES.load(dir, true)) {
                w.requests.get(d.getRequestId());
                w.bases.get(d.getSourceBaseId());
                w.bases.get(d.getDestBaseId());
                w.supplies.get(d.getSupplyId());
                w.vehicles.get(d.getVehicleId());
                w.deliveries.add(d);
            }
            rebuildPlannedQuantities(w);
            applyMeta(w, META.load(dir, true));
        } catch (DataFormatException e) {
            throw e;
        } catch (LogisticsException | RuntimeException e) {
            throw new DataFormatException("Inconsistent data in " + dir + ": " + e.getMessage(), e);
        }
        w.setDirty(false);
        return w;
    }

    private static void rebuildPlannedQuantities(World w) {
        for (SupplyRequest r : w.requests.all()) {
            r.setPlannedQty(0);
        }
        for (Delivery d : w.deliveries.all()) {
            if (d.getStatus().isOpen()) {
                try {
                    SupplyRequest r = w.requests.get(d.getRequestId());
                    r.setPlannedQty(r.getPlannedQty() + d.getQty());
                } catch (LogisticsException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    private static void applyMeta(World w, List<MetaRow> rows) throws LogisticsException {
        for (MetaRow m : rows) {
            switch (m.key) {
                case "clock_hour":
                    w.clock.set(Long.parseLong(m.value.trim()));
                    break;
                case "next_request":
                    w.requestIds.setNext(Validator.parseInt(m.value, "next_request"));
                    break;
                case "next_delivery":
                    w.deliveryIds.setNext(Validator.parseInt(m.value, "next_delivery"));
                    break;
                case "source_strategy":
                    w.setSourceStrategyName(m.value.trim().toUpperCase());
                    break;
                case "vehicle_strategy":
                    w.setVehicleStrategyName(m.value.trim().toUpperCase());
                    break;
                default:
                    w.extraMeta.put(m.key, m.value);
                    break;
            }
        }
    }

    /**
     * Writes the whole world to a folder (each file replaced atomically).
     *
     * @param w   world to save
     * @param dir target folder (created if missing)
     * @throws DataFormatException if a file cannot be written
     */
    public static void save(World w, Path dir) throws DataFormatException {
        BASES.save(dir, w.bases.all());
        SUPPLIES.save(dir, w.supplies.all());
        INVENTORY.save(dir, w.inventory.items());
        VEHICLES.save(dir, w.vehicles.all());
        REQUESTS.save(dir, w.requests.all());
        DELIVERIES.save(dir, w.deliveries.all());
        List<MetaRow> meta = new ArrayList<>();
        meta.add(new MetaRow("clock_hour", String.valueOf(w.clock.now())));
        meta.add(new MetaRow("next_request", String.valueOf(w.requestIds.peekNumber())));
        meta.add(new MetaRow("next_delivery", String.valueOf(w.deliveryIds.peekNumber())));
        meta.add(new MetaRow("source_strategy", w.getSourceStrategyName()));
        meta.add(new MetaRow("vehicle_strategy", w.getVehicleStrategyName()));
        for (Map.Entry<String, String> e : w.extraMeta.entrySet()) {
            meta.add(new MetaRow(e.getKey(), e.getValue()));
        }
        META.save(dir, meta);
        w.setDirty(false);
    }
}
