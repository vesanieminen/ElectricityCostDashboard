package com.vesanieminen.froniusvisualizer.services;

import com.google.gson.Gson;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class FroniusService {

    private final String IP_ADDRESS = "http://192.168.1.128";
    private final String API_BASE = "/solar_api/";
    private final String API_BASE_URL = "/solar_api/v1/";
    private final String GET_API_VERSION = API_BASE + "GetAPIVersion.cgi";

    private final String GET_ARCHIVE_DATA = API_BASE_URL + "GetArchiveData.cgi?Scope=System&StartDate=14.9.2022&EndDate=24.9.2022&Channel=EnergyReal_WAC_Sum_Produced&Channel=EnergyReal_WAC_Minus_Absolute";

    public APIVersion getAPIVersion() {
        try {
            final var request = HttpRequest.newBuilder().uri(new URI(IP_ADDRESS + GET_API_VERSION)).GET().build();
            final var response = HttpClient.newBuilder().build().send(request, HttpResponse.BodyHandlers.ofString());
            return new Gson().fromJson(response.body(), APIVersion.class);
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public HttpResponse<String> getHistory() {
        try {
            final var request = HttpRequest.newBuilder().uri(new URI(IP_ADDRESS + GET_ARCHIVE_DATA)).GET().build();
            final var response = HttpClient.newBuilder().build().send(request, HttpResponse.BodyHandlers.ofString());
            //final var strings = new Gson().fromJson(response.body(), String.class);
            return response;
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
