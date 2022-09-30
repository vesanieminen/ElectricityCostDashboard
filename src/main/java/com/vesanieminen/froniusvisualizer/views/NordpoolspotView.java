package com.vesanieminen.froniusvisualizer.views;

import com.fatboyindustrial.gsonjavatime.Converters;
import com.google.gson.GsonBuilder;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.router.Route;
import com.vesanieminen.froniusvisualizer.services.model.NordpoolResponse;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.stream.Stream;

@Route("nordpool")
public class NordpoolspotView extends Div {

    public NordpoolspotView() throws URISyntaxException, IOException, InterruptedException {
        final var request = HttpRequest.newBuilder().uri(new URI("https://www.nordpoolgroup.com/api/marketdata/page/10")).GET().build();
        final var response = HttpClient.newBuilder().build().send(request, HttpResponse.BodyHandlers.ofString());
        final var gson = Converters.registerAll(new GsonBuilder()).create();
        final var nordpoolResponse = gson.fromJson(response.body(), NordpoolResponse.class);
        Stream.of(nordpoolResponse.data.getClass().getDeclaredFields()).forEach(field -> getAdd(nordpoolResponse, field));
        //add(new Pre(response.body()));
    }

    private void getAdd(NordpoolResponse nordpoolResponse, Field field) {
        try {
            field.setAccessible(true);
            add(new Pre(field.getName() + ": " + field.get(nordpoolResponse.data)));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
