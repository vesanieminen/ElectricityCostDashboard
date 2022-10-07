package com.vesanieminen.froniusvisualizer.services;

import com.fatboyindustrial.gsonjavatime.Converters;
import com.google.gson.GsonBuilder;
import com.vesanieminen.froniusvisualizer.services.model.FingridResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class FingridService {

    private static final ZoneId fiZoneID = ZoneId.of("Europe/Helsinki");
    private static FingridResponse finGridResponse;
    private static LocalDateTime nextUpdate = LocalDateTime.now(fiZoneID);
    private static final String url = "https://www.fingrid.fi/api/graph/power-system-production?start=2022-10-02&end=2022-10-08";

    public static FingridResponse getLatest7Days() throws URISyntaxException, IOException, InterruptedException {
        if (nextUpdate.isBefore(LocalDateTime.now(fiZoneID))) {
            final var nowWithoutMinutes = LocalDateTime.now(fiZoneID).withMinute(0);
            nextUpdate = nowWithoutMinutes.plusHours(1);
            final var request = HttpRequest.newBuilder().uri(new URI(url)).GET().build();
            final var response = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build().send(request, HttpResponse.BodyHandlers.ofString());
            final var gson = Converters.registerAll(new GsonBuilder()).create();
            finGridResponse = gson.fromJson(response.body(), FingridResponse.class);
        }
        return finGridResponse;
    }

}
