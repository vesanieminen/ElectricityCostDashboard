package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.charts.model.Crosshair;
import com.vaadin.flow.component.charts.model.Labels;
import com.vaadin.flow.component.charts.model.ListSeries;
import com.vaadin.flow.component.charts.model.Marker;
import com.vaadin.flow.component.charts.model.PlotOptionsLine;
import com.vaadin.flow.component.charts.model.SeriesTooltip;
import com.vaadin.flow.component.charts.model.Tooltip;
import com.vaadin.flow.component.charts.model.XAxis;
import com.vaadin.flow.component.charts.model.YAxis;
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
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.getHourlyAveragePrices;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.spotDataEnd;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.spotDataStart;
import static com.vesanieminen.froniusvisualizer.util.Utils.fiZoneID;
import static com.vesanieminen.froniusvisualizer.views.MainLayout.URL_SUFFIX;

@PageTitle("Hourly Prices" + URL_SUFFIX)
@Route(value = "tuntihinnat", layout = MainLayout.class)
public class HourlyPricesView extends Main {

    public static final String FROM_DATE = "hourly-prices.from-date";
    public static final String END_DATE = "hourly-prices.end-date";
    private final DatePicker fromDatePicker;
    private final DatePicker toDatePicker;
    private final ObjectMapperService mapperService;
    private final Grid<PriceCalculatorService.HourValue> grid;
    private final Chart chart;

    public HourlyPricesView(PreservedState preservedState, ObjectMapperService mapperService) {
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

        grid = new Grid<>(PriceCalculatorService.HourValue.class, false);
        grid.addColumn(item -> "%d:00 - %d:00".formatted(item.getHour(), item.getHour() + 1))
                .setHeader(getTranslation("Hour"))
                .setSortable(true)
                .setAutoWidth(true);
        grid.addColumn(item -> String.format("%.2f %s", item.getValue(), getTranslation("c/kWh")))
                .setHeader(getTranslation("Average Price"))
                .setSortable(true)
                .setAutoWidth(true)
                .setPartNameGenerator(item -> {
                    double averagePrice = item.getValue();
                    if (averagePrice <= 5) {
                        return "cheap"; // Green background
                    } else if (averagePrice < 10) {
                        return "normal"; // Default background
                    } else {
                        return "expensive"; // Red background
                    }
                });

        chart = createChart();

        final var binder = new Binder<Selection>();
        binder.bind(fromDatePicker, Selection::getStartDate, Selection::setStartDate);
        binder.bind(toDatePicker, Selection::getEndDate, Selection::setEndDate);
        binder.setBean(preservedState.selection);

        binder.addValueChangeListener(e -> {
            createResult();
        });

        createResult();
        readFieldValues();

        fromDatePicker.addValueChangeListener(item -> mapperService.saveFieldValue(fromDatePicker));
        toDatePicker.addValueChangeListener(item -> mapperService.saveFieldValue(toDatePicker));

        final var cssGrid = new CssGrid(fromDatePicker, toDatePicker);
        add(cssGrid, chart, grid);
    }

    private Chart createChart() {
        var chart = new Chart(ChartType.COLUMN);
        chart.getConfiguration().setTitle("");
        chart.getConfiguration().getLegend().setEnabled(true);
        chart.getConfiguration().getChart().setStyledMode(true);
        final var tooltip = new Tooltip();
        tooltip.setValueDecimals(2);
        tooltip.setShared(true);
        chart.getConfiguration().setTooltip(tooltip);
        XAxis xAxis = new XAxis();
        xAxis.setCrosshair(new Crosshair());
        final var xLabel = new Labels();
        xAxis.setLabels(xLabel);
        final var categories = IntStream.range(0, 24).mapToObj(i -> i + ":00").toList();
        xAxis.setCategories(categories.toArray(String[]::new));
        chart.getConfiguration().addxAxis(xAxis);

        // YAxis
        final var spotYAxis = new YAxis();
        spotYAxis.setVisible(true);
        var spotLabels = new Labels();
        spotLabels.setFormatter("return this.value +'c/kWh'");
        spotYAxis.setLabels(spotLabels);
        spotYAxis.setTitle("");
        //spotYAxis.setOpposite(true);
        chart.getConfiguration().addyAxis(spotYAxis);
        return chart;
    }

    private void createResult() {
        final var hourlyAveragePrices = getHourlyAveragePrices(fromDatePicker.getValue().atStartOfDay(fiZoneID).toInstant(), toDatePicker.getValue().atStartOfDay(fiZoneID).toInstant(), true);
        List<PriceCalculatorService.HourValue> hourlyDataList = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            hourlyDataList.add(new PriceCalculatorService.HourValue(hour, hourlyAveragePrices[hour]));
        }
        grid.setItems(hourlyDataList);

        // Unweighted spot average series
        final var series = new ListSeries("");
        for (int i = 0; i < 24; ++i) {
            series.addData(hourlyAveragePrices[i]);
        }
        final var plotOptionsLine = new PlotOptionsLine();
        plotOptionsLine.setMarker(new Marker(false));
        final var seriesTooltip = new SeriesTooltip();
        seriesTooltip.setValueDecimals(2);
        seriesTooltip.setValueSuffix(getTranslation("c/kWh"));
        plotOptionsLine.setTooltip(seriesTooltip);
        series.setPlotOptions(plotOptionsLine);
        //unweightedSpotAverageSeries.setyAxis(spotYAxis);

        chart.getConfiguration().setSeries(series);
        chart.drawChart();
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
        Selection selection = new Selection();
    }

}
