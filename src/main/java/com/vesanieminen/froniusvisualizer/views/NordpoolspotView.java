package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.AxisType;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.charts.model.DataSeries;
import com.vaadin.flow.component.charts.model.DataSeriesItem;
import com.vaadin.flow.component.charts.model.DateTimeLabelFormats;
import com.vaadin.flow.component.charts.model.Labels;
import com.vaadin.flow.component.charts.model.Marker;
import com.vaadin.flow.component.charts.model.PlotLine;
import com.vaadin.flow.component.charts.model.PlotOptionsLine;
import com.vaadin.flow.component.charts.model.RangeSelector;
import com.vaadin.flow.component.charts.model.RangeSelectorButton;
import com.vaadin.flow.component.charts.model.RangeSelectorTimespan;
import com.vaadin.flow.component.charts.model.Series;
import com.vaadin.flow.component.charts.model.SeriesTooltip;
import com.vaadin.flow.component.charts.model.Tooltip;
import com.vaadin.flow.component.charts.model.XAxis;
import com.vaadin.flow.component.charts.model.YAxis;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vesanieminen.froniusvisualizer.components.DoubleLabel;
import com.vesanieminen.froniusvisualizer.services.FingridService;
import com.vesanieminen.froniusvisualizer.services.NordpoolSpotService;
import com.vesanieminen.froniusvisualizer.services.model.FingridResponse;
import com.vesanieminen.froniusvisualizer.services.model.FingridWindEstimateResponse;
import com.vesanieminen.froniusvisualizer.services.model.NordpoolResponse;
import org.openjdk.jol.info.GraphLayout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Route("")
public class NordpoolspotView extends Div implements HasUrlParameter<String> {

    private final DoubleLabel priceNow;
    private final DoubleLabel lowestAndHighest;
    private final DoubleLabel averagePrice;
    private static final String fiElectricityPriceTitle = "FI electricity price";
    private static final String hydroPowerProductionTitle = "Hydro production";
    private static final String windPowerProductionTitle = "Wind production";
    private static final String nuclearPowerProductionTitle = "Nuclear production";
    private static final String solarPowerProductionTitle = "Solar power";
    private static final String consumptionTitle = "Consumption";
    private static final String importExportTitle = "Net export - import";
    private final String windProductionEstimateTitle = "Wind production estimate";
    private final String totalRenewablesTitle = "Total renewables";
    private final String vat10 = "vat=10";
    private final String vat0 = "vat=0";
    private final double vat24Value = 1.24d;
    private final double vat10Value = 1.10d;
    private final double vat0Value = 1d;
    private double vat = vat24Value;

    private boolean isFullscreen = false;

    private final Button fullScreenButton;
    private final DecimalFormat df = new DecimalFormat("#0.00");

    private boolean isInitialRender = true;

    @Autowired
    private Environment environment;

    public NordpoolspotView() {
        addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.AlignItems.CENTER, LumoUtility.TextColor.PRIMARY_CONTRAST);
        setHeightFull();

        priceNow = new DoubleLabel("Price now", "");
        priceNow.addClassNames(LumoUtility.Border.RIGHT);
        //priceNow.addClassNamesToSpans("color-yellow");
        lowestAndHighest = new DoubleLabel("Lowest / highest today", "");
        lowestAndHighest.addClassNames(LumoUtility.Border.RIGHT);
        averagePrice = new DoubleLabel("7 day average", "");

