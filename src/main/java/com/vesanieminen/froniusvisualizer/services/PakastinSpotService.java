package com.vesanieminen.froniusvisualizer.services;

import com.fatboyindustrial.gsonjavatime.Converters;
import com.google.gson.GsonBuilder;
import com.vesanieminen.froniusvisualizer.services.model.PakastinResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.vesanieminen.froniusvisualizer.util.Utils.getCurrentInstantDayPrecisionFinnishZone;
import static java.util.stream.Collectors.joining;

public class PakastinSpotService {

    private static PakastinResponse pakastinResponse;
    private static final String url = "https://pakastin.fi/hinnat/prices?";

    // Format with timestamps
    // https://pakastin.fi/hinnat/prices?start=2022-10-01T00:00:00.000Z&end=2022-10-31T23:59:00.000Z

    public static void updateData() {
        PakastinResponse newPakastinResponse = runQuery(url);
        if (newPakastinResponse.isValid()) {
            pakastinResponse = newPakastinResponse;
        }
    }

    private static PakastinResponse runQuery(String query) {
        final HttpRequest request;
        final HttpResponse<String> response;
        try {
            request = HttpRequest.newBuilder().uri(new URI(query)).GET().build();
            response = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        final var gson = Converters.registerAll(new GsonBuilder()).create();
        return gson.fromJson(response.body(), PakastinResponse.class);
    }

    public static List<PakastinResponse.Price> getLatest() {
        return pakastinResponse.prices;
    }

    public static List<PakastinResponse.Price> getLatest7Days() {
        final var oneWeekBack = getCurrentInstantDayPrecisionFinnishZone().minus(7, ChronoUnit.DAYS);
        return pakastinResponse.prices.stream().filter(item -> oneWeekBack.isBefore(item.date)).collect(Collectors.toList());
    }

    public static List<PakastinResponse.Price> get(Instant start, Instant end) {
        final var query = createQuery(start, end);
        return runQuery(query).prices;
    }

    public static String createQuery(Instant start, Instant end) {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put("start", start.toString());
        requestParams.put("end", end.toString());
        return requestParams.keySet().stream().map(key -> key + "=" + requestParams.get(key)).collect(joining("&", url, ""));
    }


}
