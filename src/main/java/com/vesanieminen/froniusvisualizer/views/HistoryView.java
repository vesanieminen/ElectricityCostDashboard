package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vesanieminen.froniusvisualizer.components.HistoryTemplate;

import static com.vesanieminen.froniusvisualizer.views.MainLayout.URL_SUFFIX;

@Route(value = "historiakaavio", layout = MainLayout.class)
@PageTitle("History" + URL_SUFFIX)
public class HistoryView extends Main {

    public HistoryView() {
        add(new HistoryTemplate());
    }

}
