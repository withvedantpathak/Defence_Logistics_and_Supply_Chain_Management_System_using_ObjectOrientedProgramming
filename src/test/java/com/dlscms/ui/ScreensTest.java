package com.dlscms.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.dlscms.TestData;
import com.dlscms.exception.InvalidIdException;
import com.dlscms.model.Priority;
import com.dlscms.model.SupplyRequest;
import com.dlscms.service.LogisticsManager;
import com.dlscms.util.Style;
import org.junit.Test;

public class ScreensTest {
    private static Screens screens(LogisticsManager m) {
        return new Screens(m, new Terminal(false));
    }

    @Test
    public void dashboardShowsShortagesAndCountsOnFreshData() throws Exception {
        String s = screens(TestData.manager("small")).dashboard();
        assertTrue(s.contains("SHORTAGES (below reorder level)  8 cells"));
        assertTrue(s.contains("PUN") && s.contains("MED-KIT"));
        assertTrue(s.contains("OPEN REQUESTS  0"));
        assertTrue(s.contains("Truck 6/6"));
        assertTrue(s.contains("avg lead n/a"));
    }

    @Test
    public void alertNamesTheWorstShortage() throws Exception {
        assertEquals("PUN: MED-KIT below reorder level (40 < 120)", screens(TestData.manager("small")).firstAlert());
    }

    @Test
    public void detailScreensRenderAndRejectUnknownIds() throws Exception {
        LogisticsManager m = TestData.manager("small");
        Screens s = screens(m);
        SupplyRequest r = m.createRequest("PUN", "MED-KIT", 100, Priority.URGENT);
        m.requests().approve(r.getId());
        String d = m.fulfil(r.getId()).get(0).getId();
        assertTrue(s.base("pun").contains("Pune"));
        assertTrue(s.request(r.getId()).contains("FULFILLING"));
        assertTrue(s.delivery(d).contains("TIMELINE"));
        assertTrue(s.vehicle("TR-01").contains("Truck"));
        assertTrue(s.requests().contains(r.getId()));
        assertTrue(s.inventoryOf("PUN").contains("v SHORT -80"));
        assertTrue(s.supplies().contains("SUPPLY CATALOGUE"));
        assertTrue(s.strategies().contains("NEAREST"));
        assertTrue(s.help().contains("EXAMPLES"));
        for (String kind : new String[] {"STOCK", "SHORT", "DLV", "KPI"}) {
            assertTrue(s.report(kind).contains("REPORT " + kind));
        }
        try {
            s.base("XXX");
            fail();
        } catch (InvalidIdException expected) {
            // ok
        }
        try {
            s.report("NOPE");
            fail();
        } catch (com.dlscms.exception.ValidationException e) {
            assertTrue(e.getMessage().contains("STOCK, SHORT, DLV or KPI"));
        }
    }

    @Test
    public void statusColoursFollowMeaning() {
        assertEquals(Style.BAD, Screens.statusStyle("FAILED"));
        assertEquals(Style.BAD, Screens.statusStyle("SHORT -3"));
        assertEquals(Style.GOOD, Screens.statusStyle("DELIVERED"));
        assertEquals(Style.GOOD, Screens.statusStyle("OK +10"));
        assertEquals(Style.WARN, Screens.statusStyle("IN_TRANSIT"));
        assertEquals(Style.NORMAL, Screens.statusStyle("whatever"));
    }

}
