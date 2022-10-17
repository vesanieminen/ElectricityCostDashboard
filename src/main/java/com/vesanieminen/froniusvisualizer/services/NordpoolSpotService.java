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

public class NordpoolSpotService {

    private static NordpoolResponse nordpoolResponse;
    private static final String url = "https://www.nordpoolspot.com/api/marketdata/page/35?currency=,,EUR,EUR";

    public static void updateNordpoolData() {
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
        }
    }

    public static NordpoolResponse getLatest7Days() throws URISyntaxException, IOException, InterruptedException {
        return nordpoolResponse;
    }

}
