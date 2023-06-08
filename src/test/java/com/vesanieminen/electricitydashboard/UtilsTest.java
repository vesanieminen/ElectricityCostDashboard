package com.vesanieminen.electricitydashboard;

import com.vesanieminen.froniusvisualizer.util.Utils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

import static com.vesanieminen.froniusvisualizer.util.Utils.dateTimeFormatter;
import static com.vesanieminen.froniusvisualizer.util.Utils.fiZoneID;
import static com.vesanieminen.froniusvisualizer.util.Utils.getCurrentInstantDayPrecisionFinnishZone;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
    @Disabled
    public void testReadFileWithNulls() throws IOException, ParseException {
        //final var fingridUsageData = getFingridUsageData("src/main/resources/META-INF/resources/data/consumption.csv");
        // TODO: fixme:
        //final var fingridUsageData1 = getFingridUsageData("src/main/resources/META-INF/resources/data/consumption-with-null-only.csv");
        //final var cost = calculateFixedElectricityPrice(fingridUsageData1.data(), 36);
    }

}
