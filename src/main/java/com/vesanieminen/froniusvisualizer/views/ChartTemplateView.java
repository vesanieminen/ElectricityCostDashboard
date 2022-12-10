package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vesanieminen.froniusvisualizer.components.ChartTemplate;

import static com.vesanieminen.froniusvisualizer.views.MainLayout.URL_SUFFIX;

@Route(value = "pylvaskaavio", layout = MainLayout.class)
@RouteAlias(value = "pylvasgraafi", layout = MainLayout.class)
@PageTitle("Column Chart" + URL_SUFFIX)
public class ChartTemplateView extends Main {

    public ChartTemplateView() {
        setHeight("var(--fullscreen-height)");
        setMinHeight("300px");
        add(new ChartTemplate());
    }

}
