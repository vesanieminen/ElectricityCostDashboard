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
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.checkbox.CheckboxGroupVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateFixedElectricityPrice;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateSpotElectricityPriceDetails;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.getFingridUsageData;
import static com.vesanieminen.froniusvisualizer.util.Utils.decimalFormat;
import static com.vesanieminen.froniusvisualizer.util.Utils.fiLocale;

@Route("price-calculator")
public class PriceCalculatorView extends Div {

    private final DateTimePicker fromDateTimePicker;
    private final DateTimePicker toDateTimePicker;
    private final NumberField fixedPriceField;
    private final NumberField spotMarginField;
    private final NumberField spotProductionMarginField;
    private final List<HasEnabled> fields;
    private final Button button;
    private final CheckboxGroup<Calculations> calculationsCheckboxGroup;
    private String lastConsumptionFile;
    private String lastProductionFile;

    private LocalDateTime startConsumption;
    private LocalDateTime endConsumption;
    private LocalDateTime startProduction;
    private LocalDateTime endProduction;

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

        final var title = new Span("Spot / fixed electricity price calculator");
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

        calculationsCheckboxGroup = new CheckboxGroup<>("Select calculations");
        calculationsCheckboxGroup.addThemeVariants(CheckboxGroupVariant.LUMO_VERTICAL);
        calculationsCheckboxGroup.setItems(Calculations.values());
        calculationsCheckboxGroup.setItemLabelGenerator(Calculations::getName);
        calculationsCheckboxGroup.setItemEnabledProvider(item -> !(Objects.equals(item.getName(), Calculations.SPOT.getName())));
        final var calculations = new HashSet<Calculations>();
        calculations.add(Calculations.SPOT);
        calculationsCheckboxGroup.setValue(calculations);
        content.add(calculationsCheckboxGroup);

