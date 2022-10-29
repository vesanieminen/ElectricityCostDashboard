package com.vesanieminen.froniusvisualizer.services;

import com.fatboyindustrial.gsonjavatime.Converters;
import com.google.gson.GsonBuilder;
import com.vesanieminen.froniusvisualizer.services.model.FingridLiteResponse;
import com.vesanieminen.froniusvisualizer.services.model.FingridRealtimeResponse;
import com.vesanieminen.froniusvisualizer.util.Utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static com.vesanieminen.froniusvisualizer.util.Properties.getFingridAPIKey;
import static com.vesanieminen.froniusvisualizer.util.Utils.getCurrentTimeWithHourPrecision;
import static java.util.stream.Collectors.joining;

public class FingridService {

    public enum QueryType {
        // 3min resolution (aka "realtime")
        HYDRO(191),
        NUCLEAR(188),
        DISTRICT(201),
        INDUSTRY(202),
        NET_IMPORT_EXPORT(194),
        SOLAR(248),
        // hourly resolution
        CONSUMPTION(124),
        PRODUCTION(74),
        WIND_PREDICTION(245),
        WIND(75);
        public int id;

        /*
                3min - realtime variable ids
                    - hydro: 191
                    - nuclear: 188
                    - district: 201
                    - industry: 202
                    - wind 181
                    - solar: 248
                    - consumption: 193
                    - net-import-export: 194
                hourly variable ids
                    - production: 74
                    - consumption: 124
                    - wind: 75
                    - wind-prediction: 245
        */
        QueryType(int id) {
            this.id = id;
        }
    }

    private static final ZoneId fiZoneID = ZoneId.of("Europe/Helsinki");
    private static FingridRealtimeResponse finGridRealtimeResponse;
    private static List<FingridLiteResponse> windEstimateResponses;

    // The final target for the basic fingrid query is:
    // https://www.fingrid.fi/api/graph/power-system-production?start=2022-10-04&end=2022-10-10
    private static final String fingridRealtimeBaseUrl = "https://www.fingrid.fi/api/graph/power-system-production?";

    // The final target for the wind estimate query is the following:
    // https://api.fingrid.fi/v1/variable/245/events/json?start_time=2022-10-09T00%3A00%3A00%2B0300&end_time=2022-10-12T00%3A00%3A00%2B0300
    private static final String fingridHourlyBaseUrl = "https://api.fingrid.fi/v1/variable/";
    private static final String fingridHourlyUrlPostfix = "/events/json?";

    public static void updateFingridRealtimeData() {
        final HttpRequest request;
        final HttpResponse<String> response;
        try {
            request = HttpRequest.newBuilder().uri(new URI(createFingridRealtimeQuery())).GET().build();
            response = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        final var gson = Converters.registerAll(new GsonBuilder()).create();
        final var newFingridRealtimeResponse = gson.fromJson(response.body(), FingridRealtimeResponse.class);
        // If data is missing do not overwrite the previous values
        if (!newFingridRealtimeResponse.isValid()) {
            return;
        }
        finGridRealtimeResponse = newFingridRealtimeResponse;
        finGridRealtimeResponse.HydroPower = keepEveryNthItem(finGridRealtimeResponse.HydroPower, 20);
        finGridRealtimeResponse.NuclearPower = keepEveryNthItem(finGridRealtimeResponse.NuclearPower, 20);
        finGridRealtimeResponse.WindPower = keepEveryNthItem(finGridRealtimeResponse.WindPower, 20);
        finGridRealtimeResponse.SolarPower = keepEveryNthItem(finGridRealtimeResponse.SolarPower, 20);
        finGridRealtimeResponse.Consumption = keepEveryNthItem(finGridRealtimeResponse.Consumption, 20);
        finGridRealtimeResponse.NetImportExport = keepEveryNthItem(finGridRealtimeResponse.NetImportExport, 20);
    }

    private static String createFingridRealtimeQuery() {
        Map<String, String> requestParams = new HashMap<>();
        final var now = getCurrentTimeWithHourPrecision();
        // Nordpool gives data for the next day at 14:00. Before that we need to retrieve 6 days back and after 5 to match the amount of Fingrid and Nordpool history
        var daysBack = now.getHour() < 14 ? 6 : 5;
        requestParams.put("start", createFingridDateTimeString(now.minusDays(daysBack)));
        requestParams.put("end", createFingridDateTimeString(now.plusDays(1)));
        return requestParams.keySet().stream().map(key -> key + "=" + requestParams.get(key)).collect(joining("&", fingridRealtimeBaseUrl, ""));
    }

    public static List<FingridRealtimeResponse.Data> keepEveryNthItem(List<FingridRealtimeResponse.Data> input, int n) {
        return IntStream.range(0, input.size()).filter(item -> item % n == 0).mapToObj(input::get).toList();
    }

    public static FingridRealtimeResponse getLatest7Days() throws URISyntaxException, IOException, InterruptedException {
        return finGridRealtimeResponse;
    }

    public static void updateWindEstimateData() {
        final var start = getCurrentTimeWithHourPrecision();
        final var newWindEstimateResponses = runQuery(createHourlyQuery(QueryType.WIND_PREDICTION, start, start.plusDays(2)));
        if (newWindEstimateResponses.size() > 0) {
            windEstimateResponses = newWindEstimateResponses;
        }
    }

    public static List<FingridLiteResponse> runQuery(String query) {
        final var apiKey = getFingridAPIKey();
        final HttpRequest request;
        HttpResponse<String> response;
        try {
            request = HttpRequest.newBuilder().uri(new URI(query)).GET().header("x-api-key", apiKey).build();
            response = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        final var gson = Converters.registerAll(new GsonBuilder()).create();
        return Arrays.stream(gson.fromJson(response.body(), FingridLiteResponse[].class)).toList();
    }

    public static List<FingridLiteResponse> getWindEstimate() throws URISyntaxException, IOException, InterruptedException {
        return windEstimateResponses;
    }

    public static String createHourlyQuery(QueryType queryType, LocalDateTime start, LocalDateTime end) {
        var query = fingridHourlyBaseUrl + queryType.id + fingridHourlyUrlPostfix;
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put("start_time", createDateTimeString(start));
        requestParams.put("end_time", createDateTimeString(end));
        return requestParams.keySet().stream().map(key -> {
            try {
                return key + "=" + Utils.encodeUrl(requestParams.get(key));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }).collect(joining("&", query, ""));
    }

    private static String createFingridDateTimeString(LocalDateTime localDateTime) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd").format(localDateTime);
    }

    private static String createDateTimeString(LocalDateTime localDateTime) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd").format(localDateTime) + "T" + DateTimeFormatter.ofPattern("HH:mm:ss").format(localDateTime) + "+0300";
    }

}
