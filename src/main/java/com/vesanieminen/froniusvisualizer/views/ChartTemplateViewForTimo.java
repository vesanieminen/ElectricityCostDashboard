package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vesanieminen.froniusvisualizer.components.BarChartTemplateTimo;

import static com.vesanieminen.froniusvisualizer.views.MainLayout.URL_SUFFIX;

@Route(value = "pörssisähkö", layout = MainLayout.class)
@PageTitle("Market electricity" + URL_SUFFIX)
public class ChartTemplateViewForTimo extends Main {

    public ChartTemplateViewForTimo() {
        setHeight("var(--fullscreen-height)");
        setMinHeight("300px");
        add(new BarChartTemplateTimo());
    }

}
