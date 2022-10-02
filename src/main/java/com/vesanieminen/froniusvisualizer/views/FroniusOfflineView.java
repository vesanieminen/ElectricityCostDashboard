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

@Route(value = "fronius-offline")
public class FroniusOfflineView extends Div {

    public FroniusOfflineView() throws IOException {
        final var gson = Converters.registerAll(new GsonBuilder()).create();
        final var file = Files.readString(Path.of("src/main/resources/data/fronius/GetArchiveData 6-16.4.2022.js"));
        final var froniusResponse = gson.fromJson(file, FroniusResponse.class);
        add(new Pre(froniusResponse.Head.Timestamp.toString()));
        add(new Pre(froniusResponse.Body.Data.values().stream().toList().get(0).Data.EnergyReal_WAC_Sum_Produced.Values.values().size() + ""));
    }

}
