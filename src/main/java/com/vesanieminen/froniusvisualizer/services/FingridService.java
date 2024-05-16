package com.vesanieminen.froniusvisualizer.services;

import com.fatboyindustrial.gsonjavatime.Converters;
import com.google.gson.GsonBuilder;
import com.opencsv.CSVWriter;
import com.vesanieminen.froniusvisualizer.services.model.FingridLiteResponse;
import com.vesanieminen.froniusvisualizer.services.model.FingridLiteResponseWrapper;
import com.vesanieminen.froniusvisualizer.services.model.FingridRealtimeResponse;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.vesanieminen.froniusvisualizer.util.Properties.getFingridAPIKey;
import static com.vesanieminen.froniusvisualizer.util.Utils.fiLocale;
import static com.vesanieminen.froniusvisualizer.util.Utils.fiZoneID;
import static com.vesanieminen.froniusvisualizer.util.Utils.getCurrentTimeWithHourPrecision;
import static com.vesanieminen.froniusvisualizer.util.Utils.getCurrentZonedDateTimeHourPrecision;
import static com.vesanieminen.froniusvisualizer.util.Utils.keepEveryFirstItem;
import static com.vesanieminen.froniusvisualizer.util.Utils.keepEveryFirstItemLite;
import static com.vesanieminen.froniusvisualizer.util.Utils.keepEveryNthItem;
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
        WIND(75),
        CONSUMPTION_ESTIMATE(166),
        PRODUCTION_ESTIMATE(241);
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
                    - consumption estimate: 166
                    - production estimate: 241

        */
        QueryType(int id) {
            this.id = id;
        }
    }

    private static FingridRealtimeResponse cachedFingridRealtimeResponse;
    public static LocalDateTime fingridDataUpdated;
    private static FingridRealtimeResponse cachedFingridRealtimeResponseForMonth;
    private static List<FingridLiteResponse> cachedWindEstimateResponses;
    private static List<FingridLiteResponse> cachedProductionEstimateResponses;
    private static List<FingridLiteResponse> cachedConsumptionEstimateResponses;

    // The final target for the basic fingrid query is:
    // https://www.fingrid.fi/api/graph/power-system-production?start=2022-10-04&end=2022-10-10
    private static final String fingridRealtimeBaseUrl = "https://www.fingrid.fi/api/graph/power-system-production?";

    // The final target for the wind estimate query is the following:
    // https://api.fingrid.fi/v1/variable/245/events/json?start_time=2022-10-09T00%3A00%3A00%2B0300&end_time=2022-10-12T00%3A00%3A00%2B0300
    // new API format:
    // https://data.fingrid.fi/api/datasets/245/data?startTime=2024-05-01T00:00:00&format=json&locale=en&sortBy=startTime&sortOrder=asc
    private static final String fingridHourlyBaseUrl = "https://data.fingrid.fi/api/datasets/";
    private static final String fingridHourlyUrlPostfix = "/data?format=json&locale=en&sortBy=startTime&sortOrder=asc&pageSize=1000&";

    public static void updateRealtimeData() {
        final var newFingridRealtimeResponse = runRealtimeDataQuery(createFingridRealtimeQuery());
        if (!newFingridRealtimeResponse.isValid()) {
            return;
        }
        cachedFingridRealtimeResponse = newFingridRealtimeResponse;
        cachedFingridRealtimeResponse.HydroPower = keepEveryFirstItem(cachedFingridRealtimeResponse.HydroPower);
        cachedFingridRealtimeResponse.NuclearPower = keepEveryFirstItem(cachedFingridRealtimeResponse.NuclearPower);
        cachedFingridRealtimeResponse.WindPower = keepEveryFirstItem(cachedFingridRealtimeResponse.WindPower);
        cachedFingridRealtimeResponse.SolarPower = keepEveryFirstItem(cachedFingridRealtimeResponse.SolarPower);
        cachedFingridRealtimeResponse.Consumption = keepEveryFirstItem(cachedFingridRealtimeResponse.Consumption);
        cachedFingridRealtimeResponse.NetImportExport = keepEveryFirstItem(cachedFingridRealtimeResponse.NetImportExport);
    }

    public static FingridRealtimeResponse runRealtimeDataQuery(String query) {
        final HttpRequest request;
        final HttpResponse<String> response;
        try {
            request = HttpRequest.newBuilder().uri(new URI(query)).GET().build();
            response = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        final var gson = Converters.registerAll(new GsonBuilder()).create();
        return gson.fromJson(response.body(), FingridRealtimeResponse.class);
    }

    public static String createFingridRealtimeQuery() {
        Map<String, String> requestParams = new HashMap<>();
        final var now = getCurrentTimeWithHourPrecision();
        fingridDataUpdated = now;
        // Nordpool gives data for the next day at 14:00. Before that we need to retrieve 6 days back and after 5 to match the amount of Fingrid and Nordpool history
        var daysBack = now.getHour() < 14 ? 6 : 5;
        requestParams.put("start", createFingridDateTimeString(now.minusDays(daysBack)));
        requestParams.put("end", createFingridDateTimeString(now.plusDays(1)));
        return requestParams.keySet().stream().map(key -> key + "=" + requestParams.get(key)).collect(joining("&", fingridRealtimeBaseUrl, ""));
    }

    public static FingridRealtimeResponse getRealtimeDataForMonth() {
        if (cachedFingridRealtimeResponseForMonth == null) {
            final var now = getCurrentTimeWithHourPrecision();
            cachedFingridRealtimeResponseForMonth = runRealtimeDataQuery(createFingridRealtimeQuery(now.minusMonths(4), now.truncatedTo(ChronoUnit.DAYS)));
        }
        return cachedFingridRealtimeResponseForMonth;
    }

    public static String createFingridRealtimeQuery(LocalDateTime start, LocalDateTime end) {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put("start", createFingridDateTimeString(start));
        requestParams.put("end", createFingridDateTimeString(end));
        return requestParams.keySet().stream().map(key -> key + "=" + requestParams.get(key)).collect(joining("&", fingridRealtimeBaseUrl, ""));
    }

    public static FingridRealtimeResponse getLatest7Days() throws URISyntaxException, IOException, InterruptedException {
        return cachedFingridRealtimeResponse;
    }

    public static void updateWindEstimateData() {
        final var start = getCurrentZonedDateTimeHourPrecision();
        final var newWindEstimateResponses = runQuery(createHourlyQuery(QueryType.WIND_PREDICTION, start));
        if (!newWindEstimateResponses.isEmpty()) {
            cachedWindEstimateResponses = keepEveryFirstItemLite(newWindEstimateResponses);
        }
    }

    public static void updateProductionEstimateData() {
        final var start = getCurrentZonedDateTimeHourPrecision();
        final var newProductionEstimateResponses = runQuery(createHourlyQuery(QueryType.PRODUCTION_ESTIMATE, start));
        if (!newProductionEstimateResponses.isEmpty()) {
            cachedProductionEstimateResponses = newProductionEstimateResponses;
        }
    }

    public static void updateConsumptionEstimateData() {
        final var start = getCurrentZonedDateTimeHourPrecision();
        final var newConsumptionEstimateResponses = runQuery(createHourlyQuery(QueryType.CONSUMPTION_ESTIMATE, start));
        if (!newConsumptionEstimateResponses.isEmpty()) {
            cachedConsumptionEstimateResponses = keepEveryNthItem(newConsumptionEstimateResponses, 12);
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
        return gson.fromJson(response.body(), FingridLiteResponseWrapper.class).data;
    }

    public static List<FingridLiteResponse> getWindEstimate() {
        return cachedWindEstimateResponses;
    }

    public static List<FingridLiteResponse> getProductionEstimate() {
        return cachedProductionEstimateResponses;
    }

    public static List<FingridLiteResponse> getConsumptionEstimate() {
        return cachedConsumptionEstimateResponses;
    }

    public static String createHourlyQuery(QueryType queryType, ZonedDateTime start) {
        var query = fingridHourlyBaseUrl + queryType.id + fingridHourlyUrlPostfix;
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put("startTime", createDateTimeString(start));
        return requestParams.keySet().stream().map(key -> {
            //return key + "=" + Utils.encodeUrl(requestParams.get(key));
            return key + "=" + requestParams.get(key);
        }).collect(joining("&", query, ""));
    }

    private static String createFingridDateTimeString(LocalDateTime localDateTime) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd").format(localDateTime);
    }

    private static String createDateTimeString(ZonedDateTime zonedDateTime) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd").format(zonedDateTime) + "T" + DateTimeFormatter.ofPattern("HH:mm:ss").format(zonedDateTime);
    }

    public static void writeToCSVFile() {
        final var realtimeDataForMonth = getRealtimeDataForMonth();
        final var lowestDay = realtimeDataForMonth.WindPower.stream().min(Comparator.comparing(item -> item.startTime)).get().startTime.truncatedTo(ChronoUnit.DAYS);
        final var highestDay = realtimeDataForMonth.WindPower.stream().max(Comparator.comparing(item -> item.startTime)).get().startTime.truncatedTo(ChronoUnit.DAYS);
        final var prices = PakastinSpotService.getLatest().stream().filter(item -> !item.date.atZone(fiZoneID).truncatedTo(ChronoUnit.DAYS).isBefore(lowestDay)).toList();

        try {
            final var low = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(fiLocale).format(lowestDay);
            final var high = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(fiLocale).format(highestDay);
            Path path = Path.of("/Users/vesanieminen/Desktop/spot+fingrid export " + low + "-" + high + ".csv");
            CSVWriter writer = new CSVWriter(new FileWriter(path.toString()));
            writer.writeNext(new String[]{
                    "spot-datetime",
                    "fingrid-datetime",
                    "spothinta",
                    "windprod",
                    "hydroprod",
                    "nucprod",
                    "solar",
                    "consumption",
                    "netexport"
            });
            for (int i = 0; i < realtimeDataForMonth.WindPower.size(); ++i) {
                final var windprod = realtimeDataForMonth.WindPower.get(i);
                final var hydroprod = realtimeDataForMonth.HydroPower.get(i);
                final var nucprod = realtimeDataForMonth.NuclearPower.get(i);
                final var solar = realtimeDataForMonth.SolarPower.get(i);
                final var consumption = realtimeDataForMonth.Consumption.get(i);
                final var netexport = realtimeDataForMonth.NetImportExport.get(i);
                writer.writeNext(new String[]{
                        prices.get(i).date.toString(),
                        windprod.startTime.toInstant().toString(),
                        String.valueOf(prices.get(i).value),
                        String.valueOf(windprod.value),
                        String.valueOf(hydroprod.value),
                        String.valueOf(nucprod.value),
                        String.valueOf(solar.value),
                        String.valueOf(consumption.value),
                        String.valueOf(netexport.value),
                });
            }
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
