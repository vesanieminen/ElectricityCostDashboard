package com.vesanieminen.froniusvisualizer.services;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class SahkovatkainService {

    private static final String API_URL = "https://raw.githubusercontent.com/vividfog/nordpool-predict-fi/main/deploy/prediction.json";
    @Getter
    private static List<HourPrice> hourPrices;

    public static void updateData() {
        final var newPrices = fetchPrediction();
        if (!newPrices.isEmpty()) {
            hourPrices = newPrices;
        }
    }

    public static List<HourPrice> fetchPrediction() {
        final var httpClient = HttpClient.newHttpClient();
        final var gson = new Gson();
        final var request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .build();

        final HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage());
            return new ArrayList<>();
        }

        // Explicitly specify the Type for Gson
        final var listType = new TypeToken<List<double[]>>() {
        }.getType();
        List<double[]> dataArray = gson.fromJson(response.body(), listType);

        // Convert to List<HourPrice>
        List<HourPrice> hourPrices = new ArrayList<>();
        for (double[] dataPoint : dataArray) {
            final var timestamp = (long) dataPoint[0];
            final var price = dataPoint[1];
            final var hourPrice = new HourPrice(timestamp, price);
            hourPrices.add(hourPrice);
        }
        return hourPrices;
    }

    /**
     * @param timestamp Getters milliseconds since epoch
     */
    public record HourPrice(long timestamp, double price) {
    }

    public static List<HourPrice> getNewHourPrices() {
        return getHourPrices().stream().filter(item -> Instant.ofEpochMilli(item.timestamp()).isAfter(PriceCalculatorService.spotDataEnd)).toList();
    }

}