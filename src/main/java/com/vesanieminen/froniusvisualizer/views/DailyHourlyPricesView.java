package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.page.WebStorage;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vesanieminen.froniusvisualizer.components.CssGrid;
import com.vesanieminen.froniusvisualizer.services.ObjectMapperService;
import com.vesanieminen.froniusvisualizer.services.PriceCalculatorService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.getHourlyAveragePricesByDay;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.spotDataEnd;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.spotDataStart;
import static com.vesanieminen.froniusvisualizer.util.Utils.fiZoneID;
import static com.vesanieminen.froniusvisualizer.views.MainLayout.URL_SUFFIX;

@PageTitle("Daily Hourly Prices" + URL_SUFFIX)
@Route(value = "paivatuntihinnat", layout = MainLayout.class)
public class DailyHourlyPricesView extends Main {

    public static final String FROM_DATE = "daily-hourly-prices.from-date";
    public static final String END_DATE = "daily-hourly-prices.end-date";
    private final DatePicker fromDatePicker;
    private final DatePicker toDatePicker;
    private final ObjectMapperService mapperService;
    private final Grid<PriceCalculatorService.HourValuePerDay> grid;

    public DailyHourlyPricesView(HourlyPricesView.PreservedState preservedState, ObjectMapperService mapperService) {
        this.mapperService = mapperService;

        addClassNames(LumoUtility.Overflow.AUTO, LumoUtility.Padding.Horizontal.SMALL, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);
        // Added fix for iOS Safari header height that changes when scrolling
        //setHeight("var(--fullscreen-height)");

        fromDatePicker = new DatePicker(getTranslation("Start period"));
        fromDatePicker.setId(FROM_DATE);
        fromDatePicker.setWeekNumbersVisible(true);
        fromDatePicker.setI18n(new DatePicker.DatePickerI18n().setFirstDayOfWeek(1));
        fromDatePicker.setRequiredIndicatorVisible(true);
        fromDatePicker.setLocale(getLocale());
        fromDatePicker.setMin(spotDataStart.atZone(fiZoneID).toLocalDate());
        fromDatePicker.setMax(spotDataEnd.atZone(fiZoneID).toLocalDate());
        preservedState.selection.setStartDate(spotDataEnd.atZone(fiZoneID).minusWeeks(1).toLocalDate());
        final var datePickerI18n = new DatePicker.DatePickerI18n();
        datePickerI18n.setFirstDayOfWeek(1);
        datePickerI18n.setDateFormat("EEE dd.MM.yyyy");
        fromDatePicker.setI18n(datePickerI18n);

        toDatePicker = new DatePicker(getTranslation("End period"));
        toDatePicker.setId(END_DATE);
        toDatePicker.setWeekNumbersVisible(true);
        toDatePicker.setI18n(datePickerI18n);
        toDatePicker.setRequiredIndicatorVisible(true);
        toDatePicker.setLocale(getLocale());
        toDatePicker.setMin(spotDataStart.atZone(fiZoneID).toLocalDate());
        toDatePicker.setMax(spotDataEnd.atZone(fiZoneID).toLocalDate());
        preservedState.selection.setEndDate(spotDataEnd.atZone(fiZoneID).toLocalDate());

        grid = createGrid();

        final var binder = new Binder<HourlyPricesView.Selection>();
        binder.bind(fromDatePicker, HourlyPricesView.Selection::getStartDate, HourlyPricesView.Selection::setStartDate);
        binder.bind(toDatePicker, HourlyPricesView.Selection::getEndDate, HourlyPricesView.Selection::setEndDate);
        binder.setBean(preservedState.selection);

        binder.addValueChangeListener(e -> {
            createResult();
        });

        createResult();
        readFieldValues();

        fromDatePicker.addValueChangeListener(item -> mapperService.saveFieldValue(fromDatePicker));
        toDatePicker.addValueChangeListener(item -> mapperService.saveFieldValue(toDatePicker));

        final var cssGrid = new CssGrid(fromDatePicker, toDatePicker);
        add(cssGrid, grid);
    }

    private @NotNull Grid<PriceCalculatorService.HourValuePerDay> createGrid() {
        Grid<PriceCalculatorService.HourValuePerDay> grid = new Grid<>(PriceCalculatorService.HourValuePerDay.class, false);
        grid.setAllRowsVisible(true);

        // Add a column for the day of the week
        grid.addColumn(item -> item.getDayOfWeek().getDisplayName(TextStyle.SHORT, getLocale()))
                .setHeader(getTranslation("Weekday"))
                .setSortable(true)
                .setAutoWidth(true)
                .setFrozen(true);

        // Add columns for each hour dynamically
        IntStream.range(0, 24).forEach(hour ->
                grid.addColumn(item -> String.format("%.2f", item.getHourlyPrice(hour)))
                        .setHeader(hour + ":00")
                        .setSortable(true)
                        .setAutoWidth(true)
                        .setPartNameGenerator(item -> {
                            double averagePrice = item.getHourlyPrice(hour);
                            if (averagePrice <= 5) {
                                return "cheap"; // Green background
                            } else if (averagePrice < 10) {
                                return "normal"; // Default background
                            } else {
                                return "expensive"; // Red background
                            }
                        }));


        return grid;
    }

    private void createResult() {
        Map<DayOfWeek, PriceCalculatorService.averageMinMax> weeklyPrices = getHourlyAveragePricesByDay(
                fromDatePicker.getValue().atStartOfDay(fiZoneID).toInstant(),
                toDatePicker.getValue().atStartOfDay(fiZoneID).toInstant(),
                true
        );

        List<PriceCalculatorService.HourValuePerDay> hourlyDataList = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            PriceCalculatorService.averageMinMax dayPrices = weeklyPrices.get(day);
            Map<Integer, Double> hourlyPrices = new HashMap<>();
            for (int hour = 0; hour < 24; hour++) {
                hourlyPrices.put(hour, dayPrices.average()[hour].doubleValue());
            }
            hourlyDataList.add(new PriceCalculatorService.HourValuePerDay(day, hourlyPrices));
        }
        grid.setItems(hourlyDataList);
    }

    public void readFieldValues() {
        WebStorage.getItem(fromDatePicker.getId().orElseThrow(), item -> mapperService.readLocalDateTime(item, fromDatePicker));
        WebStorage.getItem(toDatePicker.getId().orElseThrow(), item -> mapperService.readLocalDateTime(item, toDatePicker));
    }

    @Setter
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    static class Selection {
        LocalDate startDate;
        LocalDate endDate;
    }

    @VaadinSessionScope
    @Component
    public static class PreservedState {
        HourlyPricesView.Selection selection = new HourlyPricesView.Selection();
    }
}
