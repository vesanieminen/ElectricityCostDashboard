package com.vesanieminen.froniusvisualizer.services;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class TVOQueryService {

    public HttpResponse<String> getTVOPage() throws URISyntaxException, IOException, InterruptedException {
        var url = "https://www.tvo.fi/tuotanto/laitosyksikot/ol3/ol3ennusteet.html";
        final var request = HttpRequest.newBuilder().uri(new URI(url)).GET().build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

}
