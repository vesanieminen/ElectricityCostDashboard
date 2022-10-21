package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.HasEnabled;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.charts.model.Crosshair;
import com.vaadin.flow.component.charts.model.Labels;
import com.vaadin.flow.component.charts.model.ListSeries;
import com.vaadin.flow.component.charts.model.Marker;
import com.vaadin.flow.component.charts.model.PlotOptionsColumn;
import com.vaadin.flow.component.charts.model.PlotOptionsLine;
import com.vaadin.flow.component.charts.model.SeriesTooltip;
import com.vaadin.flow.component.charts.model.Tooltip;
import com.vaadin.flow.component.charts.model.XAxis;
import com.vaadin.flow.component.charts.model.YAxis;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.FileBuffer;
import com.vaadin.flow.component.upload.receivers.FileData;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vesanieminen.froniusvisualizer.components.DoubleLabel;
import com.vesanieminen.froniusvisualizer.components.Footer;
import com.vesanieminen.froniusvisualizer.components.Spacer;
import com.vesanieminen.froniusvisualizer.services.PriceCalculatorService;

import java.io.IOException;
import java.text.ParseException;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateFixedElectricityPrice;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateSpotElectricityPriceDetails;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.getFingridConsumptionData;
import static com.vesanieminen.froniusvisualizer.util.Utils.decimalFormat;
import static com.vesanieminen.froniusvisualizer.util.Utils.fiLocale;

@Route("price-calculator")
public class PriceCalculatorView extends Div {

    private String lastFile;

