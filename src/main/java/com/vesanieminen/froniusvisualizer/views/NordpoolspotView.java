package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.charts.model.DataSeries;
import com.vaadin.flow.component.charts.model.DataSeriesItem;
import com.vaadin.flow.component.charts.model.Labels;
import com.vaadin.flow.component.charts.model.Marker;
import com.vaadin.flow.component.charts.model.PlotLine;
import com.vaadin.flow.component.charts.model.PlotOptionsLine;
import com.vaadin.flow.component.charts.model.RangeSelector;
import com.vaadin.flow.component.charts.model.RangeSelectorButton;
import com.vaadin.flow.component.charts.model.RangeSelectorTimespan;
import com.vaadin.flow.component.charts.model.Tooltip;
import com.vaadin.flow.component.charts.model.XAxis;
import com.vaadin.flow.component.charts.model.YAxis;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vesanieminen.froniusvisualizer.components.DoubleLabel;
import com.vesanieminen.froniusvisualizer.services.FingridService;
import com.vesanieminen.froniusvisualizer.services.NordpoolSpotService;
import com.vesanieminen.froniusvisualizer.services.model.FingridResponse;
import com.vesanieminen.froniusvisualizer.services.model.NordpoolResponse;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

@Route("")
public class NordpoolspotView extends Div {

    private final DoubleLabel priceNow;
    private final DoubleLabel lowestAndHighest;
    private final DoubleLabel averagePrice;
    private final DataSeries windPowerSeries;
    private final DataSeries nuclearPowerSeries;
    private final DataSeries solarPowerSeries;
    private final DataSeries consumptionSeries;
    private double vat = 1.24d;
    private final DecimalFormat df = new DecimalFormat("#0.00");
    private Button vat24Button;
    private Button vat10Button;
    private Button vat0Button;

    public NordpoolspotView() throws URISyntaxException, IOException, InterruptedException {
        addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.AlignItems.CENTER, LumoUtility.TextColor.PRIMARY_CONTRAST);
        setHeightFull();

        createVatButtons();

        priceNow = new DoubleLabel("Price now", "");
        //priceNow.addClassNamesToSpans("color-yellow");
        lowestAndHighest = new DoubleLabel("Lowest / highest today", "0.01 / 13.63 c/kWh");
        averagePrice = new DoubleLabel("7 day average", "25.36 c/kWh");
        final var pricesLayout = new Div(priceNow, lowestAndHighest, averagePrice);
        pricesLayout.addClassNames(LumoUtility.Display.FLEX, LumoUtility.Width.FULL);
        pricesLayout.setMaxWidth("1320px");
        add(pricesLayout);

        var nordpoolResponse = NordpoolSpotService.getLatest7Days();
        var fingridResponse = FingridService.getLatest7Days();

        var chart = new Chart(ChartType.LINE);
        //chart.getConfiguration().setExporting(true);
        chart.setTimeline(true);
        chart.getConfiguration().getLegend().setEnabled(true);
        chart.getConfiguration().getChart().setStyledMode(true);
        chart.setHeight("500px");
        chart.setMaxWidth("1320px");
        add(chart);

        NumberFormat format = NumberFormat.getInstance(Locale.FRANCE);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
        final var windPowerProduction = "Wind power production";
        windPowerSeries = new DataSeries(windPowerProduction);
        for (FingridResponse.Data data : fingridResponse.WindPower) {
            final var dataSeriesItem = new DataSeriesItem();
            dataSeriesItem.setX(data.start_time.toInstant());
            dataSeriesItem.setY(data.value);
            windPowerSeries.add(dataSeriesItem);
        }
        nuclearPowerSeries = new DataSeries("Nuclear power production");
        for (FingridResponse.Data data : fingridResponse.NuclearPower) {
            final var dataSeriesItem = new DataSeriesItem();
            dataSeriesItem.setX(data.start_time.toInstant());
            dataSeriesItem.setY(data.value);
            nuclearPowerSeries.add(dataSeriesItem);
        }
        solarPowerSeries = new DataSeries("Solar power production");
        for (FingridResponse.Data data : fingridResponse.SolarPower) {
            final var dataSeriesItem = new DataSeriesItem();
            dataSeriesItem.setX(data.start_time.toInstant());
            dataSeriesItem.setY(data.value);
            solarPowerSeries.add(dataSeriesItem);
        }
        consumptionSeries = new DataSeries("Consumption");
        for (FingridResponse.Data data : fingridResponse.Consumption) {
            final var dataSeriesItem = new DataSeriesItem();
            dataSeriesItem.setX(data.start_time.toInstant());
            dataSeriesItem.setY(data.value);
            consumptionSeries.add(dataSeriesItem);
        }
        createSpotPriceDataSeries(nordpoolResponse, chart, format, dateTimeFormatter);

