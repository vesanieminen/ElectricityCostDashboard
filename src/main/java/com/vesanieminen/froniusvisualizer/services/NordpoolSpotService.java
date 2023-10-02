package com.vesanieminen.froniusvisualizer.services;

import com.fatboyindustrial.gsonjavatime.Converters;
import com.google.gson.GsonBuilder;
import com.vesanieminen.froniusvisualizer.services.model.NordpoolPrice;
import com.vesanieminen.froniusvisualizer.services.model.NordpoolResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static com.vesanieminen.froniusvisualizer.util.Utils.dateTimeFormatter;
import static com.vesanieminen.froniusvisualizer.util.Utils.fiZoneID;
import static com.vesanieminen.froniusvisualizer.util.Utils.isAfter_13_45;
import static com.vesanieminen.froniusvisualizer.util.Utils.nordpoolZoneID;
import static com.vesanieminen.froniusvisualizer.util.Utils.numberFormat;

@Slf4j
public class NordpoolSpotService {

    private static NordpoolResponse nordpoolResponse;
    public static final String nordPoolSpotFile = "nordpool-spot-data.json";

    private static final String url = "https://www.nordpoolspot.com/api/marketdata/page/35?currency=,,EUR,EUR";
    private static List<NordpoolPrice> nordpoolPrices;
    private static LinkedHashMap<Instant, Double> nordpoolPriceMap;
    public static int updated = 0;

    public static void updateNordpoolData() {
        if (hasBeenUpdatedSuccessfullyToday()) {
            log.info("skipped Nordpool update due to having been updated already today successfully");
            return;
        }
        if (!isAfter_13_45(ZonedDateTime.now(fiZoneID)) && hasBeenUpdatedSuccessfullyYesterday()) {
            log.info("skipped Nordpool update due to not having new data available yet");
            return;
        }

        final HttpRequest request;
        final HttpResponse<String> response;
        try {
            request = HttpRequest.newBuilder().uri(new URI(url)).GET().build();
            response = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        final var gson = Converters.registerAll(new GsonBuilder()).create();
        var newNordpoolResponse = gson.fromJson(response.body(), NordpoolResponse.class);
        if (newNordpoolResponse.isValid()) {
            nordpoolResponse = newNordpoolResponse;
            nordpoolPrices = toPriceList(nordpoolResponse);
            nordpoolPriceMap = toPriceMap(nordpoolResponse);
        }
        log.info("NordpoolService has been updated " + ++updated + " times.");
    }

    public static NordpoolResponse getLatest7Days() {
        return nordpoolResponse;
    }

    public static void writeFile(HttpResponse<String> response) {
        try {
            log.info("Writing file: " + Paths.get(nordPoolSpotFile).getFileName());
            Files.write(Paths.get(nordPoolSpotFile), response.body().getBytes());
        } catch (IOException e) {
            log.error("Error writing to file", e);
        }
    }

    private static List<NordpoolPrice> toPriceList(NordpoolResponse nordpoolResponse) {
        final var nordpoolPrices = new ArrayList<NordpoolPrice>();
        final var rows = nordpoolResponse.data.Rows;
        int columnIndex = 6;
        while (columnIndex >= 0) {
            for (NordpoolResponse.Row row : rows.subList(0, rows.size() - 6)) {
                final var time = row.StartTime.toString().split("T")[1];
                NordpoolResponse.Column column = row.Columns.get(columnIndex);
                final var dateTimeString = column.Name + " " + time;
                final var dataLocalDataTime = LocalDateTime.parse(dateTimeString, dateTimeFormatter);
                final var instant = dataLocalDataTime.atZone(nordpoolZoneID).toInstant();
                try {
                    var price = numberFormat.parse(column.Value).doubleValue() / 10;
                    final var nordpoolPrice = new NordpoolPrice(price, instant.toEpochMilli());
                    nordpoolPrices.add(nordpoolPrice);
                } catch (ParseException e) {
                    continue;
                    //throw new RuntimeException(e);
                }
            }
            --columnIndex;
        }
        return nordpoolPrices;
    }

    public static List<NordpoolPrice> getLatest7DaysList() {
        return nordpoolPrices;
    }

    private static LinkedHashMap<Instant, Double> toPriceMap(NordpoolResponse nordpoolResponse) {
        final var nordpoolPrices = new LinkedHashMap<Instant, Double>();
        final var rows = nordpoolResponse.data.Rows;
        int columnIndex = 6;
        while (columnIndex >= 0) {
            for (NordpoolResponse.Row row : rows.subList(0, rows.size() - 6)) {
                final var time = row.StartTime.toString().split("T")[1];
                NordpoolResponse.Column column = row.Columns.get(columnIndex);
                final var dateTimeString = column.Name + " " + time;
                final var dataLocalDataTime = LocalDateTime.parse(dateTimeString, dateTimeFormatter);
                final var instant = dataLocalDataTime.atZone(nordpoolZoneID).toInstant();
                try {
                    var price = numberFormat.parse(column.Value).doubleValue() / 10;
                    nordpoolPrices.put(instant, price);
                } catch (ParseException e) {
                }
            }
            --columnIndex;
        }
        return nordpoolPrices;
    }

    public static LinkedHashMap<Instant, Double> getLatest7DaysMap() {
        return nordpoolPriceMap;
    }

    public static LocalDateTime getDateOfLatestFullDayData() {
        final var latest7Days = getLatest7Days();
        return latest7Days.data.DataEnddate.minusDays(1);
    }

    public static boolean hasBeenUpdatedSuccessfullyToday() {
        if (getLatest7Days() == null) {
            return false;
        }
        final var dateUpdated = getLatest7Days().data.DateUpdated;
        return dateUpdated.toLocalDate().getDayOfMonth() == LocalDate.now().getDayOfMonth();
    }

    public static boolean hasBeenUpdatedSuccessfullyYesterday() {
        if (getLatest7Days() == null) {
            return false;
        }
        final var dateUpdated = getLatest7Days().data.DateUpdated;
        return dateUpdated.toLocalDate().getDayOfMonth() == LocalDate.now().minusDays(1).getDayOfMonth();
    }

}
