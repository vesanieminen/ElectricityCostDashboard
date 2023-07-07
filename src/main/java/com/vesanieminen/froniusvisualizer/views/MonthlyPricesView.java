package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateSpotAveragePriceOnMonth;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.spotDataEnd;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.spotDataStart;
import static com.vesanieminen.froniusvisualizer.util.Utils.fiZoneID;
import static com.vesanieminen.froniusvisualizer.views.MainLayout.URL_SUFFIX;

@PageTitle("Monthly Prices" + URL_SUFFIX)
@Route(value = "kuukausihinnat", layout = MainLayout.class)
public class MonthlyPricesView extends Main {

    public record MonthData(int year, int month, double averagePrice) {
    }

    public MonthlyPricesView() {
        addClassNames(LumoUtility.Overflow.AUTO, LumoUtility.Padding.Horizontal.SMALL, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);
        // Set height to correctly position sticky dates
        // Added fix for iOS Safari header height that changes when scrolling
        setHeight("var(--fullscreen-height)");
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        final var startyear = spotDataStart.atZone(fiZoneID).getYear();
        final var startMonth = spotDataStart.atZone(fiZoneID).getMonth().getValue();
        add(new Span("Start year: " + startyear + ", start month: " + startMonth));

        final var endYear = spotDataEnd.atZone(fiZoneID).getYear();
        final var b = spotDataEnd.atZone(fiZoneID).getDayOfMonth() == spotDataEnd.atZone(fiZoneID).getDayOfMonth();
        final var endMonth = spotDataEnd.atZone(fiZoneID).getMonth().getValue() - 1;
        add(new Span("End year: " + endYear + ", end month: " + endMonth));

        //final var timeRangeSpan = new Span(format(spotDataStart, getLocale()) + " - " + format(spotDataEnd, getLocale()));

        final var v = calculateSpotAveragePriceOnMonth(2021, 1);


    }

}
