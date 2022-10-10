package com.vesanieminen.froniusvisualizer.services;

import com.fatboyindustrial.gsonjavatime.Converters;
import com.google.gson.GsonBuilder;
import com.vesanieminen.froniusvisualizer.services.model.TVOResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TVOService {

    public static final ZoneId fiZoneID = ZoneId.of("Europe/Helsinki");
    private static TVOResponse tvoResponse;
    private static LocalDateTime nextUpdate = LocalDateTime.now(fiZoneID).minusHours(1);

    // target url:
    //https://www.tvo.fi/ol3-2022-10-10-17-00.json
    private static final String baseUrl = "https://www.tvo.fi/ol3-%s.json";

    public static TVOResponse getDayAheadPrediction() {
        //if (nextUpdate.isBefore(LocalDateTime.now(fiZoneID))) {
        final var nowWithoutMinutes = LocalDateTime.now(fiZoneID).withMinute(36);
        nextUpdate = nowWithoutMinutes.plusHours(1);
        final HttpRequest request;
        final HttpResponse<String> response;
        try {
            request = HttpRequest.newBuilder().uri(new URI(baseUrl.formatted(createTVODateTimeFormat(nowWithoutMinutes)))).GET().build();
            response = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        final var gson = Converters.registerAll(new GsonBuilder()).create();
        tvoResponse = gson.fromJson(response.body(), TVOResponse.class);
        //}
        return tvoResponse;
    }

    private static String createTVODateTimeFormat(LocalDateTime localDateTime) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm").format(localDateTime);
    }

}
