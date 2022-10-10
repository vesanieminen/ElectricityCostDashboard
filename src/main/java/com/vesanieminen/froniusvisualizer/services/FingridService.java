package com.vesanieminen.froniusvisualizer.services;

import com.fatboyindustrial.gsonjavatime.Converters;
import com.google.gson.GsonBuilder;
import com.vesanieminen.froniusvisualizer.services.model.FingridResponse;
import com.vesanieminen.froniusvisualizer.services.model.FingridWindEstimateResponse;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;

public class FingridService {

    private static final ZoneId fiZoneID = ZoneId.of("Europe/Helsinki");
    private static FingridResponse finGridResponse;
    private static List<FingridWindEstimateResponse> windEstimateResponses;
    private static LocalDateTime nextUpdate = LocalDateTime.now(fiZoneID).minusHours(1);
    private static LocalDateTime nextWindEstimateUpdate = LocalDateTime.now(fiZoneID).minusHours(1);

    // The final target for the basic fingrid query is:
    // https://www.fingrid.fi/api/graph/power-system-production?start=2022-10-04&end=2022-10-10
    private static final String fingridBaseUrl = "https://www.fingrid.fi/api/graph/power-system-production?";

    // The final target for the wind estimate query is the following:
    // https://api.fingrid.fi/v1/variable/245/events/json?start_time=2022-10-09T00%3A00%3A00%2B0300&end_time=2022-10-12T00%3A00%3A00%2B0300
    private static final String windEstimateBaseUrl = "https://api.fingrid.fi/v1/variable/245/events/json?";


    public static FingridResponse getLatest7Days() throws URISyntaxException, IOException, InterruptedException {
        if (nextUpdate.isBefore(LocalDateTime.now(fiZoneID))) {
            final LocalDateTime nowWithoutMinutes = currentTimeWithoutMinutes();
            nextUpdate = nowWithoutMinutes.plusHours(1).plusSeconds(10);
            final var request = HttpRequest.newBuilder().uri(new URI(createFingridDataQuery())).GET().build();
            final var response = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build().send(request, HttpResponse.BodyHandlers.ofString());
            final var gson = Converters.registerAll(new GsonBuilder()).create();
            finGridResponse = gson.fromJson(response.body(), FingridResponse.class);
            finGridResponse.HydroPower = keepEveryNthItem(finGridResponse.HydroPower, 20);
            finGridResponse.NuclearPower = keepEveryNthItem(finGridResponse.NuclearPower, 20);
            finGridResponse.WindPower = keepEveryNthItem(finGridResponse.WindPower, 20);
            finGridResponse.SolarPower = keepEveryNthItem(finGridResponse.SolarPower, 20);
            finGridResponse.Consumption = keepEveryNthItem(finGridResponse.Consumption, 20);
            finGridResponse.NetImportExport = keepEveryNthItem(finGridResponse.NetImportExport, 20);
        }
        return finGridResponse;
    }

    public static List<FingridResponse.Data> keepEveryNthItem(List<FingridResponse.Data> input, int n) {
        return IntStream.range(0, input.size()).filter(item -> item % n == 0).mapToObj(input::get).toList();
    }

    public static List<FingridWindEstimateResponse> getWindEstimate(Environment env) throws URISyntaxException, IOException, InterruptedException {
        if (nextWindEstimateUpdate.isBefore(LocalDateTime.now(fiZoneID))) {
            final LocalDateTime nowWithoutMinutes = currentTimeWithoutMinutes();
            nextWindEstimateUpdate = nowWithoutMinutes.plusHours(1).plusSeconds(20);
            final var apiKey = env.getProperty("fingrid.api.key");
            final var request = HttpRequest.newBuilder().uri(new URI(createWindEstimateQuery())).GET().header("x-api-key", apiKey).build();
            var response = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build().send(request, HttpResponse.BodyHandlers.ofString());
            final var gson = Converters.registerAll(new GsonBuilder()).create();
            windEstimateResponses = Arrays.stream(gson.fromJson(response.body(), FingridWindEstimateResponse[].class)).toList();
        }
        return windEstimateResponses;
    }

    private static String createFingridDataQuery() {
        Map<String, String> requestParams = new HashMap<>();
        final var now = currentTimeWithoutMinutesAndSeconds();
        // Nordpool gives data for the next day at 14:00. Before that we need to retrieve 6 days back and after 5 to match the amount of Fingrid and Nordpool history
        var daysBack = now.getHour() < 14 ? 6 : 5;
        requestParams.put("start", createFingridDateTimeString(now.minusDays(daysBack)));
        requestParams.put("end", createFingridDateTimeString(now.plusDays(1)));
        return requestParams.keySet().stream().map(key -> key + "=" + requestParams.get(key)).collect(joining("&", fingridBaseUrl, ""));
    }

    private static String createWindEstimateQuery() {
        Map<String, String> requestParams = new HashMap<>();
        final var now = currentTimeWithoutMinutesAndSeconds();
        requestParams.put("start_time", createWindEstimateDateTimeString(now));
        requestParams.put("end_time", createWindEstimateDateTimeString(now.plusDays(2)));

        return requestParams.keySet().stream().map(key -> {
            try {
                return key + "=" + encodeUrl(requestParams.get(key));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }).collect(joining("&", windEstimateBaseUrl, ""));
    }

    private static String encodeUrl(String url) throws UnsupportedEncodingException {
        return URLEncoder.encode(url, StandardCharsets.UTF_8);
    }

    private static LocalDateTime currentTimeWithoutMinutes() {
        return LocalDateTime.now(fiZoneID).withMinute(0);
    }

    private static LocalDateTime currentTimeWithoutMinutesAndSeconds() {
        return LocalDateTime.now(fiZoneID).withMinute(0).withSecond(0);
    }

    private static String createFingridDateTimeString(LocalDateTime localDateTime) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd").format(localDateTime);
    }

    private static String createWindEstimateDateTimeString(LocalDateTime localDateTime) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd").format(localDateTime) + "T" + DateTimeFormatter.ofPattern("HH:mm:ss").format(localDateTime) + "+0300";
    }

}
