package com.vesanieminen.froniusvisualizer.services;

import com.fatboyindustrial.gsonjavatime.Converters;
import com.google.gson.GsonBuilder;
import com.vesanieminen.froniusvisualizer.services.model.PakastinResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.hasBeenUpdatedSuccessfullyToday;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.hasBeenUpdatedSuccessfullyYesterday;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.updateSpotData;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.updateSpotData_60min;
import static com.vesanieminen.froniusvisualizer.util.Utils.fiZoneID;
import static com.vesanieminen.froniusvisualizer.util.Utils.getCurrentInstantDayPrecisionFinnishZone;
import static com.vesanieminen.froniusvisualizer.util.Utils.getStartOfDay;
import static com.vesanieminen.froniusvisualizer.util.Utils.isAfter_13_45;
import static java.util.stream.Collectors.joining;

@Slf4j
public class PakastinSpotService {

    private static PakastinResponse pakastinResponse;
    private static final String url = "https://sahkotin.fi/prices?quarter&";
    public static final String pakastinFile = "pakastin.json";
    public static final String pakastin15MinFile = "pakastin-2-year.json";

    private static final String url_60min = "https://sahkotin.fi/prices?";
    public static final String pakastin60MinFile = "pakastin-60min.json";

    public static final String pakastinTempFile = "src/main/resources/data/pakastin/spot.json";
    public static int updated_15min = 0;
    public static int updated_60min = 0;

    // Format with timestamps
    // https://pakastin.fi/hinnat/prices?start=2022-10-01T00:00:00.000Z&end=2022-10-31T23:59:00.000Z

    public static void updateData() {
        PakastinResponse newPakastinResponse = runAndMapToResponse(url);
        if (newPakastinResponse.isValid()) {
            pakastinResponse = newPakastinResponse;
        }
    }

    public static PakastinResponse runAndMapToResponse(String query) {
        final HttpResponse<String> response = runQuery(query);
        return mapToResponse(response.body());
    }

    public static PakastinResponse mapToResponse(String body) {
        final var gson = Converters.registerAll(new GsonBuilder()).create();
        return gson.fromJson(body, PakastinResponse.class);
    }

    public static HttpResponse<String> runQuery() {
        return runQuery(url);
    }

    private static HttpResponse<String> runQuery(String query) {
        final HttpRequest request;
        final HttpResponse<String> response;
        try {
            request = HttpRequest.newBuilder().uri(new URI(query)).GET().build();
            response = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return response;
    }

    public static List<PakastinResponse.Price> getLatest() {
        return pakastinResponse.prices;
    }

    public static List<PakastinResponse.Price> getLatest7Days() {
        final var oneWeekBack = getCurrentInstantDayPrecisionFinnishZone().minus(7, ChronoUnit.DAYS);
        return pakastinResponse.prices.stream().filter(item -> oneWeekBack.isBefore(item.date)).collect(Collectors.toList());
    }

    public static List<PakastinResponse.Price> get(Instant start, Instant end) {
        final var query = createQuery(start, end, url);
        return runAndMapToResponse(query).prices;
    }

    public static String createQuery(Instant start, Instant end, String url) {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put("start", start.toString());
        requestParams.put("end", end.toString());
        return requestParams.keySet().stream().map(key -> key + "=" + requestParams.get(key)).collect(joining("&", url, ""));
    }

    public static void getAndWriteToFile() {
        final var stringHttpResponse = runQuery();
        try {
            Files.write(Paths.get(pakastinFile), stringHttpResponse.body().getBytes());
        } catch (IOException e) {
            log.error("Error writing to file", e);
        }
    }

    public static void getHistoricalSpotPrices_15MinPrecision() {
        if (hasBeenUpdatedSuccessfullyToday()) {
            log.info("skipped Pakastin update due to having been updated successfully today already");
            return;
        }
        if (!isAfter_13_45(ZonedDateTime.now(fiZoneID)) && hasBeenUpdatedSuccessfullyYesterday()) {
            log.info("skipped Pakastin update due to not having new data available yet");
            return;
        }

        final var stringHttpResponse = runQuery(createQuery(getStartOfDay(2020, 1, 1), Instant.now().plus(10, ChronoUnit.DAYS), url));
        try {
            log.info("Writing file: " + Paths.get(pakastin15MinFile).getFileName());
            Files.write(Paths.get(pakastin15MinFile), stringHttpResponse.body().getBytes());
        } catch (IOException e) {
            log.error("Error writing to file", e);
        }
        log.info("PakastinService has been updated " + ++updated_15min + " times.");
        updateSpotData();
    }

    public static void getHistoricalSpotPrices_HourlyPrecision() {
        if (hasBeenUpdatedSuccessfullyToday()) {
            log.info("skipped Pakastin60MinSpotService update due to having been updated successfully today already");
            return;
        }
        if (!isAfter_13_45(ZonedDateTime.now(fiZoneID)) && hasBeenUpdatedSuccessfullyYesterday()) {
            log.info("skipped Pakastin60MinSpotService update due to not having new data available yet");
            return;
        }

        final var stringHttpResponse = runQuery(createQuery(getStartOfDay(2020, 1, 1), Instant.now().plus(10, ChronoUnit.DAYS), url_60min));
        try {
            log.info("Pakastin60MinSpotService Writing file: " + Paths.get(pakastin60MinFile).getFileName());
            Files.write(Paths.get(pakastin60MinFile), stringHttpResponse.body().getBytes());
        } catch (IOException e) {
            log.error("Pakastin60MinSpotService Error writing to file", e);
        }
        log.info("Pakastin60MinSpotService has been updated " + ++updated_60min + " times.");
        updateSpotData_60min();
    }


}
