package com.vesanieminen.electricitydashboard;

import com.vesanieminen.froniusvisualizer.util.Utils;
import org.junit.Test;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

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

    @Test
    public void testGetNextHour() {
        ZonedDateTime now = ZonedDateTime.now(Utils.fiZoneID);
        ZonedDateTime nextHour = Utils.getNextHour();
        assertEquals(nextHour.getHour(), now.plusHours(1).getHour());
        final var between = Duration.between(now, nextHour);
        final var seconds = between.getSeconds();
        final var l = TimeUnit.HOURS.toSeconds(1);
    }

}