        addVatButtonListeners(nordpoolResponse, chart, format, dateTimeFormatter);

        final var plotOptionsLine = new PlotOptionsLine();
        plotOptionsLine.setStickyTracking(true);
        plotOptionsLine.setMarker(new Marker(false));
        chart.getConfiguration().setPlotOptions(plotOptionsLine);
        final var tooltip = new Tooltip();
        tooltip.setValueDecimals(2);
        tooltip.setXDateFormat("%A<br />%H:%M %e.%m.%Y");
        tooltip.setPointFormat("{point.y} c/kWh");
        chart.getConfiguration().setTooltip(tooltip);
        //final var fingridTooltip = new Tooltip();
        //fingridTooltip.setValueDecimals(2);
        ////fingridTooltip.setXDateFormat("%A<br />%H:%M %e.%m.%Y");
        //fingridTooltip.setPointFormat("{point.y} MWh");
        //windPowerSeries.getConfiguration().setTooltip(fingridTooltip);
        //tooltip.setFormatter("function() { "
        //        + "var unit = { 'FI electricity price': 'c/kWh', 'Wind power production': 'MWh' }[this.series.name];"
        //        + "return ''+ this.x +': '+ this.y +' '+ unit; }");
        //chart.getConfiguration().setTooltip(tooltip);

        final var xAxis = new XAxis();
        xAxis.setTitle("Time");
        chart.getConfiguration().addxAxis(xAxis);
        final var yAxis = new YAxis();
        var labels = new Labels();
        labels.setFormatter("return this.value +' c/kWh'");
        yAxis.setLabels(labels);
        yAxis.setMin(0);
        yAxis.setTitle("Price");
        chart.getConfiguration().addyAxis(yAxis);
        final var fingridYAxis = new YAxis();
        labels = new Labels();
        labels.setFormatter("return this.value +' MWh'");
        fingridYAxis.setLabels(labels);
        fingridYAxis.setTitle("Production");
        fingridYAxis.setOpposite(true);
        chart.getConfiguration().addyAxis(fingridYAxis);
        windPowerSeries.setyAxis(1);
        windPowerSeries.setVisible(false);
        nuclearPowerSeries.setyAxis(1);
        nuclearPowerSeries.setVisible(false);
        solarPowerSeries.setyAxis(1);
        solarPowerSeries.setVisible(false);
        consumptionSeries.setyAxis(1);
        consumptionSeries.setVisible(false);

        // Add plotline to signify the current time:
        PlotLine plotLine = new PlotLine();
        plotLine.setClassName("time");
        final LocalDateTime nowWithHourOnly = getCurrentTimeWithHourPrecision();
        plotLine.setValue(nowWithHourOnly.toEpochSecond(ZoneOffset.UTC) * 1000);
        chart.getConfiguration().getxAxis().addPlotLine(plotLine);

        final var rangeSelector = new RangeSelector();
        rangeSelector.setButtons(
                new RangeSelectorButton(RangeSelectorTimespan.DAY, 1, "1d"),
                new RangeSelectorButton(RangeSelectorTimespan.DAY, 2, "2d"),
                new RangeSelectorButton(RangeSelectorTimespan.DAY, 3, "3d"),
                new RangeSelectorButton(RangeSelectorTimespan.DAY, 5, "5d"),
                new RangeSelectorButton(RangeSelectorTimespan.ALL, "7d")
        );
        rangeSelector.setButtonSpacing(12);
        rangeSelector.setSelected(4);
        chart.getConfiguration().setRangeSelector(rangeSelector);

