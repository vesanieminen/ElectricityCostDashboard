package com.vesanieminen.froniusvisualizer.views;

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
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.FileBuffer;
import com.vaadin.flow.component.upload.receivers.FileData;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vesanieminen.froniusvisualizer.components.DoubleLabel;
import com.vesanieminen.froniusvisualizer.services.PriceCalculatorService;
import lombok.extern.slf4j.Slf4j;
import org.vaadin.miki.superfields.numbers.SuperDoubleField;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateFixedElectricityPrice;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateSpotElectricityPriceDetails;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.getFingridUsageData;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.spotDataEnd;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.spotDataStart;
import static com.vesanieminen.froniusvisualizer.util.Utils.fiLocale;
import static com.vesanieminen.froniusvisualizer.util.Utils.fiZoneID;
import static com.vesanieminen.froniusvisualizer.util.Utils.format;
import static com.vesanieminen.froniusvisualizer.util.Utils.getNumberFormat;
import static com.vesanieminen.froniusvisualizer.util.Utils.getNumberFormatMaxTwoDecimalsWithPlusPrefix;
import static com.vesanieminen.froniusvisualizer.views.MainLayout.URL_SUFFIX;

@PageTitle("Calculator" + URL_SUFFIX)
@Route(value = "laskuri", layout = MainLayout.class)
@RouteAlias(value = "hintalaskuri", layout = MainLayout.class)
@RouteAlias(value = "price-calculator", layout = MainLayout.class)
@Slf4j
public class PriceCalculatorView extends Main {

    private static int consumptionFilesUploaded = 0;
    private static int productionFilesUploaded = 0;

    private final DateTimePicker fromDateTimePicker;
    private final DateTimePicker toDateTimePicker;
    private final SuperDoubleField fixedPriceField;
    private final SuperDoubleField spotMarginField;
    private final SuperDoubleField spotProductionMarginField;
    private final List<HasEnabled> fields;
    private final Button button;
    private final CheckboxGroup<Calculations> calculationsCheckboxGroup;
    private String lastConsumptionFile;
    private String lastProductionFile;

    private LocalDateTime startConsumption;
    private LocalDateTime endConsumption;
    private LocalDateTime startProduction;
    private LocalDateTime endProduction;

