package com.dlscms.model;

import com.dlscms.exception.ValidationException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/** All inventory cells of the network, indexed by base id then supply id (nested maps, sorted). */
public final class Inventory {
    private final Map<String, Map<String, InventoryItem>> cells = new TreeMap<>();

    /**
     * Adds a cell.
     *
     * @param item the item
     * @throws ValidationException if the cell already exists
     */
    public void put(InventoryItem item) throws ValidationException {
        Map<String, InventoryItem> row = cells.computeIfAbsent(item.getBaseId(), k -> new TreeMap<>());
        if (row.containsKey(item.getSupplyId())) {
            throw new ValidationException("duplicate inventory row " + item.getBaseId() + "/" + item.getSupplyId());
        }
        row.put(item.getSupplyId(), item);
    }

    /**
     * @param baseId   base id
     * @param supplyId supply id
     * @return the cell if present
     */
    public Optional<InventoryItem> find(String baseId, String supplyId) {
        Map<String, InventoryItem> row = cells.get(baseId);
        return row == null ? Optional.empty() : Optional.ofNullable(row.get(supplyId));
    }

    /**
     * Returns the cell, creating an empty one (zero stock, reorder level 0) if missing.
     *
     * @param baseId   base id
     * @param supplyId supply id
     * @return the cell
     * @throws ValidationException if ids are invalid
     */
    public InventoryItem getOrCreate(String baseId, String supplyId) throws ValidationException {
        Optional<InventoryItem> existing = find(baseId, supplyId);
        if (existing.isPresent()) {
            return existing.get();
        }
        InventoryItem created = new InventoryItem(baseId, supplyId, 0, 0, 0);
        put(created);
        return created;
    }

    /**
     * @param baseId base id
     * @return the base's cells ordered by supply id
     */
    public List<InventoryItem> forBase(String baseId) {
        Map<String, InventoryItem> row = cells.get(baseId);
        return row == null ? List.of() : new ArrayList<>(row.values());
    }

    /** @return every cell ordered by base then supply */
    public List<InventoryItem> all() {
        List<InventoryItem> out = new ArrayList<>();
        for (Map<String, InventoryItem> row : cells.values()) {
            out.addAll(row.values());
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * @param supplyId supply id
     * @return cells of that supply across all bases
     */
    public List<InventoryItem> forSupply(String supplyId) {
        List<InventoryItem> out = new ArrayList<>();
        for (Map<String, InventoryItem> row : cells.values()) {
            InventoryItem it = row.get(supplyId);
            if (it != null) {
                out.add(it);
            }
        }
        return out;
    }

    /**
     * @param baseId base id
     * @return total units on hand at the base
     */
    public int totalUnits(String baseId) {
        int sum = 0;
        for (InventoryItem it : forBase(baseId)) {
            sum += it.getQty();
        }
        return sum;
    }

    /** @return all cells as a persistable collection */
    public Collection<InventoryItem> items() {
        return all();
    }
}
