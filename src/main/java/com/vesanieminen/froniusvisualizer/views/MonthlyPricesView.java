package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vesanieminen.froniusvisualizer.services.PriceCalculatorService;

import java.util.Set;
import java.util.stream.Collectors;

import static com.vesanieminen.froniusvisualizer.views.MainLayout.URL_SUFFIX;

@PageTitle("Monthly Prices" + URL_SUFFIX)
@Route(value = "kuukausihinnat", layout = MainLayout.class)
public class MonthlyPricesView extends Main {

    public MonthlyPricesView() {
        addClassNames(LumoUtility.Overflow.AUTO, LumoUtility.Padding.Horizontal.SMALL, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);
        // Added fix for iOS Safari header height that changes when scrolling
        setHeight("var(--fullscreen-height)");
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        final var monthlyPrices = PriceCalculatorService.getMonthlyPrices();
        final var grid = new Grid<>(PriceCalculatorService.MonthlyData.class, false);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setItems(monthlyPrices);
        grid.addColumn(PriceCalculatorService.MonthlyData::month).setHeader(getTranslation("Month")).setSortable(true).setAutoWidth(true);
        Set<Integer> years = monthlyPrices.stream().flatMap(md -> md.averagesByYear().keySet().stream()).collect(Collectors.toSet());
        for (Integer year : years) {
            grid.addColumn(md -> {
                final var orDefault = md.averagesByYear().getOrDefault(year, null);
                return orDefault == null ? "" : "%.2f %s".formatted(orDefault, getTranslation("c/kWh"));
            }).setHeader("" + year).setSortable(true).setAutoWidth(true);
        }
        add(grid);
    }

}
