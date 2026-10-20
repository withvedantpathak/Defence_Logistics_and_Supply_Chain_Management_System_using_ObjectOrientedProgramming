package com.dlscms.model;

import static org.junit.Assert.assertEquals;

import com.dlscms.exception.InsufficientStockException;
import com.dlscms.exception.ValidationException;
import org.junit.Test;

public class InventoryItemTest {
    private InventoryItem item(int qty, int reserved, int reorder) throws Exception {
        return new InventoryItem("PUN", "MED-KIT", qty, reserved, reorder);
    }

    @Test
    public void availableSurplusShortage() throws Exception {
        InventoryItem i = item(200, 20, 120);
        assertEquals(180, i.available());
        assertEquals(60, i.surplus());
        assertEquals(0, i.shortage());
        InventoryItem low = item(40, 0, 120);
        assertEquals(0, low.surplus());
        assertEquals(80, low.shortage());
    }

    @Test
    public void criticalMayDipToHalfReorderLevel() throws Exception {
        InventoryItem i = item(130, 0, 120);
        assertEquals(10, i.donatable(Priority.ROUTINE));
        assertEquals(70, i.donatable(Priority.CRITICAL));
    }

    @Test
    public void reserveDispatchReceiveConserveStock() throws Exception {
        InventoryItem i = item(100, 0, 10);
        i.reserve(30);
        assertEquals(70, i.available());
        assertEquals(100, i.getQty());
        i.dispatchOut(30);
        assertEquals(70, i.getQty());
        assertEquals(0, i.getReserved());
        i.receive(5);
        assertEquals(75, i.getQty());
    }

    @Test(expected = InsufficientStockException.class)
    public void reserveMoreThanAvailableFails() throws Exception {
        item(10, 5, 0).reserve(6);
    }

    @Test(expected = InsufficientStockException.class)
    public void adjustBelowReservedFails() throws Exception {
        item(10, 8, 0).adjust(-3);
    }

    @Test
    public void adjustWithinLimitsWorks() throws Exception {
        InventoryItem i = item(10, 2, 0);
        i.adjust(-8);
        assertEquals(2, i.getQty());
        i.adjust(15);
        assertEquals(17, i.getQty());
    }

    @Test(expected = ValidationException.class)
    public void reservedCannotExceedQuantity() throws Exception {
        item(5, 6, 0);
    }

    @Test
    public void csvRoundTrip() throws Exception {
        InventoryItem i = item(40, 0, 120);
        InventoryItem back = InventoryItem.fromCsv(com.dlscms.util.Csv.split(i.toCsvRow()));
        assertEquals(i.toCsvRow(), back.toCsvRow());
    }

    @Test
    public void inventoryContainerFindsAndCreates() throws Exception {
        Inventory inv = new Inventory();
        inv.put(item(40, 0, 120));
        assertEquals(40, inv.find("PUN", "MED-KIT").get().getQty());
        assertEquals(0, inv.getOrCreate("PUN", "OXY-CYL").getQty());
        assertEquals(2, inv.forBase("PUN").size());
        assertEquals(40, inv.totalUnits("PUN"));
        try {
            inv.put(item(1, 0, 1));
            org.junit.Assert.fail("duplicate must be rejected");
        } catch (ValidationException expected) {
            // ok
        }
    }
}
