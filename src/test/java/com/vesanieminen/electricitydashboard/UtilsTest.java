package com.vesanieminen.electricitydashboard;

import com.opencsv.exceptions.CsvValidationException;
import com.vesanieminen.froniusvisualizer.util.Utils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateFixedElectricityPrice;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateSpotElectricityPriceDetails;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.getFingridUsageData;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.readSpotFileAndUpdateSpotData;
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

    @Test
    public void testFixedConsumptionCalculation() throws IOException, ParseException, CsvValidationException {
        var consumptionFile = Files.newInputStream(Paths.get("src/main/resources/META-INF/resources/data/consumption-test.csv"));
        final var fingridConsumptionFile = getFingridUsageData(consumptionFile, true);
        final var fixedCost = calculateFixedElectricityPrice(fingridConsumptionFile.data(), 1);
        assertEquals(90.57930000000052, fixedCost);
    }

    @Test
    public void testConsumptionCalculationQuarterPrices() throws IOException, ParseException, CsvValidationException {
        var consumptionFile = Files.newInputStream(Paths.get("src/main/resources/META-INF/resources/data/consumption-test.csv"));
        final var fingridConsumptionFile = getFingridUsageData(consumptionFile, true);
        // Initialize the calculator data:
        readSpotFileAndUpdateSpotData("src/main/resources/META-INF/resources/data/2025-jan-oct-quarter-prices.json");
        final var from = LocalDateTime.parse("2025-01-01T00:00");
        final var to = LocalDateTime.parse("2025-09-30T23:00");
        final var spotCalculation = calculateSpotElectricityPriceDetails(fingridConsumptionFile.data(), 0, false, from.atZone(fiZoneID).toInstant(), to.atZone(fiZoneID).toInstant(), true);
        assertEquals(25678.342000000008, spotCalculation.totalSpotPrice);
        assertEquals(295.8227254999998, spotCalculation.totalCost);
        assertEquals(9057.930000000051, spotCalculation.totalConsumption);
        assertEquals(Instant.parse("2024-12-31T22:00:00Z"), spotCalculation.start);
        assertEquals(Instant.parse("2025-09-30T20:00:00Z"), spotCalculation.end);
    }

    @Test
    public void testConsumptionCalculationHourPrices() throws IOException, ParseException, CsvValidationException {
        var consumptionFile = Files.newInputStream(Paths.get("src/main/resources/META-INF/resources/data/consumption-test.csv"));
        final var fingridConsumptionFile = getFingridUsageData(consumptionFile, false);
        // Initialize the calculator data:
        readSpotFileAndUpdateSpotData("src/main/resources/META-INF/resources/data/2025-jan-oct-hour-prices.json");
        final var from = LocalDateTime.parse("2025-01-01T00:00");
        final var to = LocalDateTime.parse("2025-09-30T23:00");
        final var spotCalculation = calculateSpotElectricityPriceDetails(fingridConsumptionFile.data(), 0, false, from.atZone(fiZoneID).toInstant(), to.atZone(fiZoneID).toInstant(), false);
        assertEquals(25678.342000000008, spotCalculation.totalSpotPrice);
        assertEquals(295.8227254999998, spotCalculation.totalCost);
        assertEquals(9057.930000000051, spotCalculation.totalConsumption);
        assertEquals(Instant.parse("2024-12-31T22:00:00Z"), spotCalculation.start);
        assertEquals(Instant.parse("2025-09-30T20:00:00Z"), spotCalculation.end);
    }

    @Test
    @Disabled
    public void testQuarterPricesAndConsumption() throws IOException, ParseException, CsvValidationException {
        var consumptionFile = Files.newInputStream(Paths.get("src/main/resources/META-INF/resources/data/quarter-consumption-test.csv"));
        final var fingridConsumptionFile = getFingridUsageData(consumptionFile, true);
        // Initialize the calculator data:
        readSpotFileAndUpdateSpotData("src/main/resources/META-INF/resources/data/2025-jan-oct-quarter-prices.json");
        final var from = LocalDateTime.parse("2025-10-01T00:00");
        final var to = LocalDateTime.parse("2025-10-01T23:45");
        final var spotCalculation = calculateSpotElectricityPriceDetails(fingridConsumptionFile.data(), 0, false, from.atZone(fiZoneID).toInstant(), to.atZone(fiZoneID).toInstant(), true);
        assertEquals(889.295, spotCalculation.totalSpotPrice);
        assertEquals(2.0468910999999994, spotCalculation.totalCost);
        assertEquals(31.05999999999999, spotCalculation.totalConsumption);
        assertEquals(Instant.parse("2025-09-30T21:00:00Z"), spotCalculation.start);
        assertEquals(Instant.parse("2025-10-01T20:45:00Z"), spotCalculation.end);
    }

    @Test
    public void testHourPricesQuarterConsumptionWithHourPrecision() throws IOException, ParseException, CsvValidationException {
        var consumptionFile = Files.newInputStream(Paths.get("src/main/resources/META-INF/resources/data/quarter-consumption-test.csv"));
        final var fingridConsumptionFile = getFingridUsageData(consumptionFile, false);
        // Initialize the calculator data:
        readSpotFileAndUpdateSpotData("src/main/resources/META-INF/resources/data/2025-jan-oct-hour-prices.json");
        final var from = LocalDateTime.parse("2025-10-01T00:00");
        final var to = LocalDateTime.parse("2025-10-01T23:45");
        final var spotCalculation = calculateSpotElectricityPriceDetails(fingridConsumptionFile.data(), 0, false, from.atZone(fiZoneID).toInstant(), to.atZone(fiZoneID).toInstant(), true);
        assertEquals(222.32399999999998, spotCalculation.totalSpotPrice);
        assertEquals(2.03624591, spotCalculation.totalCost);
        assertEquals(31.06, spotCalculation.totalConsumption);
        assertEquals(Instant.parse("2025-09-30T21:00:00Z"), spotCalculation.start);
        assertEquals(Instant.parse("2025-10-01T20:00:00Z"), spotCalculation.end);
    }

}
