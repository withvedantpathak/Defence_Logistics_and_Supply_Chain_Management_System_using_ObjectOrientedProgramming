package com.dlscms.service;

import com.dlscms.exception.InsufficientStockException;
import com.dlscms.exception.InvalidIdException;
import com.dlscms.exception.LogisticsException;
import com.dlscms.exception.ValidationException;
import com.dlscms.model.Base;
import com.dlscms.model.InventoryItem;
import com.dlscms.model.Priority;
import com.dlscms.model.Supply;
import com.dlscms.strategy.SourceCandidate;
import java.util.ArrayList;
import java.util.List;

/** Reads and changes stock levels and finds donor bases. */
public final class InventoryService {
    private final World world;

    /** @param world the shared network state */
    public InventoryService(World world) {
        this.world = world;
    }

    /**
     * Cell for a base/supply pair, created empty if it does not exist yet.
     *
     * @param baseId   base id
     * @param supplyId supply id
     * @return the cell
     * @throws LogisticsException if either id is unknown
     */
    public InventoryItem cell(String baseId, String supplyId) throws LogisticsException {
        Base b = world.bases.get(baseId);
        Supply s = world.supplies.get(supplyId);
        return world.inventory.getOrCreate(b.getId(), s.getId());
    }

    /**
     * @param baseId base id
     * @return the base's cells ordered by supply id
     * @throws InvalidIdException if the base is unknown
     */
    public List<InventoryItem> forBase(String baseId) throws InvalidIdException {
        return world.inventory.forBase(world.bases.get(baseId).getId());
    }

    /**
     * Adds or removes stock manually.
     *
     * @param baseId   base id
     * @param supplyId supply id
     * @param delta    units to add (negative to remove)
     * @return the updated cell
     * @throws LogisticsException if ids are unknown, stock would go below reserved, or capacity is exceeded
     */
    public InventoryItem adjust(String baseId, String supplyId, int delta) throws LogisticsException {
        InventoryItem it = cell(baseId, supplyId);
        Base b = world.bases.get(baseId);
        if (delta > 0 && world.inventory.totalUnits(b.getId()) + delta > b.getCapacityUnits()) {
            throw new ValidationException("Adding " + delta + " would exceed storage capacity of "
                    + b.getId() + " (" + b.getCapacityUnits() + " units)");
        }
        it.adjust(delta);
        world.setDirty(true);
        return it;
    }

    /**
     * Overwrites stock and optionally the reorder level.
     *
     * @param baseId   base id
     * @param supplyId supply id
     * @param qty      new quantity
     * @param reorder  new reorder level, or -1 to keep the current one
     * @return the updated cell
     * @throws LogisticsException if invalid, below reserved, or capacity exceeded
     */
    public InventoryItem set(String baseId, String supplyId, int qty, int reorder) throws LogisticsException {
        InventoryItem it = cell(baseId, supplyId);
        Base b = world.bases.get(baseId);
        int others = world.inventory.totalUnits(b.getId()) - it.getQty();
        if (others + qty > b.getCapacityUnits()) {
            throw new ValidationException("Quantity would exceed storage capacity of " + b.getId()
                    + " (" + b.getCapacityUnits() + " units)");
        }
        it.set(qty, reorder < 0 ? it.getReorderLevel() : reorder);
        world.setDirty(true);
        return it;
    }

    /**
     * Bases (other than the destination) that can give away stock of a supply.
     *
     * @param supply   the supply
     * @param dest     requesting base (excluded)
     * @param priority priority of the request (CRITICAL may dip below the reorder level)
     * @return candidates ordered by base id
     */
    public List<SourceCandidate> candidates(Supply supply, Base dest, Priority priority) {
        List<SourceCandidate> out = new ArrayList<>();
        for (Base b : world.bases.all()) {
            if (b.getId().equals(dest.getId())) {
                continue;
            }
            world.inventory.find(b.getId(), supply.getId()).ifPresent(it -> {
                int give = it.donatable(priority);
                if (give > 0) {
                    out.add(new SourceCandidate(b, give, b.distanceKm(dest)));
                }
            });
        }
        return out;
    }

    /**
     * @param supplyId supply id
     * @return units of the supply on hand across the network (excluding stock in transit)
     */
    public int totalOnHand(String supplyId) {
        int sum = 0;
        for (InventoryItem it : world.inventory.forSupply(supplyId)) {
            sum += it.getQty();
        }
        return sum;
    }

    /**
     * Surplus check that only throws when asked for more than is available.
     *
     * @param baseId   base id
     * @param supplyId supply id
     * @param qty      units needed
     * @throws LogisticsException if fewer than qty units are available
     */
    public void requireAvailable(String baseId, String supplyId, int qty) throws LogisticsException {
        InventoryItem it = cell(baseId, supplyId);
        if (it.available() < qty) {
            throw new InsufficientStockException("Insufficient stock: " + it.getBaseId() + " has "
                    + it.available() + " " + it.getSupplyId() + " available, " + qty + " needed");
        }
    }
}
