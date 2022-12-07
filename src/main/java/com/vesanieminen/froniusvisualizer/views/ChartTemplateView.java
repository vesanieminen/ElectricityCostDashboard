package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vesanieminen.froniusvisualizer.components.ChartTemplate;

@Route(value = "chart-template", layout = MainLayout.class)
public class ChartTemplateView extends Div {

    public ChartTemplateView() {
        add(new ChartTemplate());
    }

}
