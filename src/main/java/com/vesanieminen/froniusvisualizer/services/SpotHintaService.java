package com.vesanieminen.froniusvisualizer.services;

import com.fatboyindustrial.gsonjavatime.Converters;
import com.google.gson.GsonBuilder;
import com.vesanieminen.froniusvisualizer.services.model.SpotHintaResponse;
import com.vesanieminen.froniusvisualizer.util.Utils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import static com.vesanieminen.froniusvisualizer.util.Utils.fiZoneID;

@Slf4j
public class SpotHintaService {

    private static List<SpotHintaResponse> spotHintaResponse;
    private static String query = "https://api.spot-hinta.fi/PostalCodeTemperatures/14700";

    public static void updateData() {
        var newSpotHintaResponse = runAndMapToResponse(query);
        var time = ZonedDateTime.of(Utils.getCurrentTimeWithHourPrecision(), fiZoneID).plusHours(36);
        if (newSpotHintaResponse.size() > 1) {
            var previous = newSpotHintaResponse.get(0).TimeStamp;
            for (int i = 1; i < newSpotHintaResponse.size(); ++i) {
                var current = newSpotHintaResponse.get(i).TimeStamp;
                // only use estimates of up to 36h from current Finnish time
                if (current.isAfter(time)) {
                    newSpotHintaResponse = newSpotHintaResponse.subList(0, i);
                    break;
                }
                //// Remove the temperature estimates that have a longer time span than 1h
                //if (ChronoUnit.HOURS.between(previous, current) > 1) {
                //    newSpotHintaResponse = newSpotHintaResponse.subList(0, i);
                //    break;
                //}
                previous = newSpotHintaResponse.get(i).TimeStamp;
            }
            spotHintaResponse = newSpotHintaResponse;
        }
    }

    public static List<SpotHintaResponse> runAndMapToResponse(String query) {
        var response = runQuery(query);
        return mapToResponse(response.body());
    }

    public static List<SpotHintaResponse> mapToResponse(String body) {
        final var gson = Converters.registerAll(new GsonBuilder()).create();
        return Arrays.stream(gson.fromJson(body, SpotHintaResponse[].class)).toList();
    }

    private static HttpResponse<String> runQuery(String query) {
        final HttpRequest request;
        final HttpResponse<String> response;
        try {
            request = HttpRequest.newBuilder().uri(new URI(query)).GET().build();
            response = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return response;
    }

    public static List<SpotHintaResponse> getLatest() {
        return spotHintaResponse;
    }

}