        // Layouts
        createHelpLayout(content);
        final var resultLayout = new Div();
        resultLayout.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexWrap.WRAP, LumoUtility.Margin.Top.MEDIUM);
        final var chartLayout = new Div();

        // Consumption file
        FileBuffer consumptionFileBuffer = new FileBuffer();
        final var uploadFingridConsumptionData = new Button("Consumption csv file Upload");
        Upload consumptionUpload = new Upload(consumptionFileBuffer);
        consumptionUpload.setDropLabel(new Span("Drop Fingrid consumption csv file here"));
        consumptionUpload.setDropLabel(new Span("Drop Fingrid consumption file here"));
        consumptionUpload.setUploadButton(uploadFingridConsumptionData);
        consumptionUpload.setDropAllowed(true);
        consumptionUpload.addClassNames(LumoUtility.Margin.Top.XSMALL);
        content.add(consumptionUpload);

        // Consumption file
        FileBuffer productionFileBuffer = new FileBuffer();
        final var uploadFingridproductionData = new Button("Production csv file Upload");
        Upload productionUpload = new Upload(productionFileBuffer);
        productionUpload.setDropLabel(new Span("Drop Fingrid production file here"));
        productionUpload.setUploadButton(uploadFingridproductionData);
        productionUpload.setDropAllowed(true);
        productionUpload.addClassNames(LumoUtility.Margin.Top.XSMALL);
        content.add(productionUpload);
        productionUpload.setVisible(false);

        fromDateTimePicker = new DateTimePicker("Start period");
        fromDateTimePicker.setWeekNumbersVisible(true);
        fromDateTimePicker.setDatePickerI18n(new DatePicker.DatePickerI18n().setFirstDayOfWeek(1));
        fromDateTimePicker.setRequiredIndicatorVisible(true);
        fromDateTimePicker.setLocale(fiLocale);
        toDateTimePicker = new DateTimePicker("End period");
        toDateTimePicker.setWeekNumbersVisible(true);
        toDateTimePicker.setDatePickerI18n(new DatePicker.DatePickerI18n().setFirstDayOfWeek(1));
        toDateTimePicker.setRequiredIndicatorVisible(true);
        toDateTimePicker.setLocale(fiLocale);
        content.add(fromDateTimePicker);
        content.add(toDateTimePicker);

        final var fieldRow = new Div();
        fieldRow.addClassNames(LumoUtility.Display.FLEX, LumoUtility.Gap.Column.MEDIUM, LumoUtility.FlexWrap.WRAP);
        content.add(fieldRow);

        // Fixed price field
        fixedPriceField = new NumberField("Fixed price");
        fixedPriceField.setRequiredIndicatorVisible(true);
        fixedPriceField.setSuffixComponent(new Span("c/kWh"));
        fixedPriceField.setPlaceholder("E.g. 12,68");
        fixedPriceField.addClassNames(LumoUtility.Flex.GROW);
        fixedPriceField.setVisible(false);
        fieldRow.add(fixedPriceField);

        // Spot price field
        spotMarginField = new NumberField("Spot margin");
        spotMarginField.setRequiredIndicatorVisible(true);
        spotMarginField.setSuffixComponent(new Span("c/kWh"));
        spotMarginField.setPlaceholder("E.g. 0,38");
        spotMarginField.addClassNames(LumoUtility.Flex.GROW);
        fieldRow.add(spotMarginField);

        // Spot price field
        spotProductionMarginField = new NumberField("Production margin");
        spotProductionMarginField.setRequiredIndicatorVisible(true);
        spotProductionMarginField.setSuffixComponent(new Span("c/kWh"));
        spotProductionMarginField.setPlaceholder("E.g. 0,30");
        spotProductionMarginField.addClassNames(LumoUtility.Flex.GROW);
        spotProductionMarginField.setVisible(false);
        fieldRow.add(spotProductionMarginField);

        calculationsCheckboxGroup.addValueChangeListener(e -> {
            fixedPriceField.setVisible(e.getValue().contains(Calculations.FIXED));
            spotProductionMarginField.setVisible(e.getValue().contains(Calculations.SPOT_PRODUCTION));
            productionUpload.setVisible(e.getValue().contains(Calculations.SPOT_PRODUCTION));
            updateCalculateButtonState();
        });
        fields = Arrays.asList(fromDateTimePicker, toDateTimePicker, fixedPriceField, spotMarginField, spotProductionMarginField);

        button = new Button("Calculate costs", e -> {
            try {
                if (spotMarginField.getValue() == null) {
                    spotMarginField.setValue(0d);
                }
                if (isCalculatingFixed()) {
                    if (fixedPriceField.getValue() == null) {
                        fixedPriceField.setValue(0d);
                    }
                }
                if (isCalculatingProduction()) {
                    if (spotProductionMarginField.getValue() == null) {
                        spotProductionMarginField.setValue(0d);
                    }
                }
                final var consumptionData = getFingridUsageData(lastConsumptionFile);
                final var spotCalculation = calculateSpotElectricityPriceDetails(consumptionData.data, spotMarginField.getValue(), fromDateTimePicker.getValue(), toDateTimePicker.getValue());
                resultLayout.removeAll();
                chartLayout.removeAll();
                final var start = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(fiLocale).format(spotCalculation.start);
                final var end = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(fiLocale).format(spotCalculation.end);

                // Total labels
                resultLayout.add(new DoubleLabel("Calculation period (start times)", start + " - " + end, true));
                resultLayout.add(new DoubleLabel("Total consumption over period", decimalFormat.format(spotCalculation.totalAmount) + "kWh", true));

                // Spot labels
                resultLayout.add(new DoubleLabel("Average spot price (incl. margin)", decimalFormat.format(spotCalculation.totalCost / spotCalculation.totalAmount * 100) + " c/kWh", true));
                resultLayout.add(new DoubleLabel("Total spot cost (incl. margin)", decimalFormat.format(spotCalculation.totalCost) + "€", true));
                resultLayout.add(new DoubleLabel("Total spot cost (without margin)", decimalFormat.format(spotCalculation.totalCostWithoutMargin) + "€", true));

                var fixedCost = 0d;
                if (isCalculatingFixed()) {
                    resultLayout.add(new DoubleLabel("Fixed price", fixedPriceField.getValue() + " c/kWh", true));
                    fixedCost = calculateFixedElectricityPrice(consumptionData.data, fixedPriceField.getValue(), fromDateTimePicker.getValue(), toDateTimePicker.getValue());
                    resultLayout.add(new DoubleLabel("Fixed cost total", decimalFormat.format(fixedCost) + "€", true));
                }

                // Create spot consumption chart
                chartLayout.add(createChart(spotCalculation, isCalculatingFixed(), "Consumption / cost per hour", "Consumption", "Spot cost"));

                if (isCalculatingProduction()) {
                    final var productionData = getFingridUsageData(lastProductionFile);
                    final var spotProductionCalculation = calculateSpotElectricityPriceDetails(productionData.data, -spotProductionMarginField.getValue(), fromDateTimePicker.getValue(), toDateTimePicker.getValue());
                    resultLayout.add(new DoubleLabel("Total production over period", decimalFormat.format(spotProductionCalculation.totalAmount) + "kWh", true));
                    resultLayout.add(new DoubleLabel("Net spot cost (consumption - production)", decimalFormat.format(spotCalculation.totalCost - spotProductionCalculation.totalCost) + "€", true));
                    resultLayout.add(new DoubleLabel("Net usage (consumption - production)", decimalFormat.format(spotCalculation.totalAmount - spotProductionCalculation.totalAmount) + "kWh", true));
                    if (isCalculatingFixed()) {

                    }
                    resultLayout.add(new DoubleLabel("Average production price (incl. margin)", decimalFormat.format(spotProductionCalculation.totalCost / spotProductionCalculation.totalAmount * 100) + " c/kWh", true));
                    resultLayout.add(new DoubleLabel("Total production value (incl. margin)", decimalFormat.format(spotProductionCalculation.totalCost) + "€", true));
                    resultLayout.add(new DoubleLabel("Total production value (without margin)", decimalFormat.format(spotProductionCalculation.totalCostWithoutMargin) + "€", true));
                    // Create spot production chart
                    chartLayout.add(createChart(spotProductionCalculation, false, "Production / value per hour", "Production", "Production value"));
                }

            } catch (IOException | ParseException ex) {
                throw new RuntimeException(ex);
            }
        });
        button.addClassNames(LumoUtility.Margin.Top.MEDIUM);

        addConsumptionSucceededListener(consumptionFileBuffer, consumptionUpload);
        addProductionSucceededListener(productionFileBuffer, productionUpload);

        fixedPriceField.addValueChangeListener(e -> updateCalculateButtonState());
        spotMarginField.addValueChangeListener(e -> updateCalculateButtonState());
        fromDateTimePicker.addValueChangeListener(e -> updateCalculateButtonState());
        toDateTimePicker.addValueChangeListener(e -> updateCalculateButtonState());
        setFieldsEnabled(false);
        button.setEnabled(false);
        content.add(button);
        add(resultLayout);
        add(chartLayout);
        add(new Spacer());
        add(new Footer());
    }

    private boolean isCalculatingFixed() {
        return calculationsCheckboxGroup.getValue().contains(Calculations.FIXED);
    }

    private boolean isCalculatingProduction() {
        return calculationsCheckboxGroup.getValue().contains(Calculations.SPOT_PRODUCTION);
    }

    private void addConsumptionSucceededListener(FileBuffer fileBuffer, Upload consumptionUpload) {
        consumptionUpload.addSucceededListener(event -> {
            FileData savedFileData = fileBuffer.getFileData();
            lastConsumptionFile = savedFileData.getFile().getAbsolutePath();
            System.out.printf("Consumption file saved to: %s%n", lastConsumptionFile);
            try {
                final var consumptionData = getFingridUsageData(lastConsumptionFile);
                final var isStartProductionAfter = startProduction != null && startProduction.isAfter(consumptionData.start);
                final var isEndProductionBefore = endProduction != null && endProduction.isBefore(consumptionData.end);
                fromDateTimePicker.setMin(isStartProductionAfter ? startProduction : consumptionData.start);
                fromDateTimePicker.setMax(isEndProductionBefore ? endConsumption.minusHours(1) : consumptionData.end.minusHours(1));
                fromDateTimePicker.setValue(isStartProductionAfter ? startProduction : consumptionData.start);
                toDateTimePicker.setMin(isStartProductionAfter ? startProduction.plusHours(1) : consumptionData.start.plusHours(1));
                toDateTimePicker.setMax(isEndProductionBefore ? endConsumption : consumptionData.end);
                toDateTimePicker.setValue(isEndProductionBefore ? endConsumption : consumptionData.end);
                startConsumption = consumptionData.start;
                endConsumption = consumptionData.end;
                updateCalculateButtonState();
                setFieldsEnabled(true);
            } catch (IOException | ParseException e) {
                throw new RuntimeException(e);
            }
        });
        consumptionUpload.addFailedListener(e -> setEnabled(false, fixedPriceField, spotMarginField, spotProductionMarginField, fromDateTimePicker, toDateTimePicker, button));
    }

    private void addProductionSucceededListener(FileBuffer fileBuffer, Upload productionUpload) {
        productionUpload.addSucceededListener(event -> {
            FileData savedFileData = fileBuffer.getFileData();
            lastProductionFile = savedFileData.getFile().getAbsolutePath();
            System.out.printf("Production file saved to: %s%n", lastProductionFile);
            try {
                final var productionData = getFingridUsageData(lastProductionFile);
                final var isStartConsumptionAfter = startConsumption != null && startConsumption.isAfter(productionData.start);
                final var isEndConsumptionBefore = endConsumption != null && endConsumption.isBefore(productionData.end);
                fromDateTimePicker.setMin(isStartConsumptionAfter ? startConsumption : productionData.start);
                fromDateTimePicker.setMax(isEndConsumptionBefore ? endConsumption.minusHours(1) : productionData.end.minusHours(1));
                fromDateTimePicker.setValue(isStartConsumptionAfter ? startConsumption : productionData.start);
                toDateTimePicker.setMin(isStartConsumptionAfter ? startConsumption.plusHours(1) : productionData.start.plusHours(1));
                toDateTimePicker.setMax(isEndConsumptionBefore ? endConsumption : productionData.end);
                toDateTimePicker.setValue(isEndConsumptionBefore ? endConsumption : productionData.end);
                startProduction = productionData.start;
                endProduction = productionData.end;
                updateCalculateButtonState();
                setFieldsEnabled(true);
            } catch (IOException | ParseException e) {
                throw new RuntimeException(e);
            }
        });
        productionUpload.addFailedListener(e -> setEnabled(false, fixedPriceField, spotMarginField, spotProductionMarginField, fromDateTimePicker, toDateTimePicker, button));
    }

    private Chart createChart(PriceCalculatorService.SpotCalculation spotCalculation, boolean isCalculatingFixed, String title, String yAxisTitle, String spotTitle) {
        var chart = new Chart(ChartType.COLUMN);
        chart.getConfiguration().setTitle(title);
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
        //xLabel.setStep(2);
        final var categories = IntStream.range(0, 24).mapToObj(i -> i + ":00").toList();
        xAxis.setCategories(categories.toArray(String[]::new));
        chart.getConfiguration().addxAxis(xAxis);

        // First YAxis
        final var consumptionYAxis = new YAxis();
        var labelsConsumption = new Labels();
        labelsConsumption.setFormatter("return this.value +' kWh'");
        consumptionYAxis.setLabels(labelsConsumption);
        consumptionYAxis.setTitle(yAxisTitle);
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

        // Consumption series
        final var consumptionHoursSeries = new ListSeries(yAxisTitle);
        for (int i = 0; i < spotCalculation.consumptionHours.length; ++i) {
            consumptionHoursSeries.addData(spotCalculation.consumptionHours[i]);
        }
        final var consumptionPlotOptionsColumn = new PlotOptionsColumn();
        final var consumptionTooltipSpot = new SeriesTooltip();
        consumptionTooltipSpot.setValueDecimals(2);
        consumptionTooltipSpot.setValueSuffix("kWh");
        consumptionPlotOptionsColumn.setTooltip(consumptionTooltipSpot);
        consumptionHoursSeries.setPlotOptions(consumptionPlotOptionsColumn);
        chart.getConfiguration().addSeries(consumptionHoursSeries);
        consumptionHoursSeries.setyAxis(consumptionYAxis);

        // Spot cost series
        final var spotCostHoursSeries = new ListSeries(spotTitle);
        for (int i = 0; i < spotCalculation.costHours.length; ++i) {
            spotCostHoursSeries.addData(spotCalculation.costHours[i]);
        }
        final var spotCostHoursPlotOptionsColumn = new PlotOptionsColumn();
        final var spotCostHoursTooltipSpot = new SeriesTooltip();
        spotCostHoursTooltipSpot.setValueDecimals(2);
        spotCostHoursTooltipSpot.setValueSuffix("€");
        spotCostHoursPlotOptionsColumn.setTooltip(spotCostHoursTooltipSpot);
        spotCostHoursSeries.setPlotOptions(spotCostHoursPlotOptionsColumn);
        chart.getConfiguration().addSeries(spotCostHoursSeries);
        spotCostHoursSeries.setyAxis(costYAxis);

        // Spot average series
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
        chart.getConfiguration().addSeries(spotAverageSeries);
        spotAverageSeries.setyAxis(spotYAxis);

        // Fixed cost series
        if (isCalculatingFixed) {
            final var fixedPrice = fixedPriceField.getValue();
            final var fixedCostSeries = new ListSeries("Fixed cost");
            for (int i = 0; i < spotCalculation.consumptionHours.length; ++i) {
                fixedCostSeries.addData(spotCalculation.consumptionHours[i] * fixedPrice / 100);
            }
            final var fixedCostPlotOptionsColumn = new PlotOptionsColumn();
            final var fixedCostTooltipSpot = new SeriesTooltip();
            fixedCostTooltipSpot.setValueDecimals(2);
            fixedCostTooltipSpot.setValueSuffix("€");
            fixedCostPlotOptionsColumn.setTooltip(fixedCostTooltipSpot);
            fixedCostSeries.setPlotOptions(fixedCostPlotOptionsColumn);
            chart.getConfiguration().addSeries(fixedCostSeries);
            fixedCostSeries.setyAxis(costYAxis);
        }

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

        //final var image = new Image("images/instructions/Fingrid_1.png", "Fingrid instructions 1");
        //image.addClassNames(LumoUtility.MaxWidth.SCREEN_SMALL);
        //helpStepsLayout.add(image);

        helpStepsLayout.add(new Span(new Span("1) Login to "), new Anchor("https://www.fingrid.fi/en/electricity-market/datahub/sign-in-to-datahub-customer-portal/", "Fingrid Datahub.")));
        helpStepsLayout.add(new Span("2) Download your hourly consumption data csv file."));
        helpStepsLayout.add(new Span("3) Upload the file below."));
        helpStepsLayout.add(new Span("4) Select the date and time range for the calculation (the end of day is 23:00)."));
        helpStepsLayout.add(new Span("5) (optional) enter your spot price margin. Will become zero if empty is used."));
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

    private void setEnabled(boolean isEnabled, HasEnabled... hasEnableds) {
        Stream.of(hasEnableds).forEach(field -> field.setEnabled(isEnabled));
    }

    private void setFieldsEnabled(boolean isEnabled) {
        fields.forEach(field -> field.setEnabled(isEnabled));
    }

    private void updateCalculateButtonState() {
        final var isDateValid = fromDateTimePicker.getValue() != null && toDateTimePicker.getValue() != null && fromDateTimePicker.getValue().isBefore(toDateTimePicker.getValue()) && !fromDateTimePicker.isInvalid() && !toDateTimePicker.isInvalid();
        final var isCalculatingProductionValid = (isCalculatingProduction() && lastProductionFile != null) || !isCalculatingProduction();
        button.setEnabled(lastConsumptionFile != null && isCalculatingProductionValid && isDateValid);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        final var backButton = new Button("Back to electricity price graph");
        backButton.addClassNames(LumoUtility.Height.MEDIUM, LumoUtility.Margin.NONE, LumoUtility.Margin.Bottom.MEDIUM, LumoUtility.BorderColor.CONTRAST_10, LumoUtility.Border.ALL, LumoUtility.BorderRadius.NONE);
        backButton.addClickListener(e -> attachEvent.getUI().navigate(NordpoolspotView.class));
        addComponentAsFirst(backButton);
    }

    enum Calculations {
        SPOT("Spot price"),
        FIXED("Fixed price"),
        SPOT_PRODUCTION("Spot production price");

        private String name;

        Calculations(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

}
