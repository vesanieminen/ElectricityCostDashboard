package com.vesanieminen.froniusvisualizer.views;

import com.fatboyindustrial.gsonjavatime.Converters;
import com.google.gson.GsonBuilder;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.router.Route;
import com.vesanieminen.froniusvisualizer.services.model.FroniusResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Route(value = "")
public class FroniusOfflineView extends Div {

    public FroniusOfflineView() throws IOException {
        final var path = Path.of("").toAbsolutePath();
        final var file = Files.readString(Path.of("src/main/resources/data/fronius/GetArchiveData 6-16.4.2022.js"));
        final var gson = Converters.registerAll(new GsonBuilder()).create();
        final var froniusResponse = gson.fromJson(file, FroniusResponse.class);
        System.out.println(froniusResponse.head);
        add(new Pre(file));
    }

}
