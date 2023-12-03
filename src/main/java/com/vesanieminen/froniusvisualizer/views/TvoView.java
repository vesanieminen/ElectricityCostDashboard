package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.router.Route;
import com.vesanieminen.froniusvisualizer.services.TVOQueryService;

import java.io.IOException;
import java.net.URISyntaxException;

@Route("tvo")
public class TvoView extends Div {

    public TvoView(TVOQueryService TVOQueryService) throws URISyntaxException, IOException, InterruptedException {
        final var tvoPage = TVOQueryService.getTVOPage();
        add(new Pre(tvoPage.body()));
    }

}
