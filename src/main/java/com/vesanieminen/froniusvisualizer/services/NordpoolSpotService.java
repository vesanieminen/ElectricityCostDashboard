package com.vesanieminen.froniusvisualizer.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vesanieminen.froniusvisualizer.services.model.NordpoolPrice;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.vesanieminen.froniusvisualizer.util.Utils.fiZoneID;
import static com.vesanieminen.froniusvisualizer.util.Utils.getCurrentInstantDayPrecisionFinnishZone;
import static com.vesanieminen.froniusvisualizer.util.Utils.isAfter_13_45;
import static com.vesanieminen.froniusvisualizer.util.Utils.nordpoolZoneID;

@Slf4j
public class NordpoolSpotService {

    private static final String API_URL = "https://dataportal-api.nordpoolgroup.com/api/DayAheadPriceIndices";
    private static final String CURRENCY = "EUR";
    private static final String MARKET = "DayAhead";
    private static final String INDEX_NAMES = "FI";
    private static final String RESOLUTION_IN_MINUTES = "60";

    @Getter
    private static NordpoolResponse nordpoolResponse;
    private static final List<NordpoolPrice> nordpoolPrices = new ArrayList<>();
    private static final LinkedHashMap<Instant, Double> nordpoolPriceMap = new LinkedHashMap<>();
    public static int updated = 0;

    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final Gson gson = new GsonBuilder().create();

    /**
     * Updates the Nordpool data by fetching it from the API.
     *
     * @param date        The date for which to fetch data.
     * @param forceUpdate If true, forces an update regardless of last update time.
     */
    public static void updateData(LocalDate date, boolean forceUpdate) {
        if (hasBeenUpdatedSuccessfullyToday() && !forceUpdate) {
            //log.info("Skipped Nordpool update because it has already been updated successfully today.");
            return;
        }

        if (!isAfter_13_45(ZonedDateTime.now(fiZoneID)) /*&& hasBeenUpdatedSuccessfullyYesterday()*/ && !forceUpdate) {
            //log.info("skipped Nordpool update due to not having new data available yet");
            return;
        }


        URI uri = buildUri(date);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Origin", "https://data.nordpoolgroup.com")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                NordpoolResponse newNordpoolResponse = gson.fromJson(response.body(), NordpoolResponse.class);
                if (newNordpoolResponse != null) {
                    nordpoolResponse = newNordpoolResponse;
                    processResponse(nordpoolResponse);
                    log.info("NordpoolService has been updated " + ++updated + " times.");
                } else {
                    log.error("Invalid response from Nordpool API: " + response.body());
                }
            } else {
                log.error("Failed to fetch data. HTTP status code: " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            log.error("Exception while fetching data from Nordpool API", e);
        }
    }

    private static URI buildUri(LocalDate date) {
        String uriStr = API_URL + "?" +
                "currency=" + URLEncoder.encode(CURRENCY, StandardCharsets.UTF_8) +
                "&market=" + URLEncoder.encode(MARKET, StandardCharsets.UTF_8) +
                "&indexNames=" + URLEncoder.encode(INDEX_NAMES, StandardCharsets.UTF_8) +
                "&date=" + URLEncoder.encode(date.toString(), StandardCharsets.UTF_8) +
                "&resolutionInMinutes=" + URLEncoder.encode(RESOLUTION_IN_MINUTES, StandardCharsets.UTF_8);

        return URI.create(uriStr);
    }

    private static void processResponse(NordpoolResponse response) {
        for (MultiIndexEntry entry : response.multiIndexEntries) {
            Instant deliveryStart = Instant.parse(entry.deliveryStart);
            String priceStr = entry.entryPerArea.get(INDEX_NAMES);

            if (priceStr != null) {
                try {
                    double price = Double.parseDouble(priceStr.replace(",", ".").replace(" ", "")) / 10;
                    NordpoolPrice nordpoolPrice = new NordpoolPrice(price, deliveryStart.toEpochMilli());
                    if (!nordpoolPriceMap.containsKey(deliveryStart)) {
                        nordpoolPrices.add(nordpoolPrice);
                        nordpoolPriceMap.put(deliveryStart, price);
                    }
                } catch (NumberFormatException e) {
                    log.warn("Invalid price format: {}", priceStr);
                }
            }
        }
    }

