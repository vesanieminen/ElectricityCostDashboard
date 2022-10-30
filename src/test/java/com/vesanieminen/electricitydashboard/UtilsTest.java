package com.vesanieminen.electricitydashboard;

import org.junit.Test;

import static com.vesanieminen.froniusvisualizer.util.Utils.dateTimeFormatter;
import static com.vesanieminen.froniusvisualizer.util.Utils.fiZoneID;
import static com.vesanieminen.froniusvisualizer.util.Utils.getCurrentInstantDayPrecisionFinnishZone;
import static org.junit.Assert.assertEquals;

public class UtilsTest {

    @Test
    public void test() {
        final var currentInstantDayPrecision = getCurrentInstantDayPrecisionFinnishZone();
        final var zonedDateTime = currentInstantDayPrecision.atZone(fiZoneID);
        final var dateTimeString = zonedDateTime.format(dateTimeFormatter);
        assertEquals("00:00", dateTimeString.split(" ")[1]);
    }

}
