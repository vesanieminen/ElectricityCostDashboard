package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vesanieminen.froniusvisualizer.components.BarChartTemplate;
import com.vesanieminen.froniusvisualizer.components.SettingsDialog;

import static com.vesanieminen.froniusvisualizer.views.MainLayout.URL_SUFFIX;

@Route(value = "pylvaskaavio", layout = MainLayout.class)
@RouteAlias(value = "pylvasgraafi", layout = MainLayout.class)
@PageTitle("Column Chart" + URL_SUFFIX)
public class ChartTemplateView extends Main {


    public ChartTemplateView(SettingsDialog.SettingsState settingsState) {
        setHeight("var(--fullscreen-height-column)");
        setMinHeight("300px");
        add(new BarChartTemplate(settingsState));
    }

}
