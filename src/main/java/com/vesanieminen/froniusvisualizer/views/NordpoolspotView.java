package com.vesanieminen.froniusvisualizer.views;

import com.fatboyindustrial.gsonjavatime.Converters;
import com.google.gson.GsonBuilder;
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
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vesanieminen.froniusvisualizer.services.model.NordpoolResponse;

import java.io.IOException;
import java.lang.reflect.Field;
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
import java.util.function.Function;

@Route("")
public class NordpoolspotView extends Div {

    private final DecimalFormat df = new DecimalFormat("#.00");

    public NordpoolspotView() throws URISyntaxException, IOException, InterruptedException, ParseException {
        addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.AlignItems.CENTER, LumoUtility.TextColor.PRIMARY_CONTRAST);
        setHeightFull();

        final var request = HttpRequest.newBuilder().uri(new URI("https://www.nordpoolspot.com/api/marketdata/page/35?currency=,,EUR,EUR")).GET().build();
        final var response = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build().send(request, HttpResponse.BodyHandlers.ofString());
        final var gson = Converters.registerAll(new GsonBuilder()).create();
        var nordpoolResponse = gson.fromJson(response.body(), NordpoolResponse.class);
        //Stream.of(nordpoolResponse.data.getClass().getDeclaredFields()).forEach(field -> getAdd(nordpoolResponse, field));

        var chart = new Chart(ChartType.LINE);
        chart.setTimeline(true);
        chart.getConfiguration().getChart().setStyledMode(true);
        chart.setHeightFull();
        add(chart);


        NumberFormat format = NumberFormat.getInstance(Locale.FRANCE);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
        final var dataSeries = new DataSeries("FI electricity price");
        final var rows = nordpoolResponse.data.Rows;
        int columnIndex = 6;
        while (columnIndex >= 0) {
            for (NordpoolResponse.Row row : rows.subList(0, rows.size() - 6)) {
                final var dataSeriesItem = new DataSeriesItem();
                final var time = row.StartTime.toString().split("T")[1];
                NordpoolResponse.Column column = row.Columns.get(columnIndex);
                final var dateTimeString = column.Name + " " + time;
                dataSeriesItem.setX(LocalDateTime.parse(dateTimeString, dateTimeFormatter).toInstant(ZoneOffset.UTC));
                dataSeriesItem.setY(format.parse(column.Value).doubleValue() * 1.24d / 10);
                dataSeries.add(dataSeriesItem);
            }
            --columnIndex;
        }
        chart.getConfiguration().addSeries(dataSeries);

        final var plotOptionsLine = new PlotOptionsLine();
        plotOptionsLine.setMarker(new Marker(true));
        chart.getConfiguration().setPlotOptions(plotOptionsLine);
        final var tooltip = new Tooltip();
        tooltip.setValueDecimals(2);
        tooltip.setXDateFormat("%A<br />%H:%M %e.%m.%Y");
        tooltip.setPointFormat("{point.y} c/kWh");
        chart.getConfiguration().setTooltip(tooltip);

        final var xAxis = new XAxis();
        //xAxis.setCategories(IntStream.range(0, 24).mapToObj(i -> i + ":00").toArray(String[]::new));
        xAxis.setTitle("Time");
        chart.getConfiguration().addxAxis(xAxis);
        final var yAxis = new YAxis();
        yAxis.setSoftMin(0);
        yAxis.setTitle("Price");
        chart.getConfiguration().addyAxis(yAxis);

        //if (LocalDateTime.now().getDayOfMonth() == nordpoolResponse.data.Rows.get(5).StartTime.getDayOfMonth()) {
        // TODO: the x plotline doesn't work anymore
        PlotLine plotLine = new PlotLine();
        plotLine.setClassName("time");
        plotLine.setValue(LocalDateTime.now(ZoneId.of("Europe/Helsinki")).toEpochSecond(ZoneOffset.UTC));
        chart.getConfiguration().getxAxis().addPlotLine(plotLine);
        //}
        final var rangeSelector = new RangeSelector();
        rangeSelector.setButtons(
                new RangeSelectorButton(RangeSelectorTimespan.DAY, 1, "1 day"),
                new RangeSelectorButton(RangeSelectorTimespan.DAY, 2, "2 days"),
                new RangeSelectorButton(RangeSelectorTimespan.DAY, 3, "3 days"),
                new RangeSelectorButton(RangeSelectorTimespan.ALL, "All")
        );
        rangeSelector.setSelected(2);
        chart.getConfiguration().setRangeSelector(rangeSelector);

        //final var averageValue = mapToPrice(format, nordpoolResponse.data.Rows.get(26));
        //PlotLine averagePrice = new PlotLine();
        //averagePrice.setLabel(new Label("Average price: " + averageValue + " c/kWh"));
        //averagePrice.setValue(averageValue);
        //chart.getConfiguration().getyAxis().addPlotLine(averagePrice);
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

    private void getAdd(NordpoolResponse nordpoolResponse, Field field) {
        try {
            field.setAccessible(true);
            add(new Pre(field.getName() + ": " + field.get(nordpoolResponse.data)));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
