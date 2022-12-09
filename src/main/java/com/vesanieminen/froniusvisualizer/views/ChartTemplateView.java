package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vesanieminen.froniusvisualizer.components.ChartTemplate;

@Route(value = "pylvaskaavio", layout = MainLayout.class)
@RouteAlias(value = "pylvasgraafi", layout = MainLayout.class)
@PageTitle("column.chart")
public class ChartTemplateView extends Main {

    public ChartTemplateView() {
        setHeight("var(--chart-template-height)");
        setMinHeight("300px");
        add(new ChartTemplate());
    }

}
