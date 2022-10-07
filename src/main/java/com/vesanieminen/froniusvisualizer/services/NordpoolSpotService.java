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

    private static final ZoneId fiZoneID = ZoneId.of("Europe/Helsinki");
    private static NordpoolResponse nordpoolResponse;
    private static LocalDateTime nextUpdate = LocalDateTime.now(fiZoneID);
    private static final String url = "https://www.nordpoolspot.com/api/marketdata/page/35?currency=,,EUR,EUR";

    public static NordpoolResponse getLatest7Days() throws URISyntaxException, IOException, InterruptedException {
        if (nextUpdate.isBefore(LocalDateTime.now(fiZoneID))) {
            final var nowWithoutMinutes = LocalDateTime.now(fiZoneID).withMinute(0);
            nextUpdate = nowWithoutMinutes.plusHours(1);
            final var request = HttpRequest.newBuilder().uri(new URI(url)).GET().build();
            final var response = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build().send(request, HttpResponse.BodyHandlers.ofString());
            final var gson = Converters.registerAll(new GsonBuilder()).create();
            nordpoolResponse = gson.fromJson(response.body(), NordpoolResponse.class);
        }
        return nordpoolResponse;
    }

}
