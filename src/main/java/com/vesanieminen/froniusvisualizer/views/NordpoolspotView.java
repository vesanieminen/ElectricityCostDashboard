package com.vesanieminen.froniusvisualizer.views;

import com.fatboyindustrial.gsonjavatime.Converters;
import com.google.gson.GsonBuilder;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.charts.model.DashStyle;
import com.vaadin.flow.component.charts.model.Label;
import com.vaadin.flow.component.charts.model.ListSeries;
import com.vaadin.flow.component.charts.model.Marker;
import com.vaadin.flow.component.charts.model.PlotLine;
import com.vaadin.flow.component.charts.model.PlotOptionsLine;
import com.vaadin.flow.component.charts.model.Tooltip;
import com.vaadin.flow.component.charts.model.XAxis;
import com.vaadin.flow.component.charts.model.YAxis;
import com.vaadin.flow.component.charts.model.style.SolidColor;
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
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.IntStream;

@Route("")
public class NordpoolspotView extends Div {

    private final DecimalFormat df = new DecimalFormat("#.00");

    public NordpoolspotView() throws URISyntaxException, IOException, InterruptedException, ParseException {
        addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.AlignItems.CENTER, LumoUtility.TextColor.PRIMARY_CONTRAST);
        setHeightFull();

        final var request = HttpRequest.newBuilder().uri(new URI("https://www.nordpoolgroup.com/api/marketdata/page/10")).GET().build();
        final var response = HttpClient.newBuilder().build().send(request, HttpResponse.BodyHandlers.ofString());
        final var gson = Converters.registerAll(new GsonBuilder()).create();
        final var nordpoolResponse = gson.fromJson(response.body(), NordpoolResponse.class);
        //Stream.of(nordpoolResponse.data.getClass().getDeclaredFields()).forEach(field -> getAdd(nordpoolResponse, field));

        Chart chart = new Chart(ChartType.LINE);
        chart.setHeightFull();
        //ChartOptions.get().setTheme(new LumoDarkTheme());
        add(chart);

        NumberFormat format = NumberFormat.getInstance(Locale.FRANCE);
        format.setMaximumFractionDigits(2);
        final var prices = nordpoolResponse.data.Rows.subList(0, 24).stream().map(getRowNumberFunction(format)).toList();
        final var dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);
        final var listSeries = new ListSeries("Electricity price on " + nordpoolResponse.data.DataStartdate.format(dateTimeFormatter) + " - " + nordpoolResponse.data.DataEnddate.format(dateTimeFormatter), prices);
        chart.getConfiguration().addSeries(listSeries);
        final var plotOptionsLine = new PlotOptionsLine();
        plotOptionsLine.setMarker(new Marker(true));
        chart.getConfiguration().setPlotOptions(plotOptionsLine);
        final var tooltip = new Tooltip();
        tooltip.setFormatter("function() { return this.y + ' snt/kWh <br/>' + this.x + ' - ' + (parseInt(this.x.split(':')[0]) + 1) + ':00' }");
        chart.getConfiguration().setTooltip(tooltip);

        final var xAxis = new XAxis();
        xAxis.setCategories(IntStream.range(0, 24).mapToObj(i -> i + ":00").toArray(String[]::new));
        xAxis.setTitle("Time");
        chart.getConfiguration().addxAxis(xAxis);
        final var yAxis = new YAxis();
        yAxis.setMin(0);
        yAxis.setTitle("Price");
        chart.getConfiguration().addyAxis(yAxis);

        if (LocalDateTime.now().getDayOfMonth() == nordpoolResponse.data.Rows.get(5).StartTime.getDayOfMonth()) {
            PlotLine plotLine = new PlotLine();
            plotLine.setColor(SolidColor.RED);
            plotLine.setDashStyle(DashStyle.SOLID);
            plotLine.setWidth(2);
            plotLine.setValue(LocalDateTime.now().getHour());
            chart.getConfiguration().getxAxis().addPlotLine(plotLine);
        }

        final var averageValue = mapToPrice(format, nordpoolResponse.data.Rows.get(26));
        PlotLine averagePrice = new PlotLine();
        averagePrice.setLabel(new Label("Average value: " + averageValue + " snt/kWh"));
        averagePrice.setColor(SolidColor.GREEN);
        averagePrice.setDashStyle(DashStyle.DASH);
        averagePrice.setWidth(2);
        averagePrice.setValue(averageValue);
        chart.getConfiguration().getyAxis().addPlotLine(averagePrice);
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
        return Double.valueOf(df.format(format.parse(row.Columns.get(5).Value).doubleValue() * 1.24d / 10));
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
