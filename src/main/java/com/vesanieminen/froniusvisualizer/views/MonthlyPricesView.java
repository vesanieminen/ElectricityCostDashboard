package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vesanieminen.froniusvisualizer.services.PriceCalculatorService;

import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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
        // Fetch the averagePriceMap from PriceCalculatorService
        Map<YearMonth, Double> averagePriceMap = PriceCalculatorService.getAveragePriceMap();

        // Collect all months present in the data
        Set<Month> months = averagePriceMap.keySet().stream()
                .map(YearMonth::getMonth)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Month.class)));

        // Create a sorted list of months to use as grid items
        List<Month> monthList = new ArrayList<>(months);
        monthList.sort(Comparator.comparingInt(Month::getValue));

        // Create the Grid with Month as the item type
        Grid<Month> grid = new Grid<>(Month.class, false);
        grid.setItems(monthList);

        // Add the Month column
        grid.addColumn(month -> month.getDisplayName(TextStyle.FULL_STANDALONE, getLocale()))
                .setHeader(getTranslation("Month"))
                .setSortable(true)
                .setAutoWidth(true)
                .setComparator(Comparator.comparingInt(Month::getValue))
                .setFrozen(true);

        // Get all the years from the data and sort them
        Set<Integer> years = averagePriceMap.keySet().stream()
                .map(YearMonth::getYear)
                .collect(Collectors.toCollection(TreeSet::new)); // Use TreeSet for sorted years

        // For each year, add a column to the Grid
        for (Integer year : years) {
            final int currentYear = year; // Needed for lambda expression
            grid.addColumn(month -> {
                        // Construct YearMonth from current year and month
                        YearMonth yearMonth = YearMonth.of(currentYear, month);
                        Double averagePrice = averagePriceMap.get(yearMonth);
                        return averagePrice == null ? "" : String.format("%.2f %s", averagePrice, getTranslation("c/kWh"));
                    })
                    .setHeader(String.valueOf(currentYear))
                    .setSortable(true)
                    .setAutoWidth(true)
                    .setPartNameGenerator(month -> {
                        YearMonth yearMonth = YearMonth.of(currentYear, month);
                        Double averagePrice = averagePriceMap.get(yearMonth);
                        if (averagePrice == null) {
                            return ""; // No data for this cell
                        }
                        // Apply different CSS classes based on the average price
                        if (averagePrice <= 5) {
                            return "cheap"; // Green background
                        } else if (averagePrice < 10) {
                            return "normal"; // Default background
                        } else {
                            return "expensive"; // Red background
                        }
                    });
        }

        // Add the Grid to the UI
        add(grid);
    }

}