        //final var averageValue = mapToPrice(format, nordpoolResponse.data.Rows.get(26));
        //PlotLine averagePrice = new PlotLine();
        //averagePrice.setLabel(new Label("Average price: " + averageValue + " c/kWh"));
        //averagePrice.setValue(averageValue);
        //chart.getConfiguration().getyAxis().addPlotLine(averagePrice);

    }

    private static LocalDateTime getCurrentTimeWithHourPrecision() {
        final var now = LocalDateTime.now(ZoneId.of("Europe/Helsinki"));
        return now.minusMinutes(now.getMinute()).minusSeconds(now.getSecond()).minusNanos(now.getNano());
    }

    private void createVatButtons() {
        vat24Button = new Button("VAT 24%");
        vat24Button.setWidthFull();
        vat24Button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        vat24Button.addClassNames(LumoUtility.BorderRadius.NONE);
        vat10Button = new Button("VAT 10%");
        vat10Button.setWidthFull();
        vat10Button.addClassNames(LumoUtility.BorderRadius.NONE);
        vat0Button = new Button("VAT 0%");
        vat0Button.setWidthFull();
        vat0Button.addClassNames(LumoUtility.BorderRadius.NONE);
        final var buttonLayout = new Div(vat24Button, vat10Button, vat0Button);
        buttonLayout.addClassNames(LumoUtility.Display.FLEX, LumoUtility.Width.FULL);
        add(buttonLayout);
    }

    private void addVatButtonListeners(NordpoolResponse nordpoolResponse, Chart chart, NumberFormat format, DateTimeFormatter dateTimeFormatter) {
        vat24Button.addClickListener(e -> {
            vat = 1.24d;
            var dataSeries = createSpotPriceDataSeries(nordpoolResponse, chart, format, dateTimeFormatter);
            dataSeries.updateSeries();
            vat24Button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            vat10Button.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
            vat0Button.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
        });
        vat10Button.addClickListener(e -> {
            vat = 1.10d;
            var dataSeries = createSpotPriceDataSeries(nordpoolResponse, chart, format, dateTimeFormatter);
            dataSeries.updateSeries();
            vat24Button.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
            vat10Button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            vat0Button.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
        });
        vat0Button.addClickListener(e -> {
            vat = 1;
            var dataSeries = createSpotPriceDataSeries(nordpoolResponse, chart, format, dateTimeFormatter);
            dataSeries.updateSeries();
            vat24Button.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
            vat10Button.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
            vat0Button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        });
    }

    private DataSeries createSpotPriceDataSeries(NordpoolResponse nordpoolResponse, Chart chart, NumberFormat format, DateTimeFormatter dateTimeFormatter) {
        var now = getCurrentTimeWithHourPrecision();
        var highest = Double.MIN_VALUE;
        var lowest = Double.MAX_VALUE;
        var total = 0d;
        var amount = 0;
        final var dataSeries = new DataSeries("FI electricity price");
        final var rows = nordpoolResponse.data.Rows;
        int columnIndex = 6;
        while (columnIndex >= 0) {
            for (NordpoolResponse.Row row : rows.subList(0, rows.size() - 6)) {
                final var dataSeriesItem = new DataSeriesItem();
                final var time = row.StartTime.toString().split("T")[1];
                NordpoolResponse.Column column = row.Columns.get(columnIndex);
                final var dateTimeString = column.Name + " " + time;
                final var dataLocalDataTime = LocalDateTime.parse(dateTimeString, dateTimeFormatter);
                final var instant = dataLocalDataTime.toInstant(ZoneOffset.of("-01:00"));
                final var localDateTime = LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));
                dataSeriesItem.setX(instant);
                try {
                    final var y = format.parse(column.Value).doubleValue() * vat / 10;
                    total += y;
                    ++amount;
                    dataSeriesItem.setY(y);
                    if (Objects.equals(localDateTime, now)) {
                        priceNow.setTitleBottom(df.format(y) + " c/kWh");
                    }
                    if (localDateTime.getDayOfMonth() == now.getDayOfMonth()) {
                        if (y > highest) {
                            highest = y;
                        }
                        if (y < lowest) {
                            lowest = y;
                        }
                    }
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
                dataSeries.add(dataSeriesItem);
            }
            --columnIndex;
        }
        lowestAndHighest.setTitleBottom(df.format(lowest) + " / " + df.format(highest) + " c/kWh");
        //var sum = rows.get(26).Columns.stream().map(column -> {
        //    try {
        //        return format.parse(column.Value).doubleValue() * vat / 10;
        //    } catch (ParseException e) {
        //        throw new RuntimeException(e);
        //    }
        //}).reduce(0d, Double::sum);
        averagePrice.setTitleBottom(df.format(total / amount) + " c/kWh");

        chart.getConfiguration().setSeries(dataSeries, windPowerSeries, nuclearPowerSeries, solarPowerSeries, consumptionSeries);

        return dataSeries;
    }

    private Function<NordpoolResponse.Row, Number> getRowNumberFunction(NumberFormat format) {
        return row -> {
            try {
                return mapToPrice(format, row);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private Double mapToPrice(NumberFormat format, NordpoolResponse.Row row) throws ParseException {
        return Double.valueOf(df.format(format.parse(row.Columns.get(5).Value).doubleValue() * 1 / 10));
    }

}