    public PriceCalculatorView() {
        addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.Margin.Top.MEDIUM);
        final var wrapper = new Div();
        wrapper.addClassNames(LumoUtility.Margin.Horizontal.AUTO);
        wrapper.setWidthFull();
        wrapper.setMaxWidth(1024, Unit.PIXELS);
        add(wrapper);
        final var content = new Div();
        content.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.Padding.Horizontal.MEDIUM);
        wrapper.add(content);

        final NumberFormat numberFormat = getNumberFormat(getLocale(), 2);

        final var title = new Span(getTranslation("calculator.title"));
        title.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.MEDIUM);
        content.add(title);
        final var spotAverage = PriceCalculatorService.calculateSpotAveragePriceThisYear();
        final var span = new Span(getTranslation("Spot average this year") + ": " + numberFormat.format(spotAverage) + " " + getTranslation("c/kWh"));
        span.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
        content.add(span);
        final var spotAverageMonth = PriceCalculatorService.calculateSpotAveragePriceThisMonth();
        final var spanMonth = new Span(getTranslation("Spot average this month") + ": " + numberFormat.format(spotAverageMonth) + " " + getTranslation("c/kWh"));
        spanMonth.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
        content.add(spanMonth);

        final var timeRangeSpanCaption = new Span(getTranslation("calculator.spot.prices.available") + ":");
        timeRangeSpanCaption.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
        final var timeRangeSpan = new Span(format(spotDataStart, getLocale()) + " - " + format(spotDataEnd, getLocale()));
        timeRangeSpan.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
        final var spotDataDiv = new Div(timeRangeSpanCaption, timeRangeSpan);
        spotDataDiv.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexWrap.WRAP, LumoUtility.Gap.Column.XSMALL);
        content.add(spotDataDiv);

        calculationsCheckboxGroup = new CheckboxGroup<>(getTranslation("Select calculations"));
        calculationsCheckboxGroup.addThemeVariants(CheckboxGroupVariant.LUMO_VERTICAL);
        calculationsCheckboxGroup.setItems(Calculations.values());
        calculationsCheckboxGroup.setItemLabelGenerator(item -> getTranslation(item.getName()));
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
        final var uploadFingridConsumptionData = new Button(getTranslation("Consumption csv file upload (1MB max)"));
        Upload consumptionUpload = new Upload(consumptionFileBuffer);
        consumptionUpload.setMaxFileSize(1000000);
        consumptionUpload.setDropLabel(new Span(getTranslation("Drop Fingrid consumption file here")));
        consumptionUpload.setUploadButton(uploadFingridConsumptionData);
        consumptionUpload.setDropAllowed(true);
        consumptionUpload.addClassNames(LumoUtility.Margin.Top.XSMALL);
        content.add(consumptionUpload);

        // Consumption file
        FileBuffer productionFileBuffer = new FileBuffer();
        final var uploadFingridproductionData = new Button(getTranslation("Production csv file upload (1MB max)"));
        Upload productionUpload = new Upload(productionFileBuffer);
        productionUpload.setMaxFileSize(1000000);
        productionUpload.setDropLabel(new Span(getTranslation("Drop Fingrid production file here")));
        productionUpload.setUploadButton(uploadFingridproductionData);
        productionUpload.setDropAllowed(true);
        productionUpload.addClassNames(LumoUtility.Margin.Top.XSMALL);
        content.add(productionUpload);
        productionUpload.setVisible(false);

        fromDateTimePicker = new DateTimePicker(getTranslation("Start period"));
        fromDateTimePicker.setWeekNumbersVisible(true);
        fromDateTimePicker.setDatePickerI18n(new DatePicker.DatePickerI18n().setFirstDayOfWeek(1));
        fromDateTimePicker.setRequiredIndicatorVisible(true);
        fromDateTimePicker.setLocale(getLocale());
        toDateTimePicker = new DateTimePicker(getTranslation("End period"));
        toDateTimePicker.setWeekNumbersVisible(true);
        toDateTimePicker.setDatePickerI18n(new DatePicker.DatePickerI18n().setFirstDayOfWeek(1));
        toDateTimePicker.setRequiredIndicatorVisible(true);
        toDateTimePicker.setLocale(getLocale());
        content.add(fromDateTimePicker);
        content.add(toDateTimePicker);

        if (fiLocale.equals(getLocale())) {
            var finnishI18n = new DatePicker.DatePickerI18n();
            finnishI18n.setMonthNames(List.of("Tammikuu", "Helmikuu", "Maaliskuu", "Huhtikuu", "Toukokuu", "Kesäkuu", "Heinäkuu", "Elokuu", "Syyskuu", "Lokakuu", "Marraskuu", "Joulukuu"));
            finnishI18n.setWeekdays(List.of("Sunnuntai", "Maanantai", "Tiistai", "Keskiviikko", "Torstai", "Perjantai", "Lauantai"));
            finnishI18n.setWeekdaysShort(
                    List.of("Su", "Ma", "Ti", "Ke", "To", "Pe", "La"));
            finnishI18n.setWeek("Viikko");
            finnishI18n.setToday("Tänään");
            finnishI18n.setCancel("Keskeytä");
            finnishI18n.setFirstDayOfWeek(1);
            fromDateTimePicker.setDatePickerI18n(finnishI18n);
            toDateTimePicker.setDatePickerI18n(finnishI18n);
        }

        final var fieldRow = new Div();
        fieldRow.addClassNames(LumoUtility.Display.FLEX, LumoUtility.Gap.Column.MEDIUM, LumoUtility.FlexWrap.WRAP);
        content.add(fieldRow);

        // Fixed price field
        fixedPriceField = new SuperDoubleField(getTranslation("Fixed price"));
        fixedPriceField.setLocale(getLocale());
        fixedPriceField.setHelperText(getTranslation("for.example") + " " + numberFormat.format(12.68));
        fixedPriceField.setRequiredIndicatorVisible(true);
        fixedPriceField.setSuffixComponent(new Span(getTranslation("c/kWh")));
        fixedPriceField.addClassNames(LumoUtility.Flex.GROW);
        fixedPriceField.setVisible(false);
        fieldRow.add(fixedPriceField);

        // Spot price field
        spotMarginField = new SuperDoubleField(getTranslation("Spot margin"));
        spotMarginField.setLocale(getLocale());
        spotMarginField.setHelperText(getTranslation("for.example") + " " + numberFormat.format(0.38));
        spotMarginField.setRequiredIndicatorVisible(true);
        spotMarginField.setSuffixComponent(new Span(getTranslation("c/kWh")));
        spotMarginField.addClassNames(LumoUtility.Flex.GROW);
        fieldRow.add(spotMarginField);

        // Spot price field
        spotProductionMarginField = new SuperDoubleField(getTranslation("Production margin"));
        spotProductionMarginField.setLocale(getLocale());
        spotProductionMarginField.setHelperText(getTranslation("for.example") + " " + numberFormat.format(0.3));
        spotProductionMarginField.setRequiredIndicatorVisible(true);
        spotProductionMarginField.setSuffixComponent(new Span(getTranslation("c/kWh")));
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

        button = new Button(getTranslation("Calculate costs"), e -> {
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
                final var spotCalculation = calculateSpotElectricityPriceDetails(consumptionData.data(), spotMarginField.getValue(), 1.24, fromDateTimePicker.getValue().atZone(fiZoneID).toInstant(), toDateTimePicker.getValue().atZone(fiZoneID).toInstant());
                resultLayout.removeAll();
                chartLayout.removeAll();

                final var start = format(spotCalculation.start, getLocale());
                final var end = format(spotCalculation.end, getLocale());

                final NumberFormat sixDecimals = getNumberFormat(getLocale(), 6);
                final var twoDecimalsWithPlusPrefix = getNumberFormatMaxTwoDecimalsWithPlusPrefix(getLocale());

                // Total labels
                resultLayout.add(new DoubleLabel(getTranslation("Calculation period (start times)"), start + " - " + end, true));
                resultLayout.add(new DoubleLabel(getTranslation("Total consumption over period"), numberFormat.format(spotCalculation.totalConsumption) + "kWh", true));

                // Spot labels
                final var totalCost = new BigDecimal(spotCalculation.totalCost).setScale(2, RoundingMode.HALF_UP).doubleValue();
                // Accurate:
                final var weightedAverage = totalCost / spotCalculation.totalConsumption * 100;
                // Helen calculates spot average like this:
                //final var weightedAverage = totalCost / ((int) spotCalculation.totalAmount) * 100;
                resultLayout.add(new DoubleLabel(getTranslation("Average spot price (incl. margin)"), sixDecimals.format(weightedAverage) + " c/kWh", true));
                resultLayout.add(new DoubleLabel(getTranslation("Total spot cost (incl. margin)"), numberFormat.format(spotCalculation.totalCost) + "€", true));
                resultLayout.add(new DoubleLabel(getTranslation("Total spot cost (without margin)"), numberFormat.format(spotCalculation.totalCostWithoutMargin) + "€", true));
                resultLayout.add(new DoubleLabel(getTranslation("Unweighted spot average"), numberFormat.format(spotCalculation.averagePriceWithoutMargin) + " c/kWh", true));
                final var loweredCost = (weightedAverage - spotCalculation.averagePriceWithoutMargin) / spotCalculation.averagePriceWithoutMargin * 100;
                final var formattedOwnSpotVsAverage = twoDecimalsWithPlusPrefix.format(loweredCost);
                resultLayout.add(new DoubleLabel(getTranslation("calculator.spot.difference.percentage"), formattedOwnSpotVsAverage + "%", true));
                final var costEffect = (spotCalculation.totalCost * 100 - spotCalculation.averagePriceWithoutMargin * spotCalculation.totalConsumption) / spotCalculation.totalConsumption;
                final var costEffectFormatted = twoDecimalsWithPlusPrefix.format(costEffect);
                resultLayout.add(new DoubleLabel(getTranslation("calculator.spot.difference.cents"), costEffectFormatted + " c/kWh", true));

                var fixedCost = 0d;
                if (isCalculatingFixed()) {
                    resultLayout.add(new DoubleLabel(getTranslation("Fixed price"), fixedPriceField.getValue() + " c/kWh", true));
                    fixedCost = calculateFixedElectricityPrice(consumptionData.data(), fixedPriceField.getValue(), fromDateTimePicker.getValue().atZone(fiZoneID).toInstant(), toDateTimePicker.getValue().atZone(fiZoneID).toInstant());
                    resultLayout.add(new DoubleLabel(getTranslation("Fixed cost total"), numberFormat.format(fixedCost) + "€", true));
                }

                // Create spot consumption chart
                chartLayout.add(createChart(spotCalculation, isCalculatingFixed(), getTranslation("Consumption / cost per hour"), getTranslation("Consumption"), getTranslation("Spot cost")));

                if (isCalculatingProduction()) {
                    final var productionData = getFingridUsageData(lastProductionFile);
                    final var spotProductionCalculation = calculateSpotElectricityPriceDetails(productionData.data(), -spotProductionMarginField.getValue(), 1, fromDateTimePicker.getValue().atZone(fiZoneID).toInstant(), toDateTimePicker.getValue().atZone(fiZoneID).toInstant());
                    resultLayout.add(new DoubleLabel(getTranslation("Total production over period"), numberFormat.format(spotProductionCalculation.totalConsumption) + "kWh", true));
                    resultLayout.add(new DoubleLabel(getTranslation("Net spot cost (consumption - production)"), numberFormat.format(spotCalculation.totalCost - spotProductionCalculation.totalCost) + "€", true));
                    resultLayout.add(new DoubleLabel(getTranslation("Net usage (consumption - production)"), numberFormat.format(spotCalculation.totalConsumption - spotProductionCalculation.totalConsumption) + "kWh", true));
                    resultLayout.add(new DoubleLabel(getTranslation("Average production price (incl. margin)"), numberFormat.format(spotProductionCalculation.totalCost / spotProductionCalculation.totalConsumption * 100) + getTranslation("c/kWh"), true));
                    resultLayout.add(new DoubleLabel(getTranslation("Total production value (incl. margin)"), numberFormat.format(spotProductionCalculation.totalCost) + "€", true));
                    resultLayout.add(new DoubleLabel(getTranslation("Total production value (without margin)"), numberFormat.format(spotProductionCalculation.totalCostWithoutMargin) + "€", true));
                    // Create spot production chart
                    chartLayout.add(createChart(spotProductionCalculation, false, getTranslation("Production / value per hour"), "Production", "Production value"));
                }

            } catch (IOException | ParseException ex) {
                throw new RuntimeException(ex);
            }
        });
        button.addClassNames(LumoUtility.Margin.Top.MEDIUM);

        addConsumptionSucceededListener(consumptionFileBuffer, consumptionUpload);
        addProductionSucceededListener(productionFileBuffer, productionUpload);
        addErrorHandling(consumptionUpload);
        addErrorHandling(productionUpload);

        fixedPriceField.addValueChangeListener(e -> updateCalculateButtonState());
        spotMarginField.addValueChangeListener(e -> updateCalculateButtonState());
        fromDateTimePicker.addValueChangeListener(e -> updateCalculateButtonState());
        toDateTimePicker.addValueChangeListener(e -> updateCalculateButtonState());
        setFieldsEnabled(false);
        button.setEnabled(false);
        content.add(button);
        add(resultLayout);
        add(chartLayout);
    }

    private void addErrorHandling(Upload upload) {
        upload.addFileRejectedListener(e -> {
            Notification.show(getTranslation("File was rejected") + ": " + e.getErrorMessage());
        });
        upload.addFailedListener(e -> {
            Notification.show(getTranslation("Upload failed") + ": " + e.getReason());
        });
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
            log.info("Consumption files uploaded: " + ++consumptionFilesUploaded);
            try {
                final var consumptionData = getFingridUsageData(lastConsumptionFile);
                final var consumptionDataStart = consumptionData.start().atZone(fiZoneID).toLocalDateTime();
                final var consumptionDataEnd = consumptionData.end().atZone(fiZoneID).toLocalDateTime();
                final var isStartProductionAfter = startProduction != null && startProduction.isAfter(consumptionDataStart);
                final var isEndProductionBefore = endProduction != null && endProduction.isBefore(consumptionDataEnd);
                fromDateTimePicker.setMin(isStartProductionAfter ? startProduction : consumptionDataStart);
                fromDateTimePicker.setMax(isEndProductionBefore ? endConsumption.minusHours(1) : consumptionDataEnd.minusHours(1));
                fromDateTimePicker.setValue(isStartProductionAfter ? startProduction : consumptionDataStart);
                toDateTimePicker.setMin(isStartProductionAfter ? startProduction.plusHours(1) : consumptionDataStart.plusHours(1));
                toDateTimePicker.setMax(isEndProductionBefore ? endConsumption : consumptionDataEnd);
                toDateTimePicker.setValue(isEndProductionBefore ? endConsumption : consumptionDataEnd);
                startConsumption = consumptionDataStart;
                endConsumption = consumptionDataEnd;
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
            log.info("Production files uploaded: " + ++productionFilesUploaded);
            try {
                final var productionData = getFingridUsageData(lastProductionFile);
                final var productionDataStart = productionData.start().atZone(fiZoneID).toLocalDateTime();
                final var productionDataEnd = productionData.end().atZone(fiZoneID).toLocalDateTime();
                final var isStartConsumptionAfter = startConsumption != null && startConsumption.isAfter(productionDataStart);
                final var isEndConsumptionBefore = endConsumption != null && endConsumption.isBefore(productionDataEnd);
                fromDateTimePicker.setMin(isStartConsumptionAfter ? startConsumption : productionDataStart);
                fromDateTimePicker.setMax(isEndConsumptionBefore ? endConsumption.minusHours(1) : productionDataEnd.minusHours(1));
                fromDateTimePicker.setValue(isStartConsumptionAfter ? startConsumption : productionDataStart);
                toDateTimePicker.setMin(isStartConsumptionAfter ? startConsumption.plusHours(1) : productionDataStart.plusHours(1));
                toDateTimePicker.setMax(isEndConsumptionBefore ? endConsumption : productionDataEnd);
                toDateTimePicker.setValue(isEndConsumptionBefore ? endConsumption : productionDataEnd);
                startProduction = productionDataStart;
                endProduction = productionDataEnd;
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
        costYAxis.setTitle(getTranslation("Price"));
        costYAxis.setOpposite(true);
        chart.getConfiguration().addyAxis(costYAxis);

        // third YAxis
        final var spotYAxis = new YAxis();
        spotYAxis.setVisible(false);
        var spotLabels = new Labels();
        spotLabels.setFormatter("return this.value +'c/kWh'");
        spotYAxis.setLabels(spotLabels);
        spotYAxis.setTitle(getTranslation("Spot"));
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

        // Weighted spot average series
        final var spotAverageSeries = new ListSeries(getTranslation("Spot average (incl. margin)"));
        for (int i = 0; i < spotCalculation.spotAverage.length; ++i) {
            final var consumptionHour = spotCalculation.consumptionHours[i];
            final var costHours = spotCalculation.costHours[i];
            spotAverageSeries.addData(costHours / consumptionHour * 100);
        }
        final var spotAveragePlotOptionsColumn = new PlotOptionsLine();
        spotAveragePlotOptionsColumn.setMarker(new Marker(false));
        spotAveragePlotOptionsColumn.setColorIndex(4);
        final var spotAverageTooltipSpot = new SeriesTooltip();
        spotAverageTooltipSpot.setValueDecimals(2);
        spotAverageTooltipSpot.setValueSuffix("c/kWh");
        spotAveragePlotOptionsColumn.setTooltip(spotAverageTooltipSpot);
        spotAverageSeries.setPlotOptions(spotAveragePlotOptionsColumn);
        chart.getConfiguration().addSeries(spotAverageSeries);
        spotAverageSeries.setyAxis(spotYAxis);

        // Unweighted spot average series
        final var unweightedSpotAverageSeries = new ListSeries(getTranslation("Unweighted spot average (incl. margin)"));
        for (int i = 0; i < spotCalculation.spotAverage.length; ++i) {
            unweightedSpotAverageSeries.addData(spotCalculation.spotAverage[i]);
        }
        final var unWeightedSpotAveragePlotOptionsColumn = new PlotOptionsLine();
        unWeightedSpotAveragePlotOptionsColumn.setMarker(new Marker(false));
        final var unweightedSpotAverageTooltipSpot = new SeriesTooltip();
        unweightedSpotAverageTooltipSpot.setValueDecimals(2);
        unweightedSpotAverageTooltipSpot.setValueSuffix(getTranslation("c/kWh"));
        unWeightedSpotAveragePlotOptionsColumn.setTooltip(unweightedSpotAverageTooltipSpot);
        unweightedSpotAverageSeries.setPlotOptions(unWeightedSpotAveragePlotOptionsColumn);
        chart.getConfiguration().addSeries(unweightedSpotAverageSeries);
        unweightedSpotAverageSeries.setyAxis(spotYAxis);


        // Fixed cost series
        if (isCalculatingFixed) {
            final var fixedPrice = fixedPriceField.getValue();
            final var fixedCostSeries = new ListSeries(getTranslation("Fixed cost"));
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

        // Average line
        var averagePriceSeries = new ListSeries(getTranslation("calculator.average.spot.during.period"));
        for (int i = 0; i < 24; ++i) {
            averagePriceSeries.addData(spotCalculation.averagePriceWithoutMargin);
        }
        final var averagePriceSeriesPlotOptionsColumn = new PlotOptionsLine();
        averagePriceSeriesPlotOptionsColumn.setMarker(new Marker(false));
        final var averagePriceSeriesTooltipSpot = new SeriesTooltip();
        averagePriceSeriesTooltipSpot.setValueDecimals(2);
        averagePriceSeriesTooltipSpot.setValueSuffix(getTranslation("c/kWh"));
        averagePriceSeriesPlotOptionsColumn.setTooltip(averagePriceSeriesTooltipSpot);
        averagePriceSeries.setPlotOptions(averagePriceSeriesPlotOptionsColumn);
        chart.getConfiguration().addSeries(averagePriceSeries);
        averagePriceSeries.setyAxis(spotYAxis);

        return chart;
    }

    private Div createHelpLayout(Div content) {
        final var helpButton = new Button(getTranslation("Click to show/hide help"));
        helpButton.addClassNames(LumoUtility.Margin.Top.SMALL, LumoUtility.Background.BASE);
        content.add(helpButton);
        final var helpLayout = new Div();
        helpLayout.setVisible(false);
        helpLayout.add(new Span(getTranslation("Usage:")));
        helpLayout.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);
        final var helpStepsLayout = new Div();
        helpStepsLayout.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.Margin.Left.LARGE);
        helpLayout.add(helpStepsLayout);
        content.add(helpLayout);
        helpStepsLayout.add(new Span(new Span(getTranslation("calculator.instructions.step1")), new Anchor("https://www.fingrid.fi/en/electricity-market/datahub/sign-in-to-datahub-customer-portal/", getTranslation("calculator.instructions.step1b"), AnchorTarget.BLANK)));
        final var fingridInstruction1 = new Image("images/instructions/Fingrid_1.png", "");
        fingridInstruction1.addClassNames(LumoUtility.Margin.Vertical.SMALL);
        fingridInstruction1.setMaxWidth("100%");
        fingridInstruction1.setMaxHeight("100%");
        helpStepsLayout.add(fingridInstruction1);
        helpStepsLayout.add(new Span(getTranslation("calculator.instructions.step2")));
        final var fingridInstruction2 = new Image("images/instructions/Fingrid_2.png", "");
        fingridInstruction2.addClassNames(LumoUtility.Margin.Vertical.SMALL);
        fingridInstruction2.setMaxWidth("100%");
        fingridInstruction2.setMaxHeight("100%");
        helpStepsLayout.add(fingridInstruction2);
        helpStepsLayout.add(new Span(getTranslation("calculator.instructions.step2b")));
        final var fingridInstruction3 = new Image("images/instructions/Fingrid_3.png", "");
        fingridInstruction3.addClassNames(LumoUtility.Margin.Vertical.SMALL);
        fingridInstruction3.setMaxWidth("100%");
        fingridInstruction3.setMaxHeight("100%");
        helpStepsLayout.add(fingridInstruction3);
        helpStepsLayout.add(new Span(getTranslation("calculator.instructions.step3")));
        helpStepsLayout.add(new Span(getTranslation("calculator.instructions.step4")));
        helpStepsLayout.add(new Span(getTranslation("calculator.instructions.step5")));
        helpStepsLayout.add(new Span(getTranslation("calculator.instructions.step6")));
        helpButton.addClickListener(e -> helpLayout.setVisible(!helpLayout.isVisible()));

        final var anchor = new Anchor("https://raw.githubusercontent.com/vesanieminen/ElectricityCostDashboard/main/src/main/resources/META-INF/resources/data/consumption.csv", getTranslation("Download example csv file here"));
        anchor.addClassNames(LumoUtility.Margin.Top.MEDIUM);
        helpLayout.add(anchor);
        //

        final var additionalInfo = new Span(getTranslation("calculator.help.notice"));
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
