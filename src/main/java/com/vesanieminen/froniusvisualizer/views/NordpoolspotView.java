package com.vesanieminen.froniusvisualizer.views;

import com.fatboyindustrial.gsonjavatime.Converters;
import com.google.gson.GsonBuilder;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.charts.model.DataSeries;
import com.vaadin.flow.component.charts.model.DataSeriesItem;
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
import com.vesanieminen.froniusvisualizer.services.model.NordpoolResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
    private double vat = 1.24d;
    private final DecimalFormat df = new DecimalFormat("#0.00");
    private Button vat24Button;
    private Button vat10Button;
    private Button vat0Button;

    public NordpoolspotView() throws URISyntaxException, IOException, InterruptedException, ParseException {
        addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.AlignItems.CENTER, LumoUtility.TextColor.PRIMARY_CONTRAST);
        setHeightFull();

        createVatButtons();

        priceNow = new DoubleLabel("Price now", "");
        priceNow.addClassNamesToSpans("color-yellow");
        lowestAndHighest = new DoubleLabel("Lowest / highest today", "0.01 / 13.63 c/kWh");
        averagePrice = new DoubleLabel("7 day average", "25.36 c/kWh");
        final var pricesLayout = new Div(priceNow, lowestAndHighest, averagePrice);
        pricesLayout.addClassNames(LumoUtility.Display.FLEX, LumoUtility.Width.FULL);
        pricesLayout.setMaxWidth("1320px");
        add(pricesLayout);

        final var request = HttpRequest.newBuilder().uri(new URI("https://www.nordpoolspot.com/api/marketdata/page/35?currency=,,EUR,EUR")).GET().build();
        final var response = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build().send(request, HttpResponse.BodyHandlers.ofString());
        final var gson = Converters.registerAll(new GsonBuilder()).create();
        var nordpoolResponse = gson.fromJson(response.body(), NordpoolResponse.class);

        var chart = new Chart(ChartType.LINE);
        //chart.getConfiguration().setExporting(true);
        chart.setTimeline(true);
        chart.getConfiguration().getChart().setStyledMode(true);
        chart.setHeight("500px");
        chart.setMaxWidth("1320px");
        add(chart);

        NumberFormat format = NumberFormat.getInstance(Locale.FRANCE);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
        createDataSeries(nordpoolResponse, chart, format, dateTimeFormatter);

        addVatButtonListeners(nordpoolResponse, chart, format, dateTimeFormatter);

        final var plotOptionsLine = new PlotOptionsLine();
        plotOptionsLine.setMarker(new Marker(false));
        chart.getConfiguration().setPlotOptions(plotOptionsLine);
        final var tooltip = new Tooltip();
        tooltip.setValueDecimals(2);
        tooltip.setXDateFormat("%A<br />%H:%M %e.%m.%Y");
        tooltip.setPointFormat("{point.y} c/kWh");
        chart.getConfiguration().setTooltip(tooltip);

        final var xAxis = new XAxis();
        xAxis.setTitle("Time");
        chart.getConfiguration().addxAxis(xAxis);
        final var yAxis = new YAxis();
        yAxis.setSoftMin(0);
        yAxis.setTitle("Price");
        chart.getConfiguration().addyAxis(yAxis);

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
        final var nowWithHourOnly = now.minusMinutes(now.getMinute()).minusSeconds(now.getSecond()).minusNanos(now.getNano());
        return nowWithHourOnly;
    }

    private void createVatButtons() {
        vat24Button = new Button("VAT 24%");
        vat24Button.setWidthFull();
        vat24Button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        vat10Button = new Button("VAT 10%");
        vat10Button.setWidthFull();
        vat0Button = new Button("VAT 0%");
        vat0Button.setWidthFull();
        final var buttonLayout = new Div(vat24Button, vat10Button, vat0Button);
        buttonLayout.addClassNames(LumoUtility.Display.FLEX, LumoUtility.Width.FULL);
        add(buttonLayout);
    }

    private void addVatButtonListeners(NordpoolResponse nordpoolResponse, Chart chart, NumberFormat format, DateTimeFormatter dateTimeFormatter) {
        vat24Button.addClickListener(e -> {
            vat = 1.24d;
            var dataSeries = createDataSeries(nordpoolResponse, chart, format, dateTimeFormatter);
            dataSeries.updateSeries();
            vat24Button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            vat10Button.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
            vat0Button.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
        });
        vat10Button.addClickListener(e -> {
            vat = 1.10d;
            var dataSeries = createDataSeries(nordpoolResponse, chart, format, dateTimeFormatter);
            dataSeries.updateSeries();
            vat24Button.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
            vat10Button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            vat0Button.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
        });
        vat0Button.addClickListener(e -> {
            vat = 1;
            var dataSeries = createDataSeries(nordpoolResponse, chart, format, dateTimeFormatter);
            dataSeries.updateSeries();
            vat24Button.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
            vat10Button.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
            vat0Button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        });
    }

    private DataSeries createDataSeries(NordpoolResponse nordpoolResponse, Chart chart, NumberFormat format, DateTimeFormatter dateTimeFormatter) {
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
        chart.getConfiguration().setSeries(dataSeries);
        lowestAndHighest.setTitleBottom(df.format(lowest) + " / " + df.format(highest) + " c/kWh");
        averagePrice.setTitleBottom(df.format(total / amount) + " c/kWh");
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