    public PriceCalculatorView() throws IOException {
        addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);
        setHeightFull();
        final var wrapper = new Div();
        wrapper.addClassNames(LumoUtility.Margin.AUTO);
        wrapper.setWidthFull();
        wrapper.setMaxWidth(1024, Unit.PIXELS);
        add(wrapper);
        final var content = new Div();
        content.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.Padding.Horizontal.MEDIUM);
        wrapper.add(content);

        final var title = new Span("Spot price / fixed electricity price calculator");
        title.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.MEDIUM);
        content.add(title);
        final var spotAverage = PriceCalculatorService.calculateSpotAveragePriceThisYear();
        final var span = new Span("Spot average this year: " + decimalFormat.format(spotAverage) + " c/kWh");
        span.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
        content.add(span);
        final var spotAverageMonth = PriceCalculatorService.calculateSpotAveragePriceThisMonth();
        final var spanMonth = new Span("Spot average this month: " + decimalFormat.format(spotAverageMonth) + " c/kWh");
        spanMonth.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
        content.add(spanMonth);

        createHelpLayout(content);

        FileBuffer fileBuffer = new FileBuffer();
        final var uploadFingridConsumptionData = new Button("Upload Fingrid consumption.csv data");
        Upload upload = new Upload(fileBuffer);
        upload.setUploadButton(uploadFingridConsumptionData);
        upload.setDropAllowed(true);
        upload.addClassNames(LumoUtility.Margin.Top.XSMALL);
        content.add(upload);
        final var resultLayout = new Div();
        resultLayout.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexWrap.WRAP, LumoUtility.Margin.Top.MEDIUM);

        final var chartLayout = new Div();

        final var fromDateTimePicker = new DateTimePicker("Start period");
        fromDateTimePicker.setRequiredIndicatorVisible(true);
        fromDateTimePicker.setLocale(fiLocale);
        final var toDateTimePicker = new DateTimePicker("End period");
        toDateTimePicker.setRequiredIndicatorVisible(true);
        toDateTimePicker.setLocale(fiLocale);
        content.add(fromDateTimePicker);
        content.add(toDateTimePicker);

        final var fieldRow = new Div();
        fieldRow.addClassNames(LumoUtility.Display.FLEX, LumoUtility.Gap.MEDIUM);
        content.add(fieldRow);
        final var numberField = new NumberField("Fixed price");
        numberField.setRequiredIndicatorVisible(true);
        numberField.setSuffixComponent(new Span("c/kWh"));
        numberField.setPlaceholder("E.g. 12.50");
        numberField.addClassNames(LumoUtility.Flex.GROW);
        final var spotMargin = new NumberField("Spot margin");
        spotMargin.setRequiredIndicatorVisible(true);
        spotMargin.setSuffixComponent(new Span("c/kWh"));
        spotMargin.setPlaceholder("E.g. 0.38");
        spotMargin.addClassNames(LumoUtility.Flex.GROW);
        fieldRow.add(numberField, spotMargin);

        final var button = new Button("Calculate costs", e -> {
            try {
                if (numberField.getValue() == null) {
                    Notification.show("Please add fixed price to compare with!", 3000, Notification.Position.MIDDLE);
                    return;
                }
                if (spotMargin.getValue() == null) {
                    Notification.show("Missing spot margin", 3000, Notification.Position.MIDDLE);
                    return;
                }
                final var consumptionData = getFingridConsumptionData(lastFile);
                final var spotCalculation = calculateSpotElectricityPriceDetails(consumptionData.data, spotMargin.getValue(), fromDateTimePicker.getValue(), toDateTimePicker.getValue());
                final var fixedCost = calculateFixedElectricityPrice(consumptionData.data, numberField.getValue(), fromDateTimePicker.getValue(), toDateTimePicker.getValue());
                resultLayout.removeAll();
                chartLayout.removeAll();
                final var start = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(fiLocale).format(spotCalculation.start);
                final var end = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(fiLocale).format(spotCalculation.end);
                resultLayout.add(new DoubleLabel("Calculation period (start times)", start + " - " + end, true));
                resultLayout.add(new DoubleLabel("Total consumption over period", decimalFormat.format(spotCalculation.totalConsumption) + "kWh", true));
                resultLayout.add(new DoubleLabel("Average spot price (incl. margin)", decimalFormat.format(spotCalculation.totalCost / spotCalculation.totalConsumption * 100) + " c/kWh", true));
                resultLayout.add(new DoubleLabel("Total spot cost (incl. margin)", decimalFormat.format(spotCalculation.totalCost) + "€", true));
                resultLayout.add(new DoubleLabel("Total spot cost (without margin)", decimalFormat.format(spotCalculation.totalCostWithoutMargin) + "€", true));
                resultLayout.add(new DoubleLabel("Fixed price", numberField.getValue() + " c/kWh", true));
                resultLayout.add(new DoubleLabel("Fixed cost total", decimalFormat.format(fixedCost) + "€", true));

                chartLayout.add(createChart(spotCalculation));

            } catch (IOException | ParseException ex) {
                throw new RuntimeException(ex);
            }
        });
        button.addClassNames(LumoUtility.Margin.Top.MEDIUM);

        upload.addSucceededListener(event -> {
            FileData savedFileData = fileBuffer.getFileData();
            lastFile = savedFileData.getFile().getAbsolutePath();
            System.out.printf("File saved to: %s%n", lastFile);
            try {
                final var consumptionData = getFingridConsumptionData(lastFile);
                fromDateTimePicker.setMin(consumptionData.start);
                fromDateTimePicker.setMax(consumptionData.end.minusHours(1));
                fromDateTimePicker.setValue(consumptionData.start);
                toDateTimePicker.setMin(consumptionData.start.plusHours(1));
                toDateTimePicker.setMax(consumptionData.end);
                toDateTimePicker.setValue(consumptionData.end);
                updateCalculateButtonState(button, numberField.getValue(), spotMargin.getValue(), fromDateTimePicker, toDateTimePicker);
                setFieldsEnabled(true, numberField, spotMargin, fromDateTimePicker, toDateTimePicker);
            } catch (IOException | ParseException e) {
                throw new RuntimeException(e);
            }
        });
        upload.addFailedListener(e -> setFieldsEnabled(false, numberField, spotMargin, fromDateTimePicker, toDateTimePicker, button));
        numberField.addValueChangeListener(e -> updateCalculateButtonState(button, numberField.getValue(), spotMargin.getValue(), fromDateTimePicker, toDateTimePicker));
        spotMargin.addValueChangeListener(e -> updateCalculateButtonState(button, numberField.getValue(), spotMargin.getValue(), fromDateTimePicker, toDateTimePicker));
        fromDateTimePicker.addValueChangeListener(e -> updateCalculateButtonState(button, numberField.getValue(), spotMargin.getValue(), fromDateTimePicker, toDateTimePicker));
        toDateTimePicker.addValueChangeListener(e -> updateCalculateButtonState(button, numberField.getValue(), spotMargin.getValue(), fromDateTimePicker, toDateTimePicker));
        setFieldsEnabled(false, numberField, spotMargin, fromDateTimePicker, toDateTimePicker);
        button.setEnabled(false);
        content.add(button);
        add(resultLayout);
        add(chartLayout);
        add(new Spacer());
        add(new Footer());
    }

    private static Chart createChart(PriceCalculatorService.SpotCalculation spotCalculation) {
        var chart = new Chart(ChartType.COLUMN);
        //chart.setMinHeight("500px");
        //chart.setHeight("580px");
        //chart.setMaxWidth("1320px");
        chart.getConfiguration().setTitle("Consumption / cost per hour");
        chart.getConfiguration().getLegend().setEnabled(true);
        chart.getConfiguration().getChart().setStyledMode(true);
        final var tooltip = new Tooltip();
        tooltip.setValueDecimals(2);
        tooltip.setShared(true);
        chart.getConfiguration().setTooltip(tooltip);
        XAxis xAxis = new XAxis();
        xAxis.setCrosshair(new Crosshair());
        final var xLabel = new Labels();
        //xLabel.setFormatter("return this.value +':00'");
        xAxis.setLabels(xLabel);
        //xLabel.setStep(2);
        final var categories = IntStream.range(0, 24).mapToObj(i -> i + ":00").toList();
        xAxis.setCategories(categories.toArray(String[]::new));
        chart.getConfiguration().addxAxis(xAxis);

        // First YAxis
        final var consumptionYAxis = new YAxis();
        var labelsConsumption = new Labels();
        labelsConsumption.setFormatter("return this.value +' kWh'");
        consumptionYAxis.setLabels(labelsConsumption);
        consumptionYAxis.setTitle("Consumption");
        consumptionYAxis.setOpposite(false);
        chart.getConfiguration().addyAxis(consumptionYAxis);

        // Second YAxis
        final var costYAxis = new YAxis();
        var labels = new Labels();
        labels.setReserveSpace(true);
        labels.setFormatter("return this.value +'€'");
        costYAxis.setLabels(labels);
        costYAxis.setTitle("Price");
        costYAxis.setOpposite(true);
        chart.getConfiguration().addyAxis(costYAxis);

        // third YAxis
        final var spotYAxis = new YAxis();
        spotYAxis.setVisible(false);
        var spotLabels = new Labels();
        ////spotLabels.setReserveSpace(true);
        spotLabels.setFormatter("return this.value +'c/kWh'");
        spotYAxis.setLabels(spotLabels);
        spotYAxis.setTitle("Spot");
        spotYAxis.setOpposite(true);
        chart.getConfiguration().addyAxis(spotYAxis);

        final var consumptionHoursSeries = new ListSeries("Consumption");
        for (int i = 0; i < spotCalculation.consumptionHours.length; ++i) {
            consumptionHoursSeries.addData(spotCalculation.consumptionHours[i]);
        }
        final var consumptionPlotOptionsColumn = new PlotOptionsColumn();
        final var consumptionTooltipSpot = new SeriesTooltip();
        consumptionTooltipSpot.setValueDecimals(2);
        consumptionTooltipSpot.setValueSuffix("kWh");
        consumptionPlotOptionsColumn.setTooltip(consumptionTooltipSpot);
        consumptionHoursSeries.setPlotOptions(consumptionPlotOptionsColumn);

        final var costHoursSeries = new ListSeries("Cost");
        for (int i = 0; i < spotCalculation.costHours.length; ++i) {
            costHoursSeries.addData(spotCalculation.costHours[i]);
        }
        final var costHoursPlotOptionsColumn = new PlotOptionsColumn();
        final var costHoursTooltipSpot = new SeriesTooltip();
        costHoursTooltipSpot.setValueDecimals(2);
        costHoursTooltipSpot.setValueSuffix("€");
        costHoursPlotOptionsColumn.setTooltip(costHoursTooltipSpot);
        costHoursSeries.setPlotOptions(costHoursPlotOptionsColumn);

        final var spotAverageSeries = new ListSeries("Spot average (incl. margin)");
        for (int i = 0; i < spotCalculation.spotAverage.length; ++i) {
            spotAverageSeries.addData(spotCalculation.spotAverage[i]);
        }
        final var spotAveragePlotOptionsColumn = new PlotOptionsLine();
        spotAveragePlotOptionsColumn.setMarker(new Marker(false));
        final var spotAverageTooltipSpot = new SeriesTooltip();
        spotAverageTooltipSpot.setValueDecimals(2);
        spotAverageTooltipSpot.setValueSuffix("c/kWh");
        spotAveragePlotOptionsColumn.setTooltip(spotAverageTooltipSpot);
        spotAverageSeries.setPlotOptions(spotAveragePlotOptionsColumn);

        chart.getConfiguration().setSeries(consumptionHoursSeries, costHoursSeries, spotAverageSeries);
        consumptionHoursSeries.setyAxis(consumptionYAxis);
        costHoursSeries.setyAxis(costYAxis);
        spotAverageSeries.setyAxis(spotYAxis);

        return chart;
    }

    private static Div createHelpLayout(Div content) {
        final var helpButton = new Button("Click to show/hide help");
        helpButton.addClassNames(LumoUtility.Margin.Top.SMALL, LumoUtility.Background.BASE);
        content.add(helpButton);
        final var helpLayout = new Div();
        helpLayout.setVisible(false);
        helpLayout.add(new Span("Usage:"));
        helpLayout.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);
        final var helpStepsLayout = new Div();
        helpStepsLayout.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.Margin.Left.LARGE);
        helpLayout.add(helpStepsLayout);
        content.add(helpLayout);
        helpStepsLayout.add(new Span(new Span("1) Login to "), new Anchor("https://www.fingrid.fi/en/electricity-market/datahub/sign-in-to-datahub-customer-portal/", "Fingrid Datahub.")));
        helpStepsLayout.add(new Span("2) Download your hourly consumption data csv file."));
        helpStepsLayout.add(new Span("3) Upload the file below."));
        helpStepsLayout.add(new Span("4) Select the date and time range for the calculation (the end of day is 23:00)."));
        helpStepsLayout.add(new Span("5) Enter your comparative fixed electricity cost in the field below."));
        helpStepsLayout.add(new Span("6) Enter your spot price margin."));
        helpStepsLayout.add(new Span("7) Click the calculate costs button."));
        helpButton.addClickListener(e -> helpLayout.setVisible(!helpLayout.isVisible()));

        final var anchor = new Anchor("https://raw.githubusercontent.com/vesanieminen/ElectricityCostDashboard/main/src/main/resources/META-INF/resources/data/consumption.csv", "Download example csv file here");
        anchor.addClassNames(LumoUtility.Margin.Top.MEDIUM);
        helpLayout.add(anchor);

        final var additionalInfo = new Span("Do note that the Fingrid csv data is in UTC timezone which is currently 3h earlier than the Finnish timezone. E.g. to calculate costs for August 2022 you need to have 3h from end of July in the csv file as well.");
        additionalInfo.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
        helpLayout.add(additionalInfo);

        return helpLayout;
    }

    private void setFieldsEnabled(boolean isEnabled, HasEnabled... hasEnableds) {
        Stream.of(hasEnableds).forEach(field -> field.setEnabled(isEnabled));
    }

    private void updateCalculateButtonState(Button button, Double fixedPrice, Double spotPrice, DateTimePicker fromDateTimePicker, DateTimePicker toDateTimePicker) {
        button.setEnabled(fixedPrice != null && spotPrice != null && lastFile != null && fromDateTimePicker.getValue() != null && toDateTimePicker.getValue() != null && fromDateTimePicker.getValue().isBefore(toDateTimePicker.getValue()) && !fromDateTimePicker.isInvalid() && !toDateTimePicker.isInvalid());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        final var backButton = new Button("Back to electricity price graph");
        backButton.addClassNames(LumoUtility.Height.MEDIUM, LumoUtility.Margin.NONE, LumoUtility.Margin.Bottom.MEDIUM, LumoUtility.BorderColor.CONTRAST_10, LumoUtility.Border.ALL, LumoUtility.BorderRadius.NONE);
        backButton.addClickListener(e -> attachEvent.getUI().navigate(NordpoolspotView.class));
        addComponentAsFirst(backButton);
    }

}
