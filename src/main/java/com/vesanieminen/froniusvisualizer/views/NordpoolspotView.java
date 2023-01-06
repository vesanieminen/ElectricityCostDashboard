package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.AxisType;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.charts.model.DataSeries;
import com.vaadin.flow.component.charts.model.DataSeriesItem;
import com.vaadin.flow.component.charts.model.DateTimeLabelFormats;
import com.vaadin.flow.component.charts.model.Label;
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
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vesanieminen.froniusvisualizer.components.DoubleLabel;
import com.vesanieminen.froniusvisualizer.services.FingridService;
import com.vesanieminen.froniusvisualizer.services.NordpoolSpotService;
import com.vesanieminen.froniusvisualizer.services.SpotHintaService;
import com.vesanieminen.froniusvisualizer.services.model.FingridLiteResponse;
import com.vesanieminen.froniusvisualizer.services.model.FingridRealtimeResponse;
import com.vesanieminen.froniusvisualizer.services.model.NordpoolResponse;
import com.vesanieminen.froniusvisualizer.services.model.SpotHintaResponse;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.vesanieminen.froniusvisualizer.services.FingridService.fingridDataUpdated;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateSpotAveragePriceThisMonth;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateSpotAveragePriceThisYear;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateSpotAveragePriceToday;
import static com.vesanieminen.froniusvisualizer.util.Utils.convertNordpoolLocalDateTimeToFinnish;
import static com.vesanieminen.froniusvisualizer.util.Utils.format;
import static com.vesanieminen.froniusvisualizer.util.Utils.getCurrentTimeWithHourPrecision;
import static com.vesanieminen.froniusvisualizer.util.Utils.getNumberFormat;
import static com.vesanieminen.froniusvisualizer.util.Utils.numberFormat;
import static com.vesanieminen.froniusvisualizer.util.Utils.utcZone;
import static com.vesanieminen.froniusvisualizer.util.Utils.vat10Instant;
import static com.vesanieminen.froniusvisualizer.views.MainLayout.URL_SUFFIX;

@PageTitle("Chart" + URL_SUFFIX)
@Route(value = "", layout = MainLayout.class)
public class NordpoolspotView extends Main implements HasUrlParameter<String> {

    private final DoubleLabel priceNow;
    private final DoubleLabel lowestAndHighest;
    private final DoubleLabel averagePrice7Days;
    private final DoubleLabel nextPrice;
    private final String fiElectricityPriceTitle;
    private final String hydroPowerProductionTitle;
    private final String windPowerProductionTitle;
    private final String nuclearPowerProductionTitle;
    private final String solarPowerProductionTitle;
    private final String consumptionTitle;
    private final String importExportTitle;
    private final String totalRenewablesTitle;

    private static final String vatDisabled = "vat=off";
    public static final Double vat24Value = 1.24d;
    public static final Double vat10Value = 1.10d;
    private boolean hasVat = true;

    private boolean isFullscreen = false;
    private boolean isTouchDevice = false;

    private final Button fullScreenButton;

    private boolean isInitialRender = true;

    private int screenWidth;

    public NordpoolspotView() {
        addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.AlignItems.CENTER);

        fiElectricityPriceTitle = getTranslation("FI electricity price");
        hydroPowerProductionTitle = getTranslation("Hydro production");
        windPowerProductionTitle = getTranslation("Wind production");
        nuclearPowerProductionTitle = getTranslation("Nuclear production");
        solarPowerProductionTitle = getTranslation("Solar power");
        consumptionTitle = getTranslation("Consumption");
        importExportTitle = getTranslation("Net export - import");
        totalRenewablesTitle = getTranslation("Total renewables");

        priceNow = new DoubleLabel(getTranslation("Price now"), "");
        lowestAndHighest = new DoubleLabel(getTranslation("Lowest / highest today"), "");
        averagePrice7Days = new DoubleLabel(getTranslation("7 day average"), "");
        nextPrice = new DoubleLabel(getTranslation("Price in 1h"), "");

        fullScreenButton = createButton(getTranslation("Fullscreen"));
        fullScreenButton.setVisible(false);
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
            this.hasVat = !vatDisabled.equals(parameter);
        } else {
            this.hasVat = true;
        }
        if (!isInitialRender) {
            renderView();
        }
    }

    @Override
    protected void onAttach(AttachEvent e) {
        final var chart = renderView();
        e.getUI().getPage().retrieveExtendedClientDetails(details -> {
            if (details.isTouchDevice()) {
                isTouchDevice = true;
                screenWidth = details.getBodyClientWidth();
                setTouchDeviceConfiguration(chart);
            }
            fullScreenButton.setVisible(!details.isTouchDevice());
        });
        // Scroll to the top after navigation
        e.getUI().scrollIntoView();
    }

    private void setTouchDeviceConfiguration(Chart chart) {
        if (isTouchDevice) {
            chart.getConfiguration().getRangeSelector().setSelected(1);
            if (screenWidth < 1000) {
                YAxis production = chart.getConfiguration().getyAxis(0);
                production.setTitle(getTranslation("Production") + " (GWh/h)");
                production.getLabels().setFormatter("return this.value/1000");
                YAxis price = chart.getConfiguration().getyAxis(1);
                price.setTitle(getTranslation("Price") + " (" + getTranslation("c/kWh") + ")");
                price.getLabels().setFormatter(null);
                chart.getConfiguration().getyAxis(2).setVisible(false);
            }
            if (screenWidth < 600) {
                chart.getConfiguration().getRangeSelector().setInputEnabled(false);
            }
            setMobileDeviceChartHeight(chart);
            chart.drawChart(true);
        }
    }

    private void setMobileDeviceChartHeight(Chart chart) {
        setHeight("auto");
        chart.setHeight("580px");
    }

    private Chart renderView() {
        isInitialRender = false;
        NordpoolResponse nordpoolResponse;
        FingridRealtimeResponse fingridResponse;
        List<FingridLiteResponse> windEstimateResponses;
        List<FingridLiteResponse> productionEstimateResponses;
        List<FingridLiteResponse> consumptionEstimateResponses;
        List<SpotHintaResponse> temperatureList;
        try {
            // the TVO OL3 requires some page crawling to work reliably
            //var test = getDayAheadPrediction();
            nordpoolResponse = NordpoolSpotService.getLatest7Days();
            fingridResponse = FingridService.getLatest7Days();
            windEstimateResponses = FingridService.getWindEstimate();
            productionEstimateResponses = FingridService.getProductionEstimate();
            consumptionEstimateResponses = FingridService.getConsumptionEstimate();
            temperatureList = SpotHintaService.getLatest();
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        removeAll();
        createMenuLayout();
        final NumberFormat numberFormat = getNumberFormat(getLocale(), 2);
        numberFormat.setMinimumFractionDigits(2);
        final var averageThisMonthLabel = new DoubleLabel(getTranslation("Average this month"), numberFormat.format(calculateSpotAveragePriceThisMonth()));
        final var averageThisYearLabel = new DoubleLabel(getTranslation("Average this year"), numberFormat.format(calculateSpotAveragePriceThisYear()));
        final var averageTodayLabel = new DoubleLabel(getTranslation("Average today"), numberFormat.format(calculateSpotAveragePriceToday()));
        var pricesLayout = new Div(priceNow, nextPrice, averageTodayLabel, averagePrice7Days, averageThisMonthLabel, averageThisYearLabel, lowestAndHighest);
        pricesLayout.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexWrap.WRAP, LumoUtility.Width.FULL/*, LumoUtility.BorderRadius.LARGE, LumoUtility.Border.ALL, LumoUtility.BorderColor.CONTRAST_10*/);
        add(pricesLayout);

        var chart = new Chart(ChartType.LINE);
        chart.setTimeline(true);
        chart.getConfiguration().getLegend().setEnabled(true);
        chart.getConfiguration().getChart().setStyledMode(true);
        if (isFullscreen) {
            chart.setHeight("calc(100vh - 13rem)");
        } else {
            chart.setHeight("580px");
            chart.setMaxWidth("1400px");
        }

        // create x and y-axis
        createXAxis(chart);
        createFingridYAxis(chart);
        createSpotPriceYAxis(chart);
        createTemperatureYAxis(chart);

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

        if (fingridResponse != null) {
            final var hydroPowerSeries = createDataSeries(fingridResponse.HydroPower, hydroPowerProductionTitle);
            final var windPowerSeries = createDataSeries(fingridResponse.WindPower, windPowerProductionTitle);
            final var nuclearPowerSeries = createDataSeries(fingridResponse.NuclearPower, nuclearPowerProductionTitle);
            final var solarPowerSeries = createDataSeries(fingridResponse.SolarPower, solarPowerProductionTitle);
            final var consumptionSeries = createDataSeries(fingridResponse.Consumption, consumptionTitle);
            final var importExportSeries = createDataSeries(fingridResponse.NetImportExport, importExportTitle);
            final var renewablesSeries = createRenewablesDataSeries(fingridResponse);
            final var windEstimateDataSeries = createEstimateDataSeries(windEstimateResponses, getTranslation("Wind production estimate"));
            final var productionEstimateDataSeries = createEstimateDataSeries(productionEstimateResponses, getTranslation("Production estimate"));
            final var consumptionEstimateDataSeries = createEstimateDataSeries(consumptionEstimateResponses, getTranslation("Consumption estimate"));
            // set the default visibility for the series
            hydroPowerSeries.setVisible(false);
            windPowerSeries.setVisible(true);
            nuclearPowerSeries.setVisible(false);
            solarPowerSeries.setVisible(false);
            consumptionSeries.setVisible(false);
            importExportSeries.setVisible(false);
            renewablesSeries.setVisible(false);
            productionEstimateDataSeries.setVisible(false);
            consumptionEstimateDataSeries.setVisible(false);
            final var spotPriceDataSeries = createSpotPriceDataSeries(nordpoolResponse, chart, dateTimeFormatter, new ArrayList<>(Arrays.asList(hydroPowerSeries, windPowerSeries, nuclearPowerSeries, solarPowerSeries, consumptionSeries, importExportSeries, windEstimateDataSeries, consumptionEstimateDataSeries, productionEstimateDataSeries, renewablesSeries)));
            configureChartTooltips(chart, spotPriceDataSeries);
            //setNetToday(fingridResponse, df, netToday);
        } else {
            add(new Span(getTranslation("Fingrid API is down currently ;~(")));
            final var spotPriceDataSeries = createSpotPriceDataSeries(nordpoolResponse, chart, dateTimeFormatter, new ArrayList<>());
            configureChartTooltips(chart, spotPriceDataSeries);
        }

        if (temperatureList != null && temperatureList.size() > 0) {
            var temperatureDataSeries = createTemperatureDataSeries(temperatureList, getTranslation("chart.temperature.series"));
            temperatureDataSeries.setyAxis(2);
            chart.getConfiguration().addSeries(temperatureDataSeries);
            final var plotOptionsLineSpot = new PlotOptionsLine();
            plotOptionsLineSpot.setStickyTracking(true);
            plotOptionsLineSpot.setMarker(new Marker(false));
            final var seriesTooltipSpot = new SeriesTooltip();
            seriesTooltipSpot.setValueDecimals(1);
            seriesTooltipSpot.setValueSuffix(" °C");
            final var dateTimeLabelFormats = new DateTimeLabelFormats();
            seriesTooltipSpot.setDateTimeLabelFormats(dateTimeLabelFormats);
            plotOptionsLineSpot.setTooltip(seriesTooltipSpot);
            temperatureDataSeries.setPlotOptions(plotOptionsLineSpot);
            temperatureDataSeries.setVisible(false);
        }

        final var rangeSelector = new RangeSelector();
        rangeSelector.setButtons(
                new RangeSelectorButton(RangeSelectorTimespan.DAY, 2, getTranslation("1d")),
                new RangeSelectorButton(RangeSelectorTimespan.DAY, 3, getTranslation("2d")),
                new RangeSelectorButton(RangeSelectorTimespan.DAY, 4, getTranslation("3d")),
                new RangeSelectorButton(RangeSelectorTimespan.DAY, 6, getTranslation("5d")),
                new RangeSelectorButton(RangeSelectorTimespan.ALL, getTranslation("7d"))
        );
        rangeSelector.setButtonSpacing(12);
        rangeSelector.setSelected(isTouchDevice ? 2 : 4);
        chart.getConfiguration().setRangeSelector(rangeSelector);
        rangeSelector.setEnabled(true);
        rangeSelector.setInputEnabled(true);

        //addAveragePrice(nordpoolResponse, chart);

        setTouchDeviceConfiguration(chart);

        add(chart);

        if (nordpoolResponse != null && nordpoolResponse.isValid()) {
            final var spotDataUpdatedTime = convertNordpoolLocalDateTimeToFinnish(nordpoolResponse.data.DateUpdated);
            final var spotDataUpdated = format(spotDataUpdatedTime, getLocale());
            final var spotDataUpdatedSpan = new Span(getTranslation("price.data.updated") + ": " + spotDataUpdated + ", ");
            spotDataUpdatedSpan.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

            final var fingridDataUpdatedFormatted = fingridDataUpdated != null ? format(fingridDataUpdated, getLocale()) : "";
            final var fingridDataUpdatedSpan = new Span(getTranslation("fingrid.data.updated") + ": " + fingridDataUpdatedFormatted);
            fingridDataUpdatedSpan.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
            final var div = new Div(spotDataUpdatedSpan, fingridDataUpdatedSpan);
            div.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexWrap.WRAP, LumoUtility.Gap.Column.XSMALL, LumoUtility.Margin.Horizontal.MEDIUM, LumoUtility.JustifyContent.CENTER);
            add(div);
        }

        final Span fingridFooter = createFingridLicenseSpan();
        add(fingridFooter);
        return chart;
    }

    private static void addAveragePrice(NordpoolResponse nordpoolResponse, Chart chart) {
        // TODO: bring back the average price per day?
        final var averageValue = nordpoolResponse.data.Rows.get(26);
        PlotLine averagePrice = new PlotLine();
        averagePrice.setLabel(new Label("Average price: " + averageValue + " c/kWh"));
        chart.getConfiguration().getyAxis().addPlotLine(averagePrice);
    }

    private Span createFingridLicenseSpan() {
        final var fingridLink = new Anchor("http://data.fingrid.fi", "data.fingrid.fi");
        final var fingridCCLicenseLink = new Anchor("https://creativecommons.org/licenses/by/4.0/", "CC 4.0 B");
        final var fingridSourceSpan = new Span(getTranslation("fingrid.source"));
        final var fingridMainLink = new Anchor("http://fingrid.fi", "Fingrid");
        final var licenseSpan = new Span(getTranslation("fingrid.license"));
        final var fingridFooter = new Span(fingridSourceSpan, fingridMainLink, new Span(" / "), fingridLink, new Span(" / "), licenseSpan, fingridCCLicenseLink);
        fingridFooter.addClassNames(LumoUtility.Display.FLEX, LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL, LumoUtility.Margin.Bottom.XSMALL, LumoUtility.Gap.XSMALL);
        return fingridFooter;
    }

    private void createFingridYAxis(Chart chart) {
        final var fingridYAxis = new YAxis();
        var labelsFingrid = new Labels();
        labelsFingrid.setFormatter("return this.value +' MWh/h'");
        fingridYAxis.setLabels(labelsFingrid);
        fingridYAxis.setTitle(getTranslation("Production"));
        fingridYAxis.setOpposite(false);
        fingridYAxis.setSoftMax(4500);
        chart.getConfiguration().addyAxis(fingridYAxis);
    }

    private void createSpotPriceYAxis(Chart chart) {
        final var yAxisSpot = new YAxis();
        var labels = new Labels();
        labels.setReserveSpace(true);
        labels.setFormatter("return this.value +' c/kWh'");
        yAxisSpot.setLabels(labels);
        yAxisSpot.setSoftMin(0);
        yAxisSpot.setTitle(getTranslation("Price"));
        yAxisSpot.setOpposite(true);
        chart.getConfiguration().addyAxis(yAxisSpot);
    }

    private void createTemperatureYAxis(Chart chart) {
        final var temperatureYAxis = new YAxis();
        var labelsTemperature = new Labels();
        labelsTemperature.setFormatter("return this.value +' °C'");
        temperatureYAxis.setLabels(labelsTemperature);
        temperatureYAxis.setTitle(getTranslation("chart.temperature"));
        temperatureYAxis.setOpposite(true);
        temperatureYAxis.setSoftMax(5);
        temperatureYAxis.setSoftMin(-5);
        chart.getConfiguration().addyAxis(temperatureYAxis);
    }

    private void createXAxis(Chart chart) {
        final var xAxis = new XAxis();
        xAxis.setTitle(getTranslation("Time"));
        xAxis.setType(AxisType.DATETIME);
        chart.getConfiguration().addxAxis(xAxis);
    }

    private void configureChartTooltips(Chart chart, DataSeries spotPriceDataSeries) {
        final var plotOptionsLineSpot = new PlotOptionsLine();
        //plotOptionsLineSpot.setAnimation(false);
        plotOptionsLineSpot.setStickyTracking(true);
        plotOptionsLineSpot.setMarker(new Marker(false));
        final var seriesTooltipSpot = new SeriesTooltip();
        seriesTooltipSpot.setValueDecimals(2);
        seriesTooltipSpot.setValueSuffix(" " + getTranslation("c/kWh"));
        final var dateTimeLabelFormats = new DateTimeLabelFormats();
        seriesTooltipSpot.setDateTimeLabelFormats(dateTimeLabelFormats);
        plotOptionsLineSpot.setTooltip(seriesTooltipSpot);
        spotPriceDataSeries.setPlotOptions(plotOptionsLineSpot);

        final var plotOptionsLine = new PlotOptionsLine();
        //plotOptionsLine.setAnimation(false);
        plotOptionsLine.setStickyTracking(true);
        plotOptionsLine.setMarker(new Marker(false));
        chart.getConfiguration().setPlotOptions(plotOptionsLine);
        final var tooltip = new Tooltip();
        tooltip.setValueDecimals(0);
        tooltip.setValueSuffix(" MWh/h");
        chart.getConfiguration().setTooltip(tooltip);

        spotPriceDataSeries.setyAxis(1);

        // Add plotline to point the current time:
        PlotLine plotLine = new PlotLine();
        plotLine.setClassName("time");
        final LocalDateTime nowWithHourOnly = getCurrentTimeWithHourPrecision();
        plotLine.setValue(nowWithHourOnly.toEpochSecond(ZoneOffset.UTC) * 1000);
        chart.getConfiguration().getxAxis().addPlotLine(plotLine);
    }

    private DataSeries createDataSeries(List<FingridRealtimeResponse.Data> datasource, String title) {
        final var dataSeries = new DataSeries(title);
        for (FingridRealtimeResponse.Data data : datasource) {
            final var dataSeriesItem = new DataSeriesItem();
            dataSeriesItem.setX(data.start_time.plusHours(3).withMinute(0).toInstant());
            dataSeriesItem.setY(data.value);
            dataSeries.add(dataSeriesItem);
        }
        return dataSeries;
    }

    private DataSeries createRenewablesDataSeries(FingridRealtimeResponse fingridResponse) {
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

    private DataSeries createEstimateDataSeries(List<FingridLiteResponse> dataSource, String title) {
        final var dataSeries = new DataSeries(title);
        for (FingridLiteResponse response : dataSource) {
            final var dataSeriesItem = new DataSeriesItem();
            dataSeriesItem.setX(response.start_time.toInstant().plus(Duration.ofHours(3)));
            dataSeriesItem.setY(response.value);
            dataSeries.add(dataSeriesItem);
        }
        return dataSeries;
    }

    private DataSeries createTemperatureDataSeries(List<SpotHintaResponse> dataSource, String title) {
        final var dataSeries = new DataSeries(title);
        for (SpotHintaResponse response : dataSource) {
            final var dataSeriesItem = new DataSeriesItem();
            dataSeriesItem.setX(response.TimeStamp.toInstant().plus(Duration.ofHours(2)));
            dataSeriesItem.setY(response.Temperature);
            dataSeries.add(dataSeriesItem);
        }
        return dataSeries;
    }

    public void setNetToday(FingridRealtimeResponse fingridResponse, DecimalFormat df, DoubleLabel netToday) {
        final var now = getCurrentTimeWithHourPrecision();
        final var value = fingridResponse.NetImportExport.stream().filter(item -> item.start_time.getDayOfMonth() == now.getDayOfMonth()).map(item -> item.value).reduce(0d, Double::sum);
        final var formattedValue = df.format(value) + " MWh/h";
        netToday.setTitleBottom(formattedValue);
        // The red color isn't looking good yet:
        //if (value < 0) {
        //    netToday.getSpanBottom().addClassNames("color-red");
        //}
        //else {
        //    netToday.getSpanBottom().addClassNames("color-red");
        //}
    }

    public enum VAT {
        VAT("With VAT"),
        VAT0("Without VAT");

        private String vatName;

        VAT(String vatName) {
            this.vatName = vatName;
        }

        public String getVatName() {
            return vatName;
        }
    }

    private void createMenuLayout() {
        final var vatComboBox = new ComboBox<VAT>();
        vatComboBox.addClassNames(LumoUtility.Padding.NONE);
        vatComboBox.setMinWidth(8, Unit.EM);
        vatComboBox.setWidthFull();
        vatComboBox.setItems(VAT.values());
        vatComboBox.setValue(hasVat ? VAT.VAT : VAT.VAT0);
        vatComboBox.setItemLabelGenerator(item -> getTranslation(item.getVatName()));
        final var menuLayout = new Div(vatComboBox, fullScreenButton);
        menuLayout.addClassNames(LumoUtility.Display.FLEX, LumoUtility.Width.FULL);
        add(menuLayout);
        // Add event listeners
        vatComboBox.addValueChangeListener(e -> getUI().ifPresent(ui -> {
            switch (e.getValue()) {
                case VAT -> ui.navigate(NordpoolspotView.class);
                case VAT0 -> ui.navigate(NordpoolspotView.class, vatDisabled);
            }
        }));
    }

    private static Button createButton(String text) {
        Button button = new Button(text);
        button.setWidthFull();
        button.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.BorderRadius.NONE, LumoUtility.Margin.Vertical.NONE, LumoUtility.Height.MEDIUM, LumoUtility.BorderColor.CONTRAST_10, LumoUtility.Border.ALL);
        return button;
    }

    private DataSeries createSpotPriceDataSeries(NordpoolResponse nordpoolResponse, Chart chart, DateTimeFormatter dateTimeFormatter, ArrayList<Series> series) {
        final NumberFormat decimalFormat = getNumberFormat(getLocale(), 2);
        decimalFormat.setMinimumFractionDigits(2);
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
                final var localDateTime = LocalDateTime.ofInstant(instant, utcZone);
                dataSeriesItem.setX(instant);
                try {
                    var y = 0.0d;
                    if (hasVat) {
                        if (0 <= instant.compareTo(vat10Instant)) {
                            y = numberFormat.parse(column.Value).doubleValue() * vat10Value / 10;
                        } else {
                            y = numberFormat.parse(column.Value).doubleValue() * vat24Value / 10;
                        }
                    } else {
                        y = numberFormat.parse(column.Value).doubleValue() / 10;
                    }
                    total += y;
                    ++amount;
                    dataSeriesItem.setY(y);
                    if (Objects.equals(localDateTime, now)) {
                        priceNow.setTitleBottom(decimalFormat.format(y));
                    }
                    if (Objects.equals(localDateTime, now.plusHours(1))) {
                        nextPrice.setTitleBottom(decimalFormat.format(y));
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
                    // skip when the time is "-": changing from or to summer time.
                    continue;
                }
                dataSeries.add(dataSeriesItem);
            }
            --columnIndex;
        }
        lowestAndHighest.setTitleBottom(decimalFormat.format(lowest) + " / " + decimalFormat.format(highest));
        averagePrice7Days.setTitleBottom(decimalFormat.format(total / amount));
        series.add(0, dataSeries);
        chart.getConfiguration().setSeries(series);
        return dataSeries;
    }

}
