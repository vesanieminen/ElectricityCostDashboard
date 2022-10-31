package com.vesanieminen.froniusvisualizer.services;

import com.google.gson.Gson;
import com.vesanieminen.froniusvisualizer.services.model.APIVersion;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class FroniusService {

    private final String IP_ADDRESS = "http://192.168.1.79";
    private final String API_BASE = "/solar_api/";
    private final String API_BASE_URL = "/solar_api/v1/";
    private final String GET_API_VERSION = API_BASE + "GetAPIVersion.cgi";

    private final String GET_ARCHIVE_DATA = API_BASE_URL + "GetArchiveData.cgi?Scope=System&StartDate=24.9.2022&EndDate=24.9.2022&Channel=TimeSpanInSec&Channel=EnergyReal_WAC_Sum_Produced";
    private final String GET_POWER_FLOW_REALTIME_DATA = API_BASE_URL + "GetPowerFlowRealtimeData.fcgi";

    public APIVersion getAPIVersion() {
        try {
            final var request = HttpRequest.newBuilder().uri(new URI(IP_ADDRESS + GET_API_VERSION)).GET().build();
            final var response = HttpClient.newBuilder().build().send(request, HttpResponse.BodyHandlers.ofString());
            return new Gson().fromJson(response.body(), APIVersion.class);
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void getHistory(Consumer<HttpResponse<String>> consumer) {
        final HttpRequest request;
        try {
            request = HttpRequest.newBuilder().uri(new URI(IP_ADDRESS + GET_ARCHIVE_DATA)).GET().build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        CompletableFuture.supplyAsync(() -> {
            try {
                return HttpClient.newBuilder().build().send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).thenAccept(consumer);
    }

    public String getPowerFlowRealtimeData() {
        try {
            final var request = HttpRequest.newBuilder().uri(new URI(IP_ADDRESS + GET_POWER_FLOW_REALTIME_DATA)).GET().build();
            final var response = HttpClient.newBuilder().build().send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
