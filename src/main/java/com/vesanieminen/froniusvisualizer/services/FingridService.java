package com.vesanieminen.froniusvisualizer.services;

import com.fatboyindustrial.gsonjavatime.Converters;
import com.google.gson.GsonBuilder;
import com.opencsv.CSVWriter;
import com.vesanieminen.froniusvisualizer.services.model.FingridLiteResponse;
import com.vesanieminen.froniusvisualizer.services.model.FingridRealtimeResponse;
import com.vesanieminen.froniusvisualizer.util.Utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static com.vesanieminen.froniusvisualizer.util.Properties.getFingridAPIKey;
import static com.vesanieminen.froniusvisualizer.util.Utils.fiLocale;
import static com.vesanieminen.froniusvisualizer.util.Utils.fiZoneID;
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
    private static final String fingridHourlyBaseUrl = "https://api.fingrid.fi/v1/variable/";
    private static final String fingridHourlyUrlPostfix = "/events/json?";

    public static void updateRealtimeData() {
        final var newFingridRealtimeResponse = runRealtimeDataQuery(createFingridRealtimeQuery());
        if (!newFingridRealtimeResponse.isValid()) {
            return;
        }
        cachedFingridRealtimeResponse = newFingridRealtimeResponse;
        cachedFingridRealtimeResponse.HydroPower = keepEveryNthItem(cachedFingridRealtimeResponse.HydroPower, 20);
        cachedFingridRealtimeResponse.NuclearPower = keepEveryNthItem(cachedFingridRealtimeResponse.NuclearPower, 20);
        cachedFingridRealtimeResponse.WindPower = keepEveryNthItem(cachedFingridRealtimeResponse.WindPower, 20);
        cachedFingridRealtimeResponse.SolarPower = keepEveryNthItem(cachedFingridRealtimeResponse.SolarPower, 20);
        cachedFingridRealtimeResponse.Consumption = keepEveryNthItem(cachedFingridRealtimeResponse.Consumption, 20);
        cachedFingridRealtimeResponse.NetImportExport = keepEveryNthItem(cachedFingridRealtimeResponse.NetImportExport, 20);
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

    public static <T> List<T> keepEveryNthItem(List<T> input, int n) {
        return IntStream.range(0, input.size()).filter(item -> item % n == 0).mapToObj(input::get).toList();
    }

    public static FingridRealtimeResponse getLatest7Days() throws URISyntaxException, IOException, InterruptedException {
        return cachedFingridRealtimeResponse;
    }

    public static void updateWindEstimateData() {
        final var start = getCurrentTimeWithHourPrecision();
        final var newWindEstimateResponses = runQuery(createHourlyQuery(QueryType.WIND_PREDICTION, start, start.plusDays(2)));
        if (newWindEstimateResponses.size() > 0) {
            cachedWindEstimateResponses = newWindEstimateResponses;
        }
    }

    public static void updateProductionEstimateData() {
        final var start = getCurrentTimeWithHourPrecision();
        final var newProductionEstimateResponses = runQuery(createHourlyQuery(QueryType.PRODUCTION_ESTIMATE, start, start.plusDays(2)));
        if (newProductionEstimateResponses.size() > 0) {
            cachedProductionEstimateResponses = newProductionEstimateResponses;
        }
    }

    public static void updateConsumptionEstimateData() {
        final var start = getCurrentTimeWithHourPrecision();
        final var newConsumptionEstimateResponses = runQuery(createHourlyQuery(QueryType.CONSUMPTION_ESTIMATE, start, start.plusDays(2)));
        if (newConsumptionEstimateResponses.size() > 0) {
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
        return Arrays.stream(gson.fromJson(response.body(), FingridLiteResponse[].class)).toList();
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

    public static void writeToCSVFile() {
        final var realtimeDataForMonth = getRealtimeDataForMonth();
        final var lowestDay = realtimeDataForMonth.WindPower.stream().min(Comparator.comparing(item -> item.start_time)).get().start_time.truncatedTo(ChronoUnit.DAYS);
        final var highestDay = realtimeDataForMonth.WindPower.stream().max(Comparator.comparing(item -> item.start_time)).get().start_time.truncatedTo(ChronoUnit.DAYS);
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
                        windprod.start_time.toInstant().toString(),
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
