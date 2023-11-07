package com.vesanieminen.electricitydashboard;

import com.opencsv.exceptions.CsvValidationException;
import com.vesanieminen.froniusvisualizer.util.Utils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateFixedElectricityPrice;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.getFingridUsageData;
import static com.vesanieminen.froniusvisualizer.util.Utils.calculateMonthsInvolved;
import static com.vesanieminen.froniusvisualizer.util.Utils.dateTimeFormatter;
import static com.vesanieminen.froniusvisualizer.util.Utils.fiZoneID;
import static com.vesanieminen.froniusvisualizer.util.Utils.getCurrentInstantDayPrecisionFinnishZone;
import static com.vesanieminen.froniusvisualizer.util.Utils.isAfter_13_45;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    @Disabled
    public void printSpotdataSizes() throws IOException, URISyntaxException, InterruptedException {
        //getSpotDataSahkoTK();
        // final var spotData = getSpotData();
        // System.out.println("sizeof spotdata: " + sizeOf(spotData));

        //updateRealtimeData();
        //final var latest7Days = getLatest7Days();
        //System.out.println("sizeof fingrid data: " + sizeOf(latest7Days));

        //updateNordpoolData();
        //final var latest7Days = NordpoolSpotService.getLatest7Days();
        //System.out.println("sizeof nordpool data: " + sizeOf(latest7Days));
    }

    @Test
    public void testReadFileWithNulls() throws IOException, ParseException, CsvValidationException {
        //final var fingridUsageData = getFingridUsageData("src/main/resources/META-INF/resources/data/consumption.csv");
        final var fingridUsageData1 = getFingridUsageData("src/main/resources/META-INF/resources/data/consumption-with-null-only.csv");
        final var cost = calculateFixedElectricityPrice(fingridUsageData1.data(), 36);
        assertEquals(0.414, cost);
    }

    @Test
    public void testIsAfter_13_50() {
        final var isAfter = isAfter_13_45(ZonedDateTime.of(2023, 9, 11, 20, 52, 0, 0, fiZoneID));
        assertTrue(isAfter);
        final var isBefore = isAfter_13_45(ZonedDateTime.of(2023, 9, 11, 10, 52, 0, 0, fiZoneID));
        assertFalse(isBefore);
        final var isAfterExactly = isAfter_13_45(ZonedDateTime.of(2023, 10, 2, 13, 49, 7, 0, fiZoneID));
        assertTrue(isAfterExactly);
    }

    @Test
    public void test2() {
        final var now = ZonedDateTime.now(fiZoneID);
        final var b = !isAfter_13_45(now);
        int i = 0;
    }

    @Test
    void keepEveryNthItem() {
        final var integers = IntStream.range(0, 20).boxed().toList();
        final var result = Utils.keepEveryNthItem(integers, 10, 9);
        assertEquals(2, result.size());
        assertEquals(9, result.get(0));
        assertEquals(19, result.get(1));
    }

    @Test
    void testMonthsBetween() {
        var start = Instant.from(ZonedDateTime.of(2023, 9, 10, 20, 52, 0, 0, fiZoneID));
        var end = Instant.from(ZonedDateTime.of(2023, 10, 11, 20, 52, 0, 0, fiZoneID));
        var between = calculateMonthsInvolved(start, end);
        assertEquals(2, between);

        start = Instant.from(ZonedDateTime.of(2023, 9, 12, 20, 52, 0, 0, fiZoneID));
        end = Instant.from(ZonedDateTime.of(2023, 10, 11, 20, 52, 0, 0, fiZoneID));
        between = calculateMonthsInvolved(start, end);
        assertEquals(2, between);

        start = Instant.from(ZonedDateTime.of(2023, 10, 10, 20, 52, 0, 0, fiZoneID));
        end = Instant.from(ZonedDateTime.of(2023, 10, 11, 20, 52, 0, 0, fiZoneID));
        between = calculateMonthsInvolved(start, end);
        assertEquals(1, between);

        start = Instant.from(ZonedDateTime.of(2023, 10, 28, 0, 0, 0, 0, fiZoneID));
        end = Instant.from(ZonedDateTime.of(2023, 10, 29, 0, 0, 0, 0, fiZoneID));
        between = calculateMonthsInvolved(start, end);
        assertEquals(1, between);
    }

    @Test
    void testExtra15Min_Error() {

    }

}
