package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.List;

import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateSpotAveragePriceOnMonth;
import static com.vesanieminen.froniusvisualizer.views.MainLayout.URL_SUFFIX;

@PageTitle("Monthly Prices" + URL_SUFFIX)
@Route(value = "kuukausihinnat", layout = MainLayout.class)
public class MonthlyPricesView extends Main {

    public record MonthData(int year, int month, double averagePrice) {
    }

    public MonthlyPricesView() {
        addClassNames(LumoUtility.Overflow.AUTO, LumoUtility.Padding.Horizontal.SMALL);
        // Set height to correctly position sticky dates
        // Added fix for iOS Safari header height that changes when scrolling
        setHeight("var(--fullscreen-height)");
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        final var v = calculateSpotAveragePriceOnMonth(2021, 1);
        final var monthDataGrid = new Grid<MonthData>();
        monthDataGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER);
        monthDataGrid.setHeightFull();
        monthDataGrid.addColumn(MonthData::month);
        monthDataGrid.addColumn(MonthData::averagePrice);
        final var monthDataList = List.of(new MonthData(2021, 1, v));
        monthDataGrid.setItems(monthDataList);
        add(monthDataGrid);
    }

}
