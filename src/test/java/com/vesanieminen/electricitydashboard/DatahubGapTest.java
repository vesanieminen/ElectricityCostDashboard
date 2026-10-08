package com.vesanieminen.electricitydashboard;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;

import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.getFingridUsageData;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Gaps (MISSING rows) in Datahub files stay gaps instead of ending the series.
 */
public class DatahubGapTest {

    private static final String HEADER = "Mittauspisteen tunnus;Tuotteen tyyppi;Resoluutio;Yksikkötyyppi;Lukeman tyyppi;Alkuaika;Määrä;Laatu\n";

    @Test
    void readingContinuesAfterMissingQuartersWhenCombiningToHours() throws Exception {
        // 15 min data with 9 missing quarters 2023-09-17T22:45Z ... 2023-09-18T00:45Z and data until 2023-09-21T20:45Z
        try (var in = Files.newInputStream(Paths.get("src/main/resources/META-INF/resources/data/15min-interval-error-at-15min.csv"))) {
            final var data = getFingridUsageData(in, false);
            assertEquals(Instant.parse("2023-09-17T00:00:00Z"), data.start());
            assertEquals(Instant.parse("2023-09-21T20:00:00Z"), data.end());
            assertTrue(data.data().containsKey(Instant.parse("2023-09-17T21:00:00Z")));
            // incomplete and missing hours are gaps
            assertFalse(data.data().containsKey(Instant.parse("2023-09-17T22:00:00Z")));
            assertFalse(data.data().containsKey(Instant.parse("2023-09-17T23:00:00Z")));
            assertFalse(data.data().containsKey(Instant.parse("2023-09-18T00:00:00Z")));
            assertTrue(data.data().containsKey(Instant.parse("2023-09-18T01:00:00Z")));
            assertEquals(22 + 24 * 3 - 1 + 21, data.data().size()); // 17.9. 00-21, 18.9. 01-23, 19.-20.9., 21.9. 00-20
        }
    }

    @Test
    void quarterValuesKeepTheirGapsAndHoursNeedNotStartOnTheHour() throws Exception {
        final StringBuilder csv = new StringBuilder(HEADER);
        Instant t = Instant.parse("2025-10-01T00:15:00Z"); // does not start on the hour
        for (int i = 0; i < 16; i++, t = t.plusSeconds(900)) {
            final boolean missing = i == 6;
            csv.append("0;0;PT15M;kWh;BN01;").append(t).append(missing ? ";;MISSING\n" : ";0,250000;OK\n");
        }
        // quarterly prices: every quarter except the missing one, in order
        var quarterly = parse(csv.toString(), true);
        assertEquals(15, quarterly.data().size());
        assertFalse(quarterly.data().containsKey(Instant.parse("2025-10-01T01:45:00Z")));
        assertEquals(Instant.parse("2025-10-01T04:00:00Z"), quarterly.end());
        // hourly prices: 00:00 and 01:00 lack a quarter, 02:00 and 03:00 are complete, 04:00 has one quarter only
        var hourly = parse(csv.toString(), false);
        assertEquals(2, hourly.data().size());
        assertEquals(1.0, hourly.data().get(Instant.parse("2025-10-01T02:00:00Z")), 1e-12);
        assertEquals(1.0, hourly.data().get(Instant.parse("2025-10-01T03:00:00Z")), 1e-12);
    }

    private static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.FingridUsageData parse(String csv, boolean quarterly) throws Exception {
        return getFingridUsageData(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), quarterly);
    }
}
