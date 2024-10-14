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
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.vesanieminen.froniusvisualizer.util.Utils.fiZoneID;

@Slf4j
public class NordpoolSpotService {

    private static final String API_URL = "https://dataportal-api.nordpoolgroup.com/api/DayAheadPrices";
    private static final String CURRENCY = "EUR";
    private static final String MARKET = "DayAhead";
    private static final String DELIVERY_AREA = "FI";

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
            log.info("Skipped Nordpool update because it has already been updated successfully today.");
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
                "&deliveryArea=" + URLEncoder.encode(DELIVERY_AREA, StandardCharsets.UTF_8) +
                "&date=" + URLEncoder.encode(date.toString(), StandardCharsets.UTF_8);

        return URI.create(uriStr);
    }

    private static void processResponse(NordpoolResponse response) {
        for (MultiAreaEntry entry : response.multiAreaEntries) {
            Instant deliveryStart = Instant.parse(entry.deliveryStart);
            String priceStr = entry.entryPerArea.get(DELIVERY_AREA);

            if (priceStr != null) {
                try {
                    double price = Double.parseDouble(priceStr.replace(",", ".").replace(" ", "")) / 10;
                    NordpoolPrice nordpoolPrice = new NordpoolPrice(price, deliveryStart.toEpochMilli());
                    nordpoolPrices.add(nordpoolPrice);
                    nordpoolPriceMap.put(deliveryStart, price);
                } catch (NumberFormatException e) {
                    log.warn("Invalid price format: {}", priceStr);
                }
            }
        }
    }

    public List<NordpoolPrice> getPriceList() {
        return nordpoolPrices;
    }

    public LinkedHashMap<Instant, Double> getPriceMap() {
        return nordpoolPriceMap;
    }

    public static List<NordpoolPrice> getLatest7DaysList() {
        return nordpoolPrices;
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
        LocalDate date = dateUpdated.atZone(ZoneId.systemDefault()).toLocalDate();
        return date.equals(LocalDate.now());
    }

    public static LocalDateTime getUpdatedAt() {
        return Instant.parse(nordpoolResponse.updatedAt).atZone(fiZoneID).toLocalDateTime();
    }

    public record NordpoolResponse(int status, String currency, List<MultiAreaEntry> multiAreaEntries,
                                   String updatedAt) {
    }

    public record MultiAreaEntry(String deliveryStart, String deliveryEnd, Map<String, String> entryPerArea) {
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