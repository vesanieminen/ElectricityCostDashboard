package com.vesanieminen.froniusvisualizer.services;

import com.fatboyindustrial.gsonjavatime.Converters;
import com.google.gson.GsonBuilder;
import com.vesanieminen.froniusvisualizer.services.model.NordpoolResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class NordpoolSpotService {

    public static final ZoneId fiZoneID = ZoneId.of("Europe/Helsinki");
    private static NordpoolResponse nordpoolResponse;
    private static LocalDateTime nextUpdate = LocalDateTime.now(fiZoneID).minusHours(1);
    private static final String url = "https://www.nordpoolspot.com/api/marketdata/page/35?currency=,,EUR,EUR";

    public static void updateNordpoolData() {
        //if (nextUpdate.isBefore(LocalDateTime.now(fiZoneID))) {
        final var nowWithoutMinutes = LocalDateTime.now(fiZoneID).withMinute(0);
        nextUpdate = nowWithoutMinutes.plusHours(1);
        final HttpRequest request;
        final HttpResponse<String> response;
        try {
            request = HttpRequest.newBuilder().uri(new URI(url)).GET().build();
            response = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        final var gson = Converters.registerAll(new GsonBuilder()).create();
        nordpoolResponse = gson.fromJson(response.body(), NordpoolResponse.class);
        //}
    }

    public static NordpoolResponse getLatest7Days() throws URISyntaxException, IOException, InterruptedException {
        return nordpoolResponse;
    }

}
