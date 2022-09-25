package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vesanieminen.froniusvisualizer.services.FroniusService;
import org.springframework.beans.factory.annotation.Autowired;

@PageTitle("")
@Route(value = "")
public class MainView extends VerticalLayout {

    private FroniusService froniusService;

    public MainView(@Autowired FroniusService froniusService) {
        this.froniusService = froniusService;

        final var apiVersion = froniusService.getAPIVersion();
        add(new Pre("""
                apiVersion: %s
                baseURL: %s
                compatibilityRange: %s
                """.formatted(apiVersion.getAPIVersion(), apiVersion.getBaseURL(), apiVersion.getCompatibilityRange())));


        final var history = froniusService.getHistory();
        add(new Pre(history.body()));
    }

}
