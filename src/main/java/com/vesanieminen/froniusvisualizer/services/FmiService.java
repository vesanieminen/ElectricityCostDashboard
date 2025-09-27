package com.vesanieminen.froniusvisualizer.services;

import com.fatboyindustrial.gsonjavatime.Converters;
import com.google.gson.GsonBuilder;
import com.vesanieminen.froniusvisualizer.services.model.FmiObservationResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;

import static com.vesanieminen.froniusvisualizer.util.Utils.fiZoneID;

public class FmiService {
    private static final String fmiApiBaseUrl = "https://www.ilmatieteenlaitos.fi/api/weather/observations?fmisid=%s&observations=true";
    private static final String observationPlaceFmisid = "101150"; // Hämeenlinna/Katinen, matches with the forecasts

    private static LocalDateTime nextUpdate = LocalDateTime.now(fiZoneID).minusHours(1);
    private static FmiObservationResponse lastResponse;
    private static final SimpleDateFormat dateparser = new SimpleDateFormat("yyyyMMdd'T'HHmmss");


    private static String buildFmiUrl() {
        return fmiApiBaseUrl.formatted(observationPlaceFmisid);
    }

    private static boolean isCacheStale() {
        return LocalDateTime.now(fiZoneID).compareTo(nextUpdate) > 0;
    }

    public static FmiObservationResponse fetchLatestObservations() {
        final var nowWithoutMinutes = LocalDateTime.now(fiZoneID);
        nextUpdate = nowWithoutMinutes.plusHours(1);
        final HttpRequest request;
        final HttpResponse<String> response;

        try {
            request = HttpRequest.newBuilder()
                    .uri(new URI(buildFmiUrl())).GET().build();
            response = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build().send(request,
                    HttpResponse.BodyHandlers.ofString());
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        final var gson = Converters.registerAll(new GsonBuilder()).create();
        lastResponse = gson.fromJson(response.body(), FmiObservationResponse.class);

        final var fmiObservationsFiltered = Arrays.stream(lastResponse.getObservations()).filter(item -> Objects.requireNonNull(FmiService.parseFmiTimestamp(item.getLocaltime(), item.getLocaltz())).getMinutes() == 0).toArray(FmiObservationResponse.FmiObservation[]::new);
        lastResponse.setObservations(fmiObservationsFiltered);

        return lastResponse;
    }

    public static FmiObservationResponse getObservations() {
        if (lastResponse == null || isCacheStale()) {
            return fetchLatestObservations();
        }
        return lastResponse;
    }

    public static Date parseFmiTimestamp(String fmiTimestamp, String fmiTimezone) {
        try {
            dateparser.setTimeZone(fmiTimezone == null ? TimeZone.getTimeZone(fiZoneID) : (TimeZone.getTimeZone(fmiTimezone)));
            return dateparser.parse(fmiTimestamp);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
}
