package com.dlscms.model;

import com.dlscms.exception.InsufficientStockException;
import com.dlscms.exception.ValidationException;
import com.dlscms.util.Csv;
import com.dlscms.util.Validator;

/** Stock of one supply at one base, with a reservation counter for planned deliveries. */
public final class InventoryItem implements Persistable {
    /** CSV header of inventory.csv. */
    public static final String HEADER = "base_id,supply_id,qty,reserved,reorder_level";

    private final String baseId;
    private final String supplyId;
    private int qty;
    private int reserved;
    private int reorderLevel;

    /**
     * @param baseId       owning base id
     * @param supplyId     supply id
     * @param qty          units on hand
     * @param reserved     units promised to planned deliveries
     * @param reorderLevel level below which the base counts as short
     * @throws ValidationException if values are inconsistent
     */
    public InventoryItem(String baseId, String supplyId, int qty, int reserved, int reorderLevel)
            throws ValidationException {
        this.baseId = Validator.id(baseId, "base id");
        this.supplyId = Validator.id(supplyId, "supply id");
        this.qty = Validator.nonNegative(qty, "quantity");
        this.reserved = Validator.nonNegative(reserved, "reserved");
        this.reorderLevel = Validator.nonNegative(reorderLevel, "reorder level");
        if (reserved > qty) {
            throw new ValidationException("reserved (" + reserved + ") exceeds quantity (" + qty + ")");
        }
    }

    /** @return base id */
    public String getBaseId() {
        return baseId;
    }

    /** @return supply id */
    public String getSupplyId() {
        return supplyId;
    }

    /** @return units on hand */
    public int getQty() {
        return qty;
    }

    /** @return units reserved for planned deliveries */
    public int getReserved() {
        return reserved;
    }

    /** @return reorder level */
    public int getReorderLevel() {
        return reorderLevel;
    }

    /** @return units on hand that are not reserved */
    public int available() {
        return qty - reserved;
    }

    /** @return available units above the reorder level (never negative) */
    public int surplus() {
        return Math.max(0, available() - reorderLevel);
    }

    /** @return units missing to reach the reorder level (never negative) */
    public int shortage() {
        return Math.max(0, reorderLevel - available());
    }

    /**
     * Units this base may give away. CRITICAL requests may draw the base down to half its reorder level.
     *
     * @param p priority of the request asking for stock
     * @return donatable units (never negative)
     */
    public int donatable(Priority p) {
        int floor = p == Priority.CRITICAL ? reorderLevel / 2 : reorderLevel;
        return Math.max(0, available() - floor);
    }

    /**
     * Reserves stock for a planned delivery.
     *
     * @param n units
     * @throws InsufficientStockException if fewer than n are available
     */
    public void reserve(int n) throws InsufficientStockException {
        if (n > available()) {
            throw new InsufficientStockException("Insufficient stock: " + baseId + " has " + available()
                    + " " + supplyId + " available, " + n + " needed");
        }
        reserved += n;
    }

    /**
     * Cancels a reservation.
     *
     * @param n units
     */
    public void release(int n) {
        if (n > reserved) {
            throw new IllegalStateException("release " + n + " > reserved " + reserved);
        }
        reserved -= n;
    }

    /**
     * Stock physically leaves (dispatch): reduces quantity and the matching reservation.
     *
     * @param n units
     */
    public void dispatchOut(int n) {
        if (n > reserved || n > qty) {
            throw new IllegalStateException("dispatchOut " + n + " exceeds reservation/qty");
        }
        qty -= n;
        reserved -= n;
    }

    /**
     * Stock arrives or is returned.
     *
     * @param n units
     */
    public void receive(int n) {
        qty += n;
    }

    /**
     * Manual correction.
     *
     * @param delta units to add (negative to remove)
     * @throws InsufficientStockException if the result would be below the reserved amount
     */
    public void adjust(int delta) throws InsufficientStockException {
        if (qty + delta < reserved) {
            throw new InsufficientStockException("Insufficient stock: " + baseId + " has " + available()
                    + " " + supplyId + " available, cannot remove " + (-delta));
        }
        qty += delta;
    }

    /**
     * Manual overwrite.
     *
     * @param newQty     units on hand
     * @param newReorder reorder level
     * @throws ValidationException        if negative
     * @throws InsufficientStockException if below the reserved amount
     */
    public void set(int newQty, int newReorder) throws ValidationException, InsufficientStockException {
        Validator.nonNegative(newQty, "quantity");
        Validator.nonNegative(newReorder, "reorder level");
        if (newQty < reserved) {
            throw new InsufficientStockException("Quantity cannot be set below reserved (" + reserved + ")");
        }
        qty = newQty;
        reorderLevel = newReorder;
    }

    @Override
    public String csvHeader() {
        return HEADER;
    }

    @Override
    public String toCsvRow() {
        return Csv.join(baseId, supplyId, qty, reserved, reorderLevel);
    }

    /**
     * Builds an item from CSV fields.
     *
     * @param f fields in header order
     * @return the item
     * @throws ValidationException if invalid
     */
    public static InventoryItem fromCsv(String[] f) throws ValidationException {
        return new InventoryItem(f[0], f[1], Validator.parseInt(f[2], "qty"),
                Validator.parseInt(f[3], "reserved"), Validator.parseInt(f[4], "reorder_level"));
    }
}
