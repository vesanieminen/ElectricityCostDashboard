package com.vesanieminen.froniusvisualizer.views;

import com.opencsv.exceptions.CsvValidationException;
import com.vaadin.flow.component.Component;
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
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
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
    private final SuperDoubleField transferAndTaxField;
    private final SuperDoubleField spotProductionMarginField;
    private final List<HasEnabled> fields;
    private final Button calculateButton;
    private final CheckboxGroup<Calculations> calculationsCheckboxGroup;
    private MemoryBuffer lastConsumptionData;
    private MemoryBuffer lastProductionData;

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

        //final var warning = new Span(getTranslation("price.calculator.warning"));
        //warning.addClassNames(LumoUtility.TextColor.SUCCESS);
        //content.add(warning);

        //final var newSpan = new Span(new Anchor("https://www.fingrid.fi/sahkomarkkinat/markkinoiden-yhtenaisyys/pohjoismainen-tasehallinta/varttitase/#taustaa", getTranslation("price.calculator.readmore"), AnchorTarget.BLANK));
        //content.add(newSpan);

        final var title = new Span(getTranslation("calculator.title"));
        title.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.MEDIUM);
        final var spotAverage = PriceCalculatorService.calculateSpotAveragePriceThisYear();
        final var span = new Span(getTranslation("Spot average this year") + ": " + numberFormat.format(spotAverage) + " " + getTranslation("c/kWh"));
        span.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
        final var spotAverageMonth = PriceCalculatorService.calculateSpotAveragePriceThisMonth();
        final var spanMonth = new Span(getTranslation("Spot average this month") + ": " + numberFormat.format(spotAverageMonth) + " " + getTranslation("c/kWh"));
        spanMonth.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

        final var timeRangeSpanCaption = new Span(getTranslation("calculator.spot.prices.available") + ":");
        timeRangeSpanCaption.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
        final var timeRangeSpan = new Span(format(spotDataStart, getLocale()) + " - " + format(spotDataEnd, getLocale()));
        timeRangeSpan.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
        final var spotDataDiv = new Div(timeRangeSpanCaption, timeRangeSpan);
        spotDataDiv.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexWrap.WRAP, LumoUtility.Gap.Column.XSMALL);

        final var div1 = new Div(title, span, spanMonth, spotDataDiv);
        div1.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);

        final var priimaImage = new Image("images/Priima 2-200x100 (banneri2).png", "PKS Priima");
        priimaImage.addClassNames(LumoUtility.Margin.Vertical.SMALL, LumoUtility.BorderRadius.MEDIUM, LumoUtility.Border.ALL, LumoUtility.BoxShadow.MEDIUM);
        priimaImage.setMaxWidth("100%");
        priimaImage.setWidth("200px");
        priimaImage.getStyle().set("border-color", "#FFFFFF");
        final var recommendationSpan = new Span(getTranslation("Electricity contract recommendation"));
        recommendationSpan.addClassNames(LumoUtility.FlexDirection.COLUMN, LumoUtility.Display.FLEX, LumoUtility.AlignItems.CENTER);
        final var priimaAnchor = new Anchor("https://www.pks.fi/sahkotarjoukset/kotiin/sahkotuotteet/priima-alykkaampi-sahko/", recommendationSpan, priimaImage);
        priimaAnchor.setTarget(AnchorTarget.BLANK);

        final var topRowDiv = new Div(div1, priimaAnchor);
        topRowDiv.addClassNames(LumoUtility.Display.FLEX, LumoUtility.JustifyContent.BETWEEN, LumoUtility.Gap.MEDIUM);
        content.add(topRowDiv);

        calculationsCheckboxGroup = new CheckboxGroup<>(getTranslation("Select calculations"));
        calculationsCheckboxGroup.addThemeVariants(CheckboxGroupVariant.LUMO_VERTICAL);
        calculationsCheckboxGroup.setItems(Calculations.values());
        calculationsCheckboxGroup.setItemLabelGenerator(item -> getTranslation(item.getName()));
        calculationsCheckboxGroup.setItemEnabledProvider(item -> !(Objects.equals(item.getName(), Calculations.SPOT.getName())));
        final var calculations = new HashSet<Calculations>();
        calculations.add(Calculations.SPOT);
        calculationsCheckboxGroup.setValue(calculations);
        final var image = new Image("images/fingrid_dh_white.png", getTranslation("login.to.fingrid"));
        final var openDatahub = new Span(getTranslation("open.datahub"));
        openDatahub.addClassNames(LumoUtility.FlexDirection.COLUMN, LumoUtility.Display.FLEX, LumoUtility.AlignItems.CENTER);
        final var datahubAnchor = new Anchor("https://oma.datahub.fi", openDatahub, image);
        datahubAnchor.setTarget(AnchorTarget.BLANK);
        image.addClassNames(LumoUtility.Margin.Vertical.SMALL, LumoUtility.BorderRadius.MEDIUM, LumoUtility.Border.ALL, LumoUtility.BoxShadow.MEDIUM);
        image.getStyle().set("background-color", "#d4121e");
        image.getStyle().set("border-color", "#d4121e");
        image.setMaxWidth("100%");
        image.setWidth("200px");
        final var checkBoxesAndDatahubLink = new Div(calculationsCheckboxGroup, datahubAnchor);
        checkBoxesAndDatahubLink.addClassNames(LumoUtility.Display.FLEX, LumoUtility.AlignItems.CENTER, LumoUtility.JustifyContent.BETWEEN);
        content.add(checkBoxesAndDatahubLink);

        // Layouts
        createHelpLayout(content);
        final var resultLayout = new Div();
        resultLayout.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.Margin.MEDIUM);
        final var chartLayout = new Div();

        // Consumption file
        final var consumptionFileBuffer = new MemoryBuffer();
        final var uploadFingridConsumptionData = new Button(getTranslation("Consumption csv file upload limit"));
        Upload consumptionUpload = new Upload(consumptionFileBuffer);
        consumptionUpload.setMaxFileSize(5000000);
        consumptionUpload.setDropLabel(new Span(getTranslation("Drop Fingrid consumption file here")));
        consumptionUpload.setUploadButton(uploadFingridConsumptionData);
        consumptionUpload.setDropAllowed(true);
        consumptionUpload.addClassNames(LumoUtility.Margin.Top.XSMALL);
        content.add(consumptionUpload);

        // Consumption file
        final var productionFileBuffer = new MemoryBuffer();
        final var uploadFingridproductionData = new Button(getTranslation("Production csv file upload limit"));
        Upload productionUpload = new Upload(productionFileBuffer);
        productionUpload.setMaxFileSize(5000000);
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
            //finnishI18n.setWeek("Viikko");
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
        fixedPriceField.setHelperText(getTranslation("for.example") + " " + numberFormat.format(5.41));
        fixedPriceField.setRequiredIndicatorVisible(true);
        fixedPriceField.setSuffixComponent(new Span(getTranslation("c/kWh")));
        fixedPriceField.addClassNames(LumoUtility.Flex.GROW);
        fixedPriceField.setVisible(false);
        fieldRow.add(fixedPriceField);

        // Spot margin field
        spotMarginField = new SuperDoubleField(getTranslation("Spot margin"));
        spotMarginField.setLocale(getLocale());
        spotMarginField.setHelperText(getTranslation("for.example") + " " + numberFormat.format(0.30) + " " + getTranslation("calculator.with.helen"));
        spotMarginField.setRequiredIndicatorVisible(true);
        spotMarginField.setSuffixComponent(new Span(getTranslation("c/kWh")));
        spotMarginField.addClassNames(LumoUtility.Flex.GROW);
        fieldRow.add(spotMarginField);

        // Spot production field
        spotProductionMarginField = new SuperDoubleField(getTranslation("Production margin"));
        spotProductionMarginField.setLocale(getLocale());
        spotProductionMarginField.setHelperText(getTranslation("for.example") + " " + numberFormat.format(0.3));
        spotProductionMarginField.setRequiredIndicatorVisible(true);
        spotProductionMarginField.setSuffixComponent(new Span(getTranslation("c/kWh")));
        spotProductionMarginField.addClassNames(LumoUtility.Flex.GROW);
        spotProductionMarginField.setVisible(false);
        fieldRow.add(spotProductionMarginField);

        // transfer and tax price field
        transferAndTaxField = new SuperDoubleField(getTranslation("calculator.transfer.and.tax"));
        transferAndTaxField.setMaximumFractionDigits(6);
        transferAndTaxField.setLocale(getLocale());
        transferAndTaxField.setHelperText(getTranslation("for.example") + " " + numberFormat.format(7.86) + " " + getTranslation("calculator.with.caruna"));
        transferAndTaxField.setRequiredIndicatorVisible(true);
        transferAndTaxField.setSuffixComponent(new Span(getTranslation("c/kWh")));
        transferAndTaxField.addClassNames(LumoUtility.Flex.GROW);
        transferAndTaxField.setVisible(false);
        fieldRow.add(transferAndTaxField);

        calculationsCheckboxGroup.addValueChangeListener(e -> {
            fixedPriceField.setVisible(e.getValue().contains(Calculations.FIXED));
            spotProductionMarginField.setVisible(e.getValue().contains(Calculations.SPOT_PRODUCTION));
            transferAndTaxField.setVisible(e.getValue().contains(Calculations.TRANSFER_AND_TAX));
            productionUpload.setVisible(e.getValue().contains(Calculations.SPOT_PRODUCTION));
            updateCalculateButtonState();
        });
        fields = Arrays.asList(fromDateTimePicker, toDateTimePicker, fixedPriceField, spotMarginField, transferAndTaxField, spotProductionMarginField);

        calculateButton = new Button(getTranslation("Calculate costs"), e -> {
            try {
                if (spotMarginField.getValue() == null) {
                    spotMarginField.setValue(0d);
                }
                if (isCalculatingFixed()) {
                    if (fixedPriceField.getValue() == null) {
                        fixedPriceField.setValue(0d);
                    }
                }
                if (isCalculatingTransferAndTax()) {
                    if (transferAndTaxField.getValue() == null) {
                        transferAndTaxField.setValue(0d);
                    }
                }
                if (isCalculatingProduction()) {
                    if (spotProductionMarginField.getValue() == null) {
                        spotProductionMarginField.setValue(0d);
                    }
                }
                final var consumptionData = getFingridUsageData(lastConsumptionData);
                final var spotCalculation = calculateSpotElectricityPriceDetails(consumptionData.data(), spotMarginField.getValue(), 1.24, fromDateTimePicker.getValue().atZone(fiZoneID).toInstant(), toDateTimePicker.getValue().atZone(fiZoneID).toInstant());
                resultLayout.removeAll();
                chartLayout.removeAll();

                final var start = format(spotCalculation.start, getLocale());
                final var end = format(spotCalculation.end, getLocale());

                final NumberFormat sixDecimals = getNumberFormat(getLocale(), 6);
                final var twoDecimalsWithPlusPrefix = getNumberFormatMaxTwoDecimalsWithPlusPrefix(getLocale());

                final Div overviewDiv = addSection(resultLayout, getTranslation("Spot price"));
                // Total labels
                overviewDiv.add(new DoubleLabel(getTranslation("Calculation period (start times)"), start + " - " + end, true));
                overviewDiv.add(new DoubleLabel(getTranslation("Total consumption over period"), numberFormat.format(spotCalculation.totalConsumption) + " kWh", true));

                // Spot labels
                //final var totalCost = new BigDecimal(spotCalculation.totalCost).doubleValue();
                //final var totalCostWithoutMargin = new BigDecimal(spotCalculation.totalCostWithoutMargin).doubleValue();
                // electricity companies calculate the euros with max 2 decimals:
                final var totalCost = new BigDecimal(spotCalculation.totalCost).setScale(2, RoundingMode.HALF_UP).doubleValue();
                // the following method provides rounding errors between the total cost with margin and without.
                //final var totalCostWithoutMargin = new BigDecimal(spotCalculation.totalCostWithoutMargin).setScale(2, RoundingMode.HALF_UP).doubleValue();
                // Accurate:
                final var weightedAverage = totalCost / spotCalculation.totalConsumption * 100;
                //final var weightedAverageWithoutMargin = totalCostWithoutMargin / spotCalculation.totalConsumption * 100;
                // Helen calculates spot average like this:
                //final var weightedAverage = totalCost / ((int) spotCalculation.totalAmount) * 100;
                overviewDiv.add(new DoubleLabel(getTranslation("Average spot price (incl. margin)"), sixDecimals.format(weightedAverage) + " " + getTranslation("c/kWh"), true));
                overviewDiv.add(new DoubleLabel(getTranslation("Average spot price (without margin)"), sixDecimals.format(weightedAverage - spotMarginField.getValue()) + " " + getTranslation("c/kWh"), true));
                overviewDiv.add(new DoubleLabel(getTranslation("Total spot cost (incl. margin)"), numberFormat.format(spotCalculation.totalCost) + " €", true));
                overviewDiv.add(new DoubleLabel(getTranslation("Total spot cost (without margin)"), numberFormat.format(spotCalculation.totalCostWithoutMargin) + " €", true));
                overviewDiv.add(new DoubleLabel(getTranslation("Unweighted spot average"), numberFormat.format(spotCalculation.averagePriceWithoutMargin) + " " + getTranslation("c/kWh"), true));
                final var loweredCost = (spotCalculation.totalCostWithoutMargin / spotCalculation.totalConsumption * 100 - spotCalculation.averagePriceWithoutMargin) / spotCalculation.averagePriceWithoutMargin * 100;
                final var formattedOwnSpotVsAverage = twoDecimalsWithPlusPrefix.format(loweredCost);
                overviewDiv.add(new DoubleLabel(getTranslation("calculator.spot.difference.percentage"), formattedOwnSpotVsAverage + " %", true));
                final var costEffect = (spotCalculation.totalCostWithoutMargin * 100 - spotCalculation.averagePriceWithoutMargin * spotCalculation.totalConsumption) / spotCalculation.totalConsumption;
                final var costEffectFormatted = twoDecimalsWithPlusPrefix.format(costEffect);
                overviewDiv.add(new DoubleLabel(getTranslation("calculator.spot.difference.cents"), costEffectFormatted + " " + getTranslation("c/kWh"), true));

                if (isCalculatingFixed()) {
                    final Div fixedPriceDiv = addSection(resultLayout, getTranslation("Fixed Price details"));
                    resultLayout.add(fixedPriceDiv);
                    fixedPriceDiv.add(new DoubleLabel(getTranslation("Fixed price"), fixedPriceField.getValue() + " " + getTranslation("c/kWh"), true));
                    var fixedCost = calculateFixedElectricityPrice(consumptionData.data(), fixedPriceField.getValue(), fromDateTimePicker.getValue().atZone(fiZoneID).toInstant(), toDateTimePicker.getValue().atZone(fiZoneID).toInstant());
                    fixedPriceDiv.add(new DoubleLabel(getTranslation("Fixed cost total"), numberFormat.format(fixedCost) + " €", true));
                }

                if (isCalculatingTransferAndTax()) {
                    final Div transferAndTaxDiv = addSection(resultLayout, getTranslation("Transfer and tax"));
                    transferAndTaxDiv.add(new DoubleLabel(getTranslation("calculator.transfer.and.tax"), transferAndTaxField.getValue() + " " + getTranslation("c/kWh"), true));
                    var transferAndTaxTotalCost = calculateFixedElectricityPrice(consumptionData.data(), transferAndTaxField.getValue(), fromDateTimePicker.getValue().atZone(fiZoneID).toInstant(), toDateTimePicker.getValue().atZone(fiZoneID).toInstant());
                    transferAndTaxDiv.add(new DoubleLabel(getTranslation("calculator.transfer.and.tax.total"), numberFormat.format(transferAndTaxTotalCost) + " €", true));
                    transferAndTaxDiv.add(new DoubleLabel(getTranslation("calculator.spot.cost.and.transfer"), numberFormat.format(spotCalculation.totalCost + transferAndTaxTotalCost) + " €", true));
                }

                // Create spot consumption chart
                chartLayout.add(createChart(spotCalculation, isCalculatingFixed(), getTranslation("Consumption / cost per hour"), getTranslation("Consumption"), getTranslation("Spot cost")));

                if (isCalculatingProduction()) {
                    final var productionData = getFingridUsageData(lastProductionData);
                    final var spotProductionCalculation = calculateSpotElectricityPriceDetails(productionData.data(), -spotProductionMarginField.getValue(), 1, fromDateTimePicker.getValue().atZone(fiZoneID).toInstant(), toDateTimePicker.getValue().atZone(fiZoneID).toInstant());
                    final Div productionDiv = addSection(resultLayout, getTranslation("Production"));
                    productionDiv.add(new DoubleLabel(getTranslation("Total production over period"), numberFormat.format(spotProductionCalculation.totalConsumption) + " kWh", true));
                    productionDiv.add(new DoubleLabel(getTranslation("Net spot cost (consumption - production)"), numberFormat.format(spotCalculation.totalCost - spotProductionCalculation.totalCost) + " €", true));
                    productionDiv.add(new DoubleLabel(getTranslation("Net usage (consumption - production)"), numberFormat.format(spotCalculation.totalConsumption - spotProductionCalculation.totalConsumption) + " kWh", true));
                    productionDiv.add(new DoubleLabel(getTranslation("Average production price (incl. margin)"), numberFormat.format(spotProductionCalculation.totalCost / spotProductionCalculation.totalConsumption * 100) + " " + getTranslation("c/kWh"), true));
                    productionDiv.add(new DoubleLabel(getTranslation("Total production value (incl. margin)"), numberFormat.format(spotProductionCalculation.totalCost) + " €", true));
                    productionDiv.add(new DoubleLabel(getTranslation("Total production value (without margin)"), numberFormat.format(spotProductionCalculation.totalCostWithoutMargin) + " €", true));
                    // Create spot production chart
                    chartLayout.add(createChart(spotProductionCalculation, false, getTranslation("Production / value per hour"), "Production", "Production value"));
                }

            } catch (IOException | ParseException | CsvValidationException ex) {
                throw new RuntimeException(ex);
            }
        });
        calculateButton.addClassNames(LumoUtility.Margin.Top.MEDIUM);

        addConsumptionSucceededListener(consumptionFileBuffer, consumptionUpload);
        addProductionSucceededListener(productionFileBuffer, productionUpload);
        addErrorHandling(consumptionUpload);
        addErrorHandling(productionUpload);

        fixedPriceField.addValueChangeListener(e -> updateCalculateButtonState());
        spotMarginField.addValueChangeListener(e -> updateCalculateButtonState());
        fromDateTimePicker.addValueChangeListener(e -> updateCalculateButtonState());
        toDateTimePicker.addValueChangeListener(e -> updateCalculateButtonState());
        setFieldsEnabled(false);
        calculateButton.setEnabled(false);
        content.add(calculateButton);
        add(resultLayout);
        add(chartLayout);
    }

    private Div createWrapDiv(Component... components) {
        final var wrapDiv = new Div(components);
        wrapDiv.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexWrap.WRAP, LumoUtility.Margin.Top.MEDIUM);
        return wrapDiv;
    }

    private Component createSectionHeader(String title) {
        final var header = new H2(title);
        header.addClassNames(LumoUtility.FontSize.MEDIUM, LumoUtility.Margin.NONE);
        return header;
    }

    private Div addSection(Div div, String title) {
        final var overviewDiv = createWrapDiv();
        final var overviewHeader = createSectionHeader(title);
        div.add(overviewHeader, overviewDiv);
        return overviewDiv;
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

    private boolean isCalculatingTransferAndTax() {
        return calculationsCheckboxGroup.getValue().contains(Calculations.TRANSFER_AND_TAX);
    }

    private boolean isCalculatingProduction() {
        return calculationsCheckboxGroup.getValue().contains(Calculations.SPOT_PRODUCTION);
    }

    private void addConsumptionSucceededListener(MemoryBuffer fileBuffer, Upload consumptionUpload) {
        consumptionUpload.addSucceededListener(event -> {
            lastConsumptionData = fileBuffer;
            log.info("Consumption data uploaded: " + ++consumptionFilesUploaded);
            try {
                final var consumptionData = getFingridUsageData(lastConsumptionData);
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
            } catch (IOException | ParseException | CsvValidationException e) {
                throw new RuntimeException(e);
            }
        });
        consumptionUpload.addFailedListener(e -> setEnabled(false, fixedPriceField, spotMarginField, transferAndTaxField, spotProductionMarginField, fromDateTimePicker, toDateTimePicker, calculateButton));
    }

    private void addProductionSucceededListener(MemoryBuffer fileBuffer, Upload productionUpload) {
        productionUpload.addSucceededListener(event -> {
            lastProductionData = fileBuffer;
            log.info("Production data uploaded: " + ++productionFilesUploaded);
            try {
                final var productionData = getFingridUsageData(lastProductionData);
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
            } catch (IOException | ParseException | CsvValidationException e) {
                throw new RuntimeException(e);
            }
        });
        productionUpload.addFailedListener(e -> setEnabled(false, fixedPriceField, spotMarginField, transferAndTaxField, spotProductionMarginField, fromDateTimePicker, toDateTimePicker, calculateButton));
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
        final var unweightedSpotAverageSeries = new ListSeries(getTranslation("Unweighted spot average by the hour"));
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
        unweightedSpotAverageSeries.setVisible(false);


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

    private void createHelpLayout(Div content) {
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
        helpStepsLayout.add(new Span(new Span(getTranslation("calculator.instructions.step1")), new Anchor("https://oma.datahub.fi", getTranslation("calculator.instructions.step1b"), AnchorTarget.BLANK)));
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

        final var additionalInfo = new Span(getTranslation("calculator.help.notice"));
        additionalInfo.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
        helpLayout.add(additionalInfo);

    }

    private void setEnabled(boolean isEnabled, HasEnabled... hasEnableds) {
        Stream.of(hasEnableds).forEach(field -> field.setEnabled(isEnabled));
    }

    private void setFieldsEnabled(boolean isEnabled) {
        fields.forEach(field -> field.setEnabled(isEnabled));
    }

    private void updateCalculateButtonState() {
        final var isDateValid = fromDateTimePicker.getValue() != null && toDateTimePicker.getValue() != null && fromDateTimePicker.getValue().isBefore(toDateTimePicker.getValue()) && !fromDateTimePicker.isInvalid() && !toDateTimePicker.isInvalid();
        final var isCalculatingProductionValid = (isCalculatingProduction() && lastProductionData != null) || !isCalculatingProduction();
        calculateButton.setEnabled(lastConsumptionData != null && isCalculatingProductionValid && isDateValid);
    }

    enum Calculations {
        SPOT("Spot price"),
        FIXED("Fixed price"),
        //COST_FACTOR("cost.factor"),
        TRANSFER_AND_TAX("calculator.transfer.and.tax"),
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