    public static List<NordpoolPrice> getPriceList() {
        return nordpoolPrices;
    }

    public static LinkedHashMap<Instant, Double> getPriceMap() {
        return nordpoolPriceMap;
    }

    public static List<NordpoolPrice> getLatest7DaysList() {
        final var oneWeekBack = getCurrentInstantDayPrecisionFinnishZone().minus(7, ChronoUnit.DAYS);
        return nordpoolPrices.stream().filter(item -> oneWeekBack.isBefore(item.timeInstant())).collect(Collectors.toList());
    }

    public static LinkedHashMap<Instant, Double> getLatest7DaysMap() {
        return nordpoolPriceMap;
    }

    public static LocalDateTime getDateOfLatestFullDayData() {
        final var list = nordpoolPriceMap.keySet().stream().toList();
        return list.getLast().atZone(fiZoneID).toLocalDateTime().minusDays(1);
    }

    public static boolean hasBeenUpdatedSuccessfullyToday() {
        if (nordpoolResponse == null) {
            return false;
        }
        Instant dateUpdated = Instant.parse(nordpoolResponse.updatedAt);
        final var zonedDateTime = dateUpdated.atZone(nordpoolZoneID).truncatedTo(ChronoUnit.DAYS);
        final var other = ZonedDateTime.now(nordpoolZoneID).truncatedTo(ChronoUnit.DAYS);
        return zonedDateTime.getDayOfMonth() == other.getDayOfMonth();
    }

    public static boolean hasBeenUpdatedSuccessfullyYesterday() {
        if (nordpoolResponse == null) {
            return false;
        }
        // TODO: implement this
        return false;
    }

    public static LocalDateTime getUpdatedAt() {
        return Instant.parse(nordpoolResponse.updatedAt).atZone(fiZoneID).toLocalDateTime();
    }

    public record NordpoolResponse(String currency, List<MultiIndexEntry> multiIndexEntries, String updatedAt) {
    }

    public record MultiIndexEntry(String deliveryStart, String deliveryEnd, Map<String, String> entryPerArea) {
    }


    // Example usage
    //public static void main(String[] args) {
    //    NordpoolSpotService service = new NordpoolSpotService();
    //    service.updateData(LocalDate.now(), false);

    //    // Get the latest price list
    //    List<NordpoolPrice> prices = service.getPriceList();
    //    if (prices != null) {
    //        for (NordpoolPrice price : prices) {
    //            ZonedDateTime dateTime = Instant.ofEpochMilli(price.timestamp()).atZone(ZoneId.of("Europe/Helsinki"));
    //            System.out.printf("Time: %s, Price: %.2f EUR\n", dateTime, price.price());
    //        }
    //    }
    //}

    //public static final String spotPriceDataFile = "src/main/resources/data/sahko.tk/chart.csv";
    //public static final DateTimeFormatter datetimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    //private static List<NordpoolPrice> oldPriceList;
    //private static LinkedHashMap<Instant, Double> oldPriceMap;

    //public static LinkedHashMap<Instant, Double> readOldSpotData() throws IOException {
    //    if (oldPriceMap == null) {
    //        oldPriceList = new ArrayList<>();
    //        oldPriceMap = new LinkedHashMap<>();
    //        final var reader = Files.newBufferedReader(Path.of(spotPriceDataFile));
    //        final var csvReader = new CSVReader(reader);
    //        csvReader.skip(1);
    //        String[] line = new String[0];
    //        while (true) {
    //            try {
    //                if ((line = csvReader.readNext()) == null) break;
    //            } catch (CsvValidationException e) {
    //                log.error("CSV validation error: {}", e.getMessage());
    //            }
    //            final var dateTime = LocalDateTime.parse(line[0], datetimeFormatter).atZone(nordpoolZoneID).toInstant();
    //            final var price = Double.parseDouble(line[1]);
    //            oldPriceMap.put(dateTime, price);
    //            oldPriceList.add(new NordpoolPrice(price, dateTime.toEpochMilli()));
    //        }
    //    }
    //    return oldPriceMap;
    //}

}