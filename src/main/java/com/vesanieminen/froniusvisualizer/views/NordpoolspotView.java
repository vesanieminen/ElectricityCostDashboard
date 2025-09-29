package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.AxisType;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.charts.model.Configuration;
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
import com.vaadin.flow.component.charts.model.Time;
import com.vaadin.flow.component.charts.model.Tooltip;
import com.vaadin.flow.component.charts.model.XAxis;
import com.vaadin.flow.component.charts.model.YAxis;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vesanieminen.froniusvisualizer.components.DoubleLabel;
import com.vesanieminen.froniusvisualizer.services.FingridService;
import com.vesanieminen.froniusvisualizer.services.FmiService;
import com.vesanieminen.froniusvisualizer.services.NordpoolSpotService;
import com.vesanieminen.froniusvisualizer.services.SahkovatkainService;
import com.vesanieminen.froniusvisualizer.services.SpotHintaService;
import com.vesanieminen.froniusvisualizer.services.model.FingridLiteResponse;
import com.vesanieminen.froniusvisualizer.services.model.FingridRealtimeResponse;
import com.vesanieminen.froniusvisualizer.services.model.FmiObservationResponse;
import com.vesanieminen.froniusvisualizer.services.model.FmiObservationResponse.FmiObservation;
import com.vesanieminen.froniusvisualizer.services.model.NordpoolPrice;
import com.vesanieminen.froniusvisualizer.services.model.SpotHintaResponse;
import lombok.Getter;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.vesanieminen.froniusvisualizer.services.FingridService.fingridDataUpdated;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateSpotAveragePriceThisMonth;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateSpotAveragePriceThisMonthWithoutVAT;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateSpotAveragePriceThisYear;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateSpotAveragePriceThisYearWithoutVAT;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateSpotAveragePriceToday;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateSpotAveragePriceTodayWithoutVAT;
import static com.vesanieminen.froniusvisualizer.services.SahkovatkainService.getNewHourPrices;
import static com.vesanieminen.froniusvisualizer.util.Utils.fiZoneID;
import static com.vesanieminen.froniusvisualizer.util.Utils.format;
import static com.vesanieminen.froniusvisualizer.util.Utils.getCurrentInstant15minPrecisionFinnishZone;
import static com.vesanieminen.froniusvisualizer.util.Utils.getCurrentLocalDateTime15MinPrecisionFinnishZone;
import static com.vesanieminen.froniusvisualizer.util.Utils.getCurrentTimeWithHourPrecision;
import static com.vesanieminen.froniusvisualizer.util.Utils.getNumberFormat;
import static com.vesanieminen.froniusvisualizer.util.Utils.getVAT;
import static com.vesanieminen.froniusvisualizer.util.Utils.isDaylightSavingsInFinland;
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
    private boolean hasVat = true;
    private boolean isTouchDevice = false;
    private boolean isInitialRender = true;
    private int screenWidth;
    private DataSeries pricePredictionSeries;

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
        final var conf = chart.getConfiguration();
        e.getUI().getPage().retrieveExtendedClientDetails(details -> {
            if (details.isTouchDevice()) {
                isTouchDevice = true;
                screenWidth = details.getBodyClientWidth();
                setTouchDeviceConfiguration(chart, conf);
            }
        });
        // Scroll to the top after navigation
        e.getUI().scrollIntoView();
    }

    private void setTouchDeviceConfiguration(Chart chart, Configuration conf) {
        if (isTouchDevice) {
            if (screenWidth < 1000) {
                conf.getRangeSelector().setSelected(1);
                YAxis production = conf.getyAxis(0);
                production.setTitle(getTranslation("Production") + " (GWh/h)");
                production.getLabels().setFormatter("return this.value/1000");
                YAxis price = conf.getyAxis(1);
                price.setTitle(getTranslation("Price") + " (" + getTranslation("c/kWh") + ")");
                price.getLabels().setFormatter(null);
                conf.getyAxis(2).setVisible(false);
                pricePredictionSeries.setVisible(false);
            }
            if (screenWidth < 600) {
                conf.getRangeSelector().setSelected(1);
                conf.getRangeSelector().setInputEnabled(false);
            }
            setMobileDeviceChartHeight(chart);
            chart.setConfiguration(conf);
        }
    }

    private void setMobileDeviceChartHeight(Chart chart) {
        setHeight("auto");
        chart.setHeight("580px");
    }

    private Chart renderView() {
        isInitialRender = false;
        List<NordpoolPrice> nordpoolPrices;
        FingridRealtimeResponse fingridResponse;
        List<FingridLiteResponse> windEstimateResponses;
        List<FingridLiteResponse> productionEstimateResponses;
        List<FingridLiteResponse> consumptionEstimateResponses;
        List<SpotHintaResponse> temperatureForecastList;
        FmiObservationResponse temperatureObservations;
        try {
            nordpoolPrices = NordpoolSpotService.getLatest7DaysList();
            fingridResponse = FingridService.getLatest7Days();
            windEstimateResponses = FingridService.getWindEstimate();
            productionEstimateResponses = FingridService.getProductionEstimate();
            consumptionEstimateResponses = FingridService.getConsumptionEstimate();
            temperatureForecastList = SpotHintaService.getLatest();
            temperatureObservations = FmiService.getObservations();
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        removeAll();
        createMenuLayout();
        final NumberFormat numberFormat = getNumberFormat(getLocale(), 2);
        numberFormat.setMinimumFractionDigits(2);
        final var averageThisMonthLabel = new DoubleLabel(getTranslation("Average this month"), numberFormat.format(hasVat ? calculateSpotAveragePriceThisMonth() : calculateSpotAveragePriceThisMonthWithoutVAT()));
        final var averageThisYearLabel = new DoubleLabel(getTranslation("Average this year"), numberFormat.format(hasVat ? calculateSpotAveragePriceThisYear() : calculateSpotAveragePriceThisYearWithoutVAT()));
        final var averageTodayLabel = new DoubleLabel(getTranslation("Day's average"), numberFormat.format(hasVat ? calculateSpotAveragePriceToday() : calculateSpotAveragePriceTodayWithoutVAT()));
        var pricesLayout = new Div(priceNow, nextPrice, averageTodayLabel, averagePrice7Days, averageThisMonthLabel, averageThisYearLabel, lowestAndHighest);
        pricesLayout.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexWrap.WRAP, LumoUtility.Width.FULL/*, LumoUtility.BorderRadius.LARGE, LumoUtility.Border.ALL, LumoUtility.BorderColor.CONTRAST_10*/);
        add(pricesLayout);

        var chart = new Chart(ChartType.LINE);
        final var time = new Time();
        time.setUseUTC(false);
        time.setTimezoneOffset((isDaylightSavingsInFinland() ? -3 : -2) * 60);
        chart.getConfiguration().setTime(time);
        chart.setTimeline(true);
        // Attempts to make the Navigator changes be instant. Not successful so far :(
        //chart.getConfiguration().getNavigator().setAdaptToUpdatedData(true);
        //chart.addAttachListener(event -> {
        //    chart.getElement().executeJs(
        //            "const chart = this; " +
        //                    "chart.addEventListener('chartRendered', function() {" +
        //                    "  if (chart.configuration && chart.configuration.navigator) {" +
        //                    "    chart.configuration.navigator.liveRedraw = true;" +
        //                    "    console.log('Navigator liveRedraw set:', chart.configuration.navigator); " +
        //                    "  } else {" +
        //                    "    console.warn('Navigator not found or not initialized yet'); " +
        //                    "  }" +
        //                    "});"
        //    );
        //});

        //chart.getConfiguration().getNavigator().setEnabled(false);
        chart.getConfiguration().getScrollbar().setEnabled(false);
        chart.getConfiguration().getLegend().setEnabled(true);
        chart.getConfiguration().getChart().setStyledMode(true);
        chart.setHeight("calc(100vh - 10rem)");

        // create x and y-axis
        createXAxis(chart);
        createFingridYAxis(chart);
        createSpotPriceYAxis(chart);
        createTemperatureYAxis(chart);

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

        if (fingridResponse != null) {
            final var seriesList = new ArrayList<Series>();
            createDataSeries(fingridResponse.HydroPower, hydroPowerProductionTitle, false, seriesList);
            createDataSeries(fingridResponse.WindPower, windPowerProductionTitle, true, seriesList);
            createDataSeries(fingridResponse.NuclearPower, nuclearPowerProductionTitle, false, seriesList);
            createDataSeries(fingridResponse.SolarPower, solarPowerProductionTitle, false, seriesList);
            createDataSeries(fingridResponse.Consumption, consumptionTitle, false, seriesList);
            createDataSeries(fingridResponse.NetImportExport, importExportTitle, false, seriesList);
            final var windEstimateDataSeries = createEstimateDataSeries(windEstimateResponses, getTranslation("Wind production estimate"), true, seriesList);
            // TODO: add these back in:
            //final var consumptionEstimateDataSeries = createEstimateDataSeries(consumptionEstimateResponses, getTranslation("Consumption estimate"), false, seriesList);
            //final var productionEstimateDataSeries = createEstimateDataSeries(productionEstimateResponses, getTranslation("Production estimate"), false, seriesList);
            createRenewablesDataSeries(fingridResponse, seriesList);

            if (windEstimateDataSeries == null) {
                add(new Span(getTranslation("Fingrid not responding for estimate data")));
            }
            final var spotPriceDataSeries = createSpotPriceDataSeries(nordpoolPrices, chart, dateTimeFormatter, seriesList);
            configureChartTooltips(chart, spotPriceDataSeries);
            //setNetToday(fingridResponse, df, netToday);
        } else {
            add(new Span(getTranslation("Fingrid API is down currently ;~(")));
            final var spotPriceDataSeries = createSpotPriceDataSeries(nordpoolPrices, chart, dateTimeFormatter, new ArrayList<>());
            configureChartTooltips(chart, spotPriceDataSeries);
        }

        var temperatureDataSeries = createTemperatureDataSeries(temperatureForecastList, temperatureObservations, getTranslation("chart.temperature.series"));
        chart.getConfiguration().addSeries(temperatureDataSeries);
        temperatureDataSeries.setVisible(false);
        configureTemperatureDataSeries(temperatureDataSeries);

        pricePredictionSeries = addPredictionPrices(chart);

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

        setTouchDeviceConfiguration(chart, chart.getConfiguration());

        add(chart);

        if (!nordpoolPrices.isEmpty()) {
            final var spotDataUpdatedTime = NordpoolSpotService.getUpdatedAt();
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

        final var sahkovatkainAnchor = new Anchor("https://sahkovatkain.web.app", "Sähkövatkain");
        final var pricePredictionSpan = new Span("%s: ".formatted(getTranslation("Price prediction")));
        final var sahkovatkainSpan = new Span(pricePredictionSpan, sahkovatkainAnchor);
        sahkovatkainSpan.addClassNames(LumoUtility.Display.FLEX, LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL, LumoUtility.Gap.XSMALL);
        add(sahkovatkainSpan);


        chart.drawChart();
        return chart;
    }

    private DataSeries addPredictionPrices(Chart chart) {
        // Price prediction
        final var pricePredictionSeries = new DataSeries();
        pricePredictionSeries.setyAxis(1);
        pricePredictionSeries.setName(getTranslation("Price prediction"));
        if (SahkovatkainService.getHourPrices() == null) {
            return pricePredictionSeries;
        }
        // add the first value as the last known nordpool price
        final var lastItem = new DataSeriesItem();
        final var last = NordpoolSpotService.getPriceList().getLast();
        lastItem.setX(last.time());
        lastItem.setY(last.price() * getVAT(Instant.ofEpochMilli(last.time() * 1000)));
        pricePredictionSeries.add(lastItem);
        // Hide tooltip for the first item
        final var itemTooltip = new Tooltip();
        itemTooltip.setEnabled(false);

        final var hourPrices = getNewHourPrices();
        for (final var hourPrice : hourPrices) {
            final var item = new DataSeriesItem();
            item.setX(hourPrice.timestamp());
            item.setY(hourPrice.price());
            pricePredictionSeries.add(item);

            pricePredictionSeries.add(new DataSeriesItem(hourPrice.timestamp() + 15 * 60 * 1000, hourPrice.price()));
            pricePredictionSeries.add(new DataSeriesItem(hourPrice.timestamp() + 30 * 60 * 1000, hourPrice.price()));
            pricePredictionSeries.add(new DataSeriesItem(hourPrice.timestamp() + 45 * 60 * 1000, hourPrice.price()));
        }
        final var plotOptionsLineSpot = new PlotOptionsLine();
        plotOptionsLineSpot.setClassName("price-prediction");
        plotOptionsLineSpot.setAnimation(false);
        plotOptionsLineSpot.setStickyTracking(true);
        plotOptionsLineSpot.setMarker(new Marker(false));
        final var seriesTooltipSpot = new SeriesTooltip();
        seriesTooltipSpot.setValueDecimals(2);
        seriesTooltipSpot.setValueSuffix(" " + getTranslation("c/kWh"));
        final var dateTimeLabelFormats = new DateTimeLabelFormats();
        // Custom point formatter to hide tooltip for the first item
        seriesTooltipSpot.setPointFormatter(
                "function() {" +
                        "if (this.series.data && this.series.data[0] === this) { return ''; } " +  // Check if the current point is the first item
                        "const formattedY = (Math.round(this.y * 100) / 100).toFixed(2);" +  // Manually format to 2 decimal places
                        "return '<span style=\"color:' + this.color + '\">●</span> ' + this.series.name + ': <b>' + formattedY + '</b>' + this.series.tooltipOptions.valueSuffix;" +
                        "}"
        );
        seriesTooltipSpot.setDateTimeLabelFormats(dateTimeLabelFormats);
        plotOptionsLineSpot.setTooltip(seriesTooltipSpot);
        pricePredictionSeries.setPlotOptions(plotOptionsLineSpot);
        chart.getConfiguration().addSeries(pricePredictionSeries);
        return pricePredictionSeries;
    }

    private static void configureTemperatureDataSeries(DataSeries temperatureDataSeries) {
        temperatureDataSeries.setyAxis(2);
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
    }

    private Span createFingridLicenseSpan() {
        final var fingridLink = new Anchor("http://data.fingrid.fi", "data.fingrid.fi");
        final var fingridCCLicenseLink = new Anchor("https://creativecommons.org/licenses/by/4.0/", "CC 4.0 B");
        final var fingridSourceSpan = new Span(getTranslation("fingrid.source"));
        final var fingridMainLink = new Anchor("http://fingrid.fi", "Fingrid");
        final var licenseSpan = new Span(getTranslation("fingrid.license"));
        final var fingridFooter = new Span(fingridSourceSpan, fingridMainLink, new Span(" / "), fingridLink, new Span(" / "), licenseSpan, fingridCCLicenseLink);
        fingridFooter.addClassNames(LumoUtility.Display.FLEX, LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL, LumoUtility.Gap.XSMALL);
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
        plotOptionsLineSpot.setAnimation(false);
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
        plotOptionsLine.setAnimation(false);
        plotOptionsLine.setStickyTracking(true);
        //plotOptionsLine.setMarker(new Marker(false));
        chart.getConfiguration().setPlotOptions(plotOptionsLine);
        final var tooltip = new Tooltip();
        tooltip.setShared(true);
        tooltip.setValueDecimals(0);
        tooltip.setValueSuffix(" MWh/h");
        chart.getConfiguration().setTooltip(tooltip);

        spotPriceDataSeries.setyAxis(1);

        // Add plotline to point the current time:
        PlotLine plotLine = new PlotLine();
        plotLine.setClassName("time");
        final var nowWithHourOnly = getCurrentInstant15minPrecisionFinnishZone();
        plotLine.setValue(nowWithHourOnly.getEpochSecond() * 1000);
        chart.getConfiguration().getxAxis().addPlotLine(plotLine);
    }

    private void createDataSeries(List<FingridRealtimeResponse.Data> datasource, String title, boolean isVisible, List<Series> seriesList) {
        final var dataSeries = new DataSeries(title);
        dataSeries.setVisible(isVisible);
        for (FingridRealtimeResponse.Data data : datasource) {
            //if (data.startTime.getMinute() != 0) {
            //    continue;
            //}
            final var dataSeriesItem = new DataSeriesItem();
            dataSeriesItem.setX(data.startTime.toInstant());
            dataSeriesItem.setY(data.value);
            dataSeries.add(dataSeriesItem);
        }
        seriesList.add(dataSeries);
    }

    private void createRenewablesDataSeries(FingridRealtimeResponse fingridResponse, ArrayList<Series> seriesList) {
        final var dataSeries = new DataSeries(totalRenewablesTitle);
        dataSeries.setVisible(false);
        for (int i = 0; i < fingridResponse.WindPower.size() && i < fingridResponse.HydroPower.size() && i < fingridResponse.SolarPower.size(); ++i) {
            final var value = fingridResponse.WindPower.get(i).value + fingridResponse.HydroPower.get(i).value + fingridResponse.SolarPower.get(i).value;
            final var dataSeriesItem = new DataSeriesItem();
            dataSeriesItem.setX(fingridResponse.WindPower.get(i).startTime.withMinute(0).toInstant());
            dataSeriesItem.setY(value);
            dataSeries.add(dataSeriesItem);
        }
        seriesList.add(dataSeries);
    }

    private DataSeries createEstimateDataSeries(List<FingridLiteResponse> dataSource, String title, boolean isVisible, List<Series> seriesList) {
        final var dataSeries = new DataSeries(title);
        dataSeries.setVisible(isVisible);
        if (dataSource == null) {
            return null;
        }
        for (FingridLiteResponse response : dataSource) {
            final var dataSeriesItem = new DataSeriesItem();
            if (response.startTime == null) {
                continue;
            }
            dataSeriesItem.setX(response.startTime.toInstant());
            dataSeriesItem.setY(response.value);
            dataSeries.add(dataSeriesItem);
        }
        seriesList.add(dataSeries);
        return dataSeries;
    }


    private DataSeries createTemperatureDataSeries(List<SpotHintaResponse> spotHintadataSource, FmiObservationResponse fmiObservations, String title) {
        final var dataSeries = new DataSeries(title);

        if (fmiObservations != null) {
            for (FmiObservation observation : fmiObservations.getObservations()) {
                final var dataSeriesItem = new DataSeriesItem();
                var timestamp = FmiService.parseFmiTimestamp(observation.getLocaltime(), observation.getLocaltz());
                if (timestamp == null) {
                    continue;
                }
                dataSeriesItem.setX(timestamp.toInstant());
                dataSeriesItem.setY(observation.getTemperature());
                dataSeries.add(dataSeriesItem);
            }
        }

        if (spotHintadataSource != null) {
            for (SpotHintaResponse response : spotHintadataSource) {
                final var dataSeriesItem = new DataSeriesItem();
                dataSeriesItem.setX(response.TimeStamp.toInstant());
                dataSeriesItem.setY(response.Temperature);
                dataSeries.add(dataSeriesItem);
            }
        }

        return dataSeries;
    }

    public void setNetToday(FingridRealtimeResponse fingridResponse, DecimalFormat df, DoubleLabel netToday) {
        final var now = getCurrentTimeWithHourPrecision();
        final var value = fingridResponse.NetImportExport.stream().filter(item -> item.startTime.getDayOfMonth() == now.getDayOfMonth()).map(item -> item.value).reduce(0d, Double::sum);
        final var formattedValue = df.format(value) + " MWh/h";
        netToday.setTitleBottom(formattedValue);
    }

    @Getter
    public enum VAT {
        VAT("With VAT"),
        VAT0("Without VAT");

        private final String vatName;

        VAT(String vatName) {
            this.vatName = vatName;
        }

    }

    private void createMenuLayout() {
        final var vatComboBox = new ComboBox<VAT>();
        vatComboBox.addClassNames(LumoUtility.Padding.NONE);
        vatComboBox.setMinWidth(18, Unit.EM);
        vatComboBox.setWidthFull();
        vatComboBox.setItems(VAT.values());
        vatComboBox.setValue(hasVat ? VAT.VAT : VAT.VAT0);
        vatComboBox.setItemLabelGenerator(item -> getTranslation(item.getVatName()));
        final var menuLayout = new FlexLayout(vatComboBox);
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

    private DataSeries createSpotPriceDataSeries(List<NordpoolPrice> nordpoolPrices, Chart chart, DateTimeFormatter dateTimeFormatter, ArrayList<Series> series) {
        final NumberFormat decimalFormat = getNumberFormat(getLocale(), 2);
        decimalFormat.setMinimumFractionDigits(2);
        var now = getCurrentLocalDateTime15MinPrecisionFinnishZone();
        var highest = Double.MIN_VALUE;
        var lowest = Double.MAX_VALUE;
        var total = 0d;
        var amount = 0;
        final var dataSeries = new DataSeries(fiElectricityPriceTitle);
        if (nordpoolPrices == null) {
            return dataSeries;
        }

        for (NordpoolPrice nordpoolPrice : nordpoolPrices) {
            final var dataSeriesItem = new DataSeriesItem();
            dataSeriesItem.setX(nordpoolPrice.timeInstant());
            final var localDateTime = LocalDateTime.ofInstant(nordpoolPrice.timeInstant(), fiZoneID);
            var y = 0.0d;
            if (hasVat) {
                y = nordpoolPrice.price() * getVAT(nordpoolPrice.timeInstant());
            } else {
                y = nordpoolPrice.price();
            }
            total += y;
            ++amount;
            dataSeriesItem.setY(y);
            if (Objects.equals(localDateTime, now)) {
                priceNow.setTitleBottom(decimalFormat.format(y));
            }
            if (Objects.equals(localDateTime, now.plusMinutes(15))) {
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
            dataSeries.add(dataSeriesItem);
        }

        lowestAndHighest.setTitleBottom(decimalFormat.format(lowest) + " / " + decimalFormat.format(highest));
        averagePrice7Days.setTitleBottom(decimalFormat.format(total / amount));
        series.addFirst(dataSeries);
        chart.getConfiguration().setSeries(series);
        return dataSeries;
    }

}