        fullScreenButton = createButton("Fullscreen");
        if (isFullscreen) {
            fullScreenButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        }
        fullScreenButton.addClickListener(e -> {
            isFullscreen = !isFullscreen;
            fullScreenButton.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
            if (isFullscreen) {
                fullScreenButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            }
            renderView();
        });
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        if (parameter != null) {
            switch (parameter) {
                case vat10 -> this.vat = vat10Value;
                case vat0 -> this.vat = vat0Value;
                default -> this.vat = vat24Value;
            }
        } else {
            this.vat = vat24Value;
        }
        if (!isInitialRender) {
            renderView();
        }
    }

    @Override
    protected void onAttach(AttachEvent e) {
        final var chart = renderView();
        e.getUI().getPage().retrieveExtendedClientDetails(details -> {
            if(details.isTouchDevice()) {
                chart.getConfiguration().getRangeSelector().setSelected(2);
                if(details.getScreenWidth() < 1000) {
                    YAxis production = chart.getConfiguration().getyAxis(0);
                    production.setTitle("Production (GWh)");
                    production.getLabels().setFormatter("return this.value/1000");
                    YAxis price = chart.getConfiguration().getyAxis(1);
                    price.setTitle("Price (c/kWh)");
                    price.getLabels().setFormatter(null);
                }
                chart.drawChart(true);
            }
            fullScreenButton.setVisible(!details.isTouchDevice());
        });
    }

    private Chart renderView() {
        isInitialRender = false;
        NordpoolResponse nordpoolResponse = null;
        FingridResponse fingridResponse = null;
        List<FingridWindEstimateResponse> windEstimateResponses = null;
        try {
            // the TVO OL3 requires some page crawling to work reliably
            //var test = getDayAheadPrediction();
            nordpoolResponse = NordpoolSpotService.getLatest7Days();
            fingridResponse = FingridService.getLatest7Days();
            windEstimateResponses = FingridService.getWindEstimate(environment);
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        removeAll();
        createVatButtons();
        var pricesLayout = new Div(priceNow, lowestAndHighest, averagePrice);
        pricesLayout.addClassNames(LumoUtility.Display.FLEX, LumoUtility.Width.FULL);
        add(pricesLayout);

        var chart = new Chart(ChartType.LINE);
        // Buggy still and cannot be enabled yet:
        //chart.getConfiguration().setExporting(true);
        //final var exporting = chart.getConfiguration().getExporting();
        //exporting.setScale(1);
        //exporting.setSourceHeight(500);
        //exporting.setSourceWidth(1320);
        //exporting.setAllowHTML(true);
        chart.setTimeline(true);
        chart.getConfiguration().getLegend().setEnabled(true);
        chart.getConfiguration().getChart().setStyledMode(true);
        if (isFullscreen) {
            chart.setHeightFull();
        } else {
            chart.setMinHeight("500px");
            chart.setHeight("580px");
            chart.setMaxWidth("1320px");
            //chart.setHeightFull();
        }

        // create x and y-axis
        createXAxis(chart);
        createFingridYAxis(chart);
        createSpotPriceYAxis(chart);

        NumberFormat format = NumberFormat.getInstance(Locale.FRANCE);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

        final var hydroPowerSeries = createDataSeries(fingridResponse.HydroPower, hydroPowerProductionTitle);
        final var windPowerSeries = createDataSeries(fingridResponse.WindPower, windPowerProductionTitle);
        final var nuclearPowerSeries = createDataSeries(fingridResponse.NuclearPower, nuclearPowerProductionTitle);
        final var solarPowerSeries = createDataSeries(fingridResponse.SolarPower, solarPowerProductionTitle);
        final var consumptionSeries = createDataSeries(fingridResponse.Consumption, consumptionTitle);
        final var importExportSeries = createDataSeries(fingridResponse.NetImportExport, importExportTitle);
        final var renewablesSeries = createRenewablesDataSeries(fingridResponse);
        final var windEstimateDataSeries = createWindEstimateDataSeries(windEstimateResponses);
        final var spotPriceDataSeries = createSpotPriceDataSeries(nordpoolResponse, chart, format, dateTimeFormatter, new ArrayList<>(Arrays.asList(hydroPowerSeries, windPowerSeries, nuclearPowerSeries, solarPowerSeries, consumptionSeries, importExportSeries, windEstimateDataSeries, renewablesSeries)));
        configureChartTooltips(chart, hydroPowerSeries, windPowerSeries, nuclearPowerSeries, solarPowerSeries, consumptionSeries, importExportSeries, spotPriceDataSeries, windEstimateDataSeries, renewablesSeries);

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
        rangeSelector.setEnabled(true);

        // TODO: bring back the average price per day?
        //final var averageValue = mapToPrice(format, nordpoolResponse.data.Rows.get(26));
        //PlotLine averagePrice = new PlotLine();
        //averagePrice.setLabel(new Label("Average price: " + averageValue + " c/kWh"));
        //averagePrice.setValue(averageValue);
        //chart.getConfiguration().getyAxis().addPlotLine(averagePrice);

        add(chart);
        createFooter();
        return chart;
    }

    private void createFooter() {
        final var spacer = new Div();
        spacer.addClassNames(LumoUtility.Display.FLEX, LumoUtility.Flex.GROW);
        add(spacer);
        final var icon = new Icon(VaadinIcon.VAADIN_H);
        icon.addClassNames(LumoUtility.Height.MEDIUM, LumoUtility.TextColor.PRIMARY);
        final var vaadin = new Anchor("http://vaadin.com", "Built with Vaadin ");
        final var anchor = new Anchor("https://github.com/vesanieminen/ElectricityCostDashboard", "Fork me on GitHub");
        final var githubIcon = new Image("images/GitHub-Mark-32px.png", "GitHub icon");
        githubIcon.addClassNames(LumoUtility.IconSize.MEDIUM, LumoUtility.TextColor.PRIMARY);
        final var spanLayout = new Span(vaadin, icon, anchor, githubIcon);
        spanLayout.addClassNames(LumoUtility.Display.FLEX, LumoUtility.JustifyContent.CENTER, LumoUtility.AlignItems.CENTER, LumoUtility.Gap.SMALL);
        final var footer = new Div(spanLayout);
        getUI().ifPresent(ui -> ui.getElement().getThemeList());
        footer.addClassNames(LumoUtility.Display.FLEX, LumoUtility.Background.CONTRAST_5, LumoUtility.Width.FULL, LumoUtility.Height.LARGE);
        footer.addClassNames(LumoUtility.AlignItems.CENTER, LumoUtility.JustifyContent.CENTER, LumoUtility.Flex.SHRINK_NONE);
        add(footer);
    }

    private static void printSizeOf(Object object) {
        System.out.println(GraphLayout.parseInstance(object).toFootprint());
    }

    private static void createFingridYAxis(Chart chart) {
        final var fingridYAxis = new YAxis();
        var labelsFingrid = new Labels();
        labelsFingrid.setFormatter("return this.value +' MWh'");
        fingridYAxis.setLabels(labelsFingrid);
        fingridYAxis.setTitle("Production");
        fingridYAxis.setOpposite(false);
        chart.getConfiguration().addyAxis(fingridYAxis);
    }

    private static void createSpotPriceYAxis(Chart chart) {
        final var yAxisSpot = new YAxis();
        var labels = new Labels();
        //labels.setAlign(HorizontalAlign.RIGHT);
        labels.setReserveSpace(true);
        labels.setFormatter("return this.value +' c/kWh'");
        yAxisSpot.setLabels(labels);
        yAxisSpot.setMin(0);
        //yAxisSpot.setMinRange(-1);
        yAxisSpot.setTitle("Price");
        yAxisSpot.setOpposite(true);
        chart.getConfiguration().addyAxis(yAxisSpot);
    }

    private static void createXAxis(Chart chart) {
        final var xAxis = new XAxis();
        xAxis.setTitle("Time");
        xAxis.setType(AxisType.DATETIME);
        chart.getConfiguration().addxAxis(xAxis);
    }

    private static void configureChartTooltips(Chart chart, DataSeries hydroPowerSeries, DataSeries windPowerSeries, DataSeries nuclearPowerSeries, DataSeries solarPowerSeries, DataSeries consumptionSeries, DataSeries importExportSeries, DataSeries spotPriceDataSeries, DataSeries windEstimateDataSeries, DataSeries renewablesSeries) {
        final var plotOptionsLineSpot = new PlotOptionsLine();
        plotOptionsLineSpot.setStickyTracking(true);
        plotOptionsLineSpot.setMarker(new Marker(false));
        final var seriesTooltipSpot = new SeriesTooltip();
        seriesTooltipSpot.setValueDecimals(2);
        seriesTooltipSpot.setValueSuffix(" c/kWh");
        final var dateTimeLabelFormats = new DateTimeLabelFormats();
        seriesTooltipSpot.setDateTimeLabelFormats(dateTimeLabelFormats);
        plotOptionsLineSpot.setTooltip(seriesTooltipSpot);
        spotPriceDataSeries.setPlotOptions(plotOptionsLineSpot);

        final var plotOptionsLine = new PlotOptionsLine();
        plotOptionsLine.setStickyTracking(true);
        plotOptionsLine.setMarker(new Marker(false));
        chart.getConfiguration().setPlotOptions(plotOptionsLine);
        final var tooltip = new Tooltip();
        tooltip.setValueDecimals(0);
        tooltip.setValueSuffix(" MWh");
        chart.getConfiguration().setTooltip(tooltip);

        // Change the fingrid series to use the 2nd y-axis
        hydroPowerSeries.setVisible(false);
        windPowerSeries.setVisible(true);
        nuclearPowerSeries.setVisible(false);
        solarPowerSeries.setVisible(false);
        consumptionSeries.setVisible(false);
        importExportSeries.setVisible(false);
        windEstimateDataSeries.setVisible(true);
        renewablesSeries.setVisible(false);
        spotPriceDataSeries.setyAxis(1);

        // Add plotline to point the current time:
        PlotLine plotLine = new PlotLine();
        plotLine.setClassName("time");
        final LocalDateTime nowWithHourOnly = getCurrentTimeWithHourPrecision();
        plotLine.setValue(nowWithHourOnly.toEpochSecond(ZoneOffset.UTC) * 1000);
        chart.getConfiguration().getxAxis().addPlotLine(plotLine);
    }

    private DataSeries createDataSeries(List<FingridResponse.Data> datasource, String title) {
        final var dataSeries = new DataSeries(title);
        for (FingridResponse.Data data : datasource) {
            final var dataSeriesItem = new DataSeriesItem();
            dataSeriesItem.setX(data.start_time.plusHours(3).withMinute(0).toInstant());
            dataSeriesItem.setY(data.value);
            dataSeries.add(dataSeriesItem);
        }
        return dataSeries;
    }

    private DataSeries createRenewablesDataSeries(FingridResponse fingridResponse) {
        final var dataSeries = new DataSeries(totalRenewablesTitle);
        for (int i = 0; i < fingridResponse.WindPower.size() && i < fingridResponse.HydroPower.size() && i < fingridResponse.SolarPower.size(); ++i) {
            final var value = fingridResponse.WindPower.get(i).value + fingridResponse.HydroPower.get(i).value + fingridResponse.SolarPower.get(i).value;
            final var dataSeriesItem = new DataSeriesItem();
            dataSeriesItem.setX(fingridResponse.WindPower.get(i).start_time.withMinute(0).plusHours(3).toInstant());
            dataSeriesItem.setY(value);
            dataSeries.add(dataSeriesItem);
        }
        return dataSeries;
    }

    private DataSeries createWindEstimateDataSeries(List<FingridWindEstimateResponse> dataSource) {
        final var dataSeries = new DataSeries(windProductionEstimateTitle);
        for (FingridWindEstimateResponse response : dataSource) {
            final var dataSeriesItem = new DataSeriesItem();
            dataSeriesItem.setX(response.start_time.toInstant().plus(Duration.ofHours(3)));
            dataSeriesItem.setY(response.value);
            dataSeries.add(dataSeriesItem);
        }
        return dataSeries;
    }

    private static LocalDateTime getCurrentTimeWithHourPrecision() {
        final var now = LocalDateTime.now(ZoneId.of("Europe/Helsinki"));
        return now.minusMinutes(now.getMinute()).minusSeconds(now.getSecond()).minusNanos(now.getNano());
    }

    private void createVatButtons() {
        Button vat24Button = createButton("VAT 24%");
        Button vat10Button = createButton("VAT 10%");
        Button vat0Button = createButton("VAT 0%");
        final var buttonLayout = new Div(vat24Button, vat10Button, vat0Button, fullScreenButton);
        buttonLayout.addClassNames(LumoUtility.Display.FLEX, LumoUtility.Width.FULL);
        add(buttonLayout);

        // Add event listeners
        vat24Button.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(NordpoolspotView.class)));
        vat10Button.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(NordpoolspotView.class, vat10)));
        vat0Button.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(NordpoolspotView.class, vat0)));

        if (vat == vat24Value) {
            vat24Button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        }
        if (vat == vat10Value) {
            vat10Button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        }
        if (vat == vat0Value) {
            vat0Button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        }
    }

    private static Button createButton(String text) {
        Button button = new Button(text);
        button.setWidthFull();
        button.addClassNames(LumoUtility.BorderRadius.NONE, LumoUtility.Margin.Vertical.NONE, LumoUtility.Height.LARGE);
        return button;
    }

    private DataSeries createSpotPriceDataSeries(NordpoolResponse nordpoolResponse, Chart chart, NumberFormat format, DateTimeFormatter dateTimeFormatter, ArrayList<Series> series) {
        var now = getCurrentTimeWithHourPrecision();
        var highest = Double.MIN_VALUE;
        var lowest = Double.MAX_VALUE;
        var total = 0d;
        var amount = 0;
        final var dataSeries = new DataSeries(fiElectricityPriceTitle);
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
        averagePrice.setTitleBottom(df.format(total / amount) + " c/kWh");
        series.add(0, dataSeries);
        chart.getConfiguration().setSeries(series);
        return dataSeries;
    }

}
