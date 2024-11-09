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
    private final Grid<Map<String, Double>> grid;

    public DailyHourlyPricesView(HourlyPricesView.PreservedState preservedState, ObjectMapperService mapperService) {
        this.mapperService = mapperService;

        addClassNames(LumoUtility.Overflow.AUTO, LumoUtility.Padding.Horizontal.SMALL, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);
        // Added fix for iOS Safari header height that changes when scrolling
        setHeight("var(--fullscreen-height-daily-hourly-prices)");

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

    private @NotNull Grid<Map<String, Double>> createGrid() {
        Grid<Map<String, Double>> grid = new Grid<>();

        // Add a column for the hour
        grid.addColumn(item -> {
                    Double hourValue = item.get("Hour");
                    return hourValue != null ? "%d:00".formatted(hourValue.intValue()) : getTranslation("avg.");
                })
                .setHeader(getTranslation("Hour"))
                .setSortable(true)
                .setAutoWidth(true)
                .setFrozen(true);

        // Add columns for each day (Monday to Sunday)
        for (DayOfWeek day : DayOfWeek.values()) {
            String dayName = day.getDisplayName(TextStyle.FULL, getLocale());
            grid.addColumn(item -> {
                        Double price = item.get(dayName);
                        return price != null ? String.format("%.2f", price) : "";
                    })
                    .setHeader(day.getDisplayName(TextStyle.SHORT, getLocale()))
                    .setSortable(true)
                    .setPartNameGenerator(item -> {
                        Double price = item.get(dayName);
                        if (price == null) return "normal";
                        if (price <= 5) return "cheap";
                        if (price < 10) return "normal";
                        return "expensive";
                    });
        }

        // Add a column for the average price across all days
        grid.addColumn(item -> {
                    Double avgPrice = item.get("Average");
                    return avgPrice != null ? String.format("%.2f", avgPrice) : "";
                })
                .setHeader(getTranslation("avg."))
                .setSortable(true)
                .setAutoWidth(true)
                .setPartNameGenerator(item -> {
                    Double avgPrice = item.get("Average");
                    if (avgPrice == null) return "normal";
                    if (avgPrice <= 5) return "cheap";
                    if (avgPrice < 10) return "normal";
                    return "expensive";
                });

        // Add a column for the average price for weekdays (Monday-Friday)
        grid.addColumn(item -> {
                    Double weekdayAvgPrice = item.get("Weekday Average");
                    return weekdayAvgPrice != null ? String.format("%.2f", weekdayAvgPrice) : "";
                })
                .setHeader(getTranslation("Mon - Fri"))
                .setSortable(true)
                .setAutoWidth(true)
                .setPartNameGenerator(item -> {
                    Double weekdayAvgPrice = item.get("Weekday Average");
                    if (weekdayAvgPrice == null) return "normal";
                    if (weekdayAvgPrice <= 5) return "cheap";
                    if (weekdayAvgPrice < 10) return "normal";
                    return "expensive";
                });

        // Add a column for the average price for weekends (Saturday-Sunday)
        grid.addColumn(item -> {
                    Double weekendAvgPrice = item.get("Weekend Average");
                    return weekendAvgPrice != null ? String.format("%.2f", weekendAvgPrice) : "";
                })
                .setHeader(getTranslation("Sat - Sun"))
                .setSortable(true)
                .setAutoWidth(true)
                .setPartNameGenerator(item -> {
                    Double weekendAvgPrice = item.get("Weekend Average");
                    if (weekendAvgPrice == null) return "normal";
                    if (weekendAvgPrice <= 5) return "cheap";
                    if (weekendAvgPrice < 10) return "normal";
                    return "expensive";
                });

        // Set a class name generator to style the summary row
        grid.setClassNameGenerator(item -> {
            if (item.containsKey("SummaryRow")) {
                return "summary-row"; // This class can be used in your CSS to style the row
            }
            return null;
        });

        return grid;
    }

    private void createResult() {
        Map<DayOfWeek, PriceCalculatorService.averageMinMax> weeklyPrices = getHourlyAveragePricesByDay(
                fromDatePicker.getValue().atStartOfDay(fiZoneID).toInstant(),
                toDatePicker.getValue().atStartOfDay(fiZoneID).toInstant(),
                true
        );

        List<Map<String, Double>> hourlyDataList = new ArrayList<>();

        // Initialize maps to accumulate sums per day
        Map<String, Double> sumPerDay = new HashMap<>();
        Map<String, Integer> countPerDay = new HashMap<>();

        for (int hour = 0; hour < 24; hour++) {
            Map<String, Double> hourData = new HashMap<>();
            hourData.put("Hour", (double) hour);  // Store the hour as key "Hour"

            double sumPrices = 0;
            int countPrices = 0;

            double sumWeekdayPrices = 0;
            int countWeekdayPrices = 0;

            double sumWeekendPrices = 0;
            int countWeekendPrices = 0;

            for (DayOfWeek day : DayOfWeek.values()) {
                PriceCalculatorService.averageMinMax dayPrices = weeklyPrices.get(day);
                String dayName = day.getDisplayName(TextStyle.FULL, getLocale());
                if (dayPrices != null) {
                    Double price = dayPrices.average()[hour] != null ? dayPrices.average()[hour].doubleValue() : null;
                    if (price != null) {
                        hourData.put(dayName, price);
                        sumPrices += price;
                        countPrices++;

                        // Accumulate sums per day
                        sumPerDay.merge(dayName, price, Double::sum);
                        countPerDay.merge(dayName, 1, Integer::sum);

                        if (day.getValue() >= DayOfWeek.MONDAY.getValue() && day.getValue() <= DayOfWeek.FRIDAY.getValue()) {
                            // Weekday
                            sumWeekdayPrices += price;
                            countWeekdayPrices++;
                        } else {
                            // Weekend
                            sumWeekendPrices += price;
                            countWeekendPrices++;
                        }
                    } else {
                        hourData.put(dayName, null);
                    }
                } else {
                    hourData.put(dayName, null);
                }
            }

            // Calculate and store the average across all days
            if (countPrices > 0) {
                double avgPrice = sumPrices / countPrices;
                hourData.put("Average", avgPrice);
                // Accumulate for "Average" column
                sumPerDay.merge("Average", avgPrice, Double::sum);
                countPerDay.merge("Average", 1, Integer::sum);
            } else {
                hourData.put("Average", null);
            }

            // Calculate and store the weekday average
            if (countWeekdayPrices > 0) {
                double weekdayAvgPrice = sumWeekdayPrices / countWeekdayPrices;
                hourData.put("Weekday Average", weekdayAvgPrice);
                // Accumulate for "Weekday Average" column
                sumPerDay.merge("Weekday Average", weekdayAvgPrice, Double::sum);
                countPerDay.merge("Weekday Average", 1, Integer::sum);
            } else {
                hourData.put("Weekday Average", null);
            }

            // Calculate and store the weekend average
            if (countWeekendPrices > 0) {
                double weekendAvgPrice = sumWeekendPrices / countWeekendPrices;
                hourData.put("Weekend Average", weekendAvgPrice);
                // Accumulate for "Weekend Average" column
                sumPerDay.merge("Weekend Average", weekendAvgPrice, Double::sum);
                countPerDay.merge("Weekend Average", 1, Integer::sum);
            } else {
                hourData.put("Weekend Average", null);
            }

            hourlyDataList.add(hourData);
        }

        // After processing all hours, compute the averages per day
        Map<String, Double> summaryData = new HashMap<>();
        summaryData.put("Hour", null); // For the Hour column, display "Average" or leave it empty

        for (String key : sumPerDay.keySet()) {
            Double totalSum = sumPerDay.get(key);
            Integer totalCount = countPerDay.get(key);
            if (totalCount > 0) {
                double average = totalSum / totalCount;
                summaryData.put(key, average);
            } else {
                summaryData.put(key, null);
            }
        }

        // Optional: Add a marker to identify the summary row
        summaryData.put("SummaryRow", 1.0);

        // Add the summary row to the data list
        hourlyDataList.add(summaryData);

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