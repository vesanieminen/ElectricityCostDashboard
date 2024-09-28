package com.vesanieminen.froniusvisualizer.views;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.exceptions.CsvValidationException;
import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasEnabled;
import com.vaadin.flow.component.HasValue;
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
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.AnchorTarget;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.page.WebStorage;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vesanieminen.froniusvisualizer.components.DoubleLabel;
import com.vesanieminen.froniusvisualizer.services.PriceCalculatorService;
import com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.FingridUsageData;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateDayConsumption;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateDayPrice;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateElectricityTaxPrice;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateFixedElectricityPrice;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateFixedElectricityPriceWithPastProductionReduced;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateNightConsumption;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateNightPrice;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateSeasonalOtherConsumption;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateSeasonalOtherPrice;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateSeasonalWinterConsumption;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateSeasonalWinterPrice;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateSpotElectricityPriceDetails;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.getFingridUsageData;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.spotDataEnd;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.spotDataStart;
import static com.vesanieminen.froniusvisualizer.util.Utils.calculateMonthsInvolved;
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
    private final SuperDoubleField generalTransferField;
    private final SuperDoubleField spotProductionMarginField;
    private final SuperDoubleField baasPriceField;
    private final List<HasEnabled> fields;
    private final Button calculateButton;
    private final CheckboxGroup<Calculations> calculationsCheckboxGroup;
    private final Div topRowDiv;
    private final SuperDoubleField nightTransferDayPriceField;
    private final SuperDoubleField nightTransferNightPriceField;
    private final SuperDoubleField nightTransferMonthlyPriceField;
    private final SuperDoubleField transferMonthlyPriceField;
    private final SuperDoubleField baasMonthlyPriceField;
    private final Select<TaxClass> taxClassSelect;
    private final SuperDoubleField lockedPriceField;
    private final SuperDoubleField seasonalTransferWinterPriceField;
    private final SuperDoubleField seasonalTransferOtherPriceField;
    private final SuperDoubleField seasonalTransferMonthlyPriceField;
    private MemoryBuffer lastConsumptionData;
    private MemoryBuffer lastProductionData;

    private LocalDateTime startConsumption;
    private LocalDateTime endConsumption;
    private LocalDateTime startProduction;
    private LocalDateTime endProduction;

    public PriceCalculatorView() {
        addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.Margin.Top.MEDIUM);

        final Div wrapper = createWrapper();

        final var content = new Div();
        content.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.Padding.Horizontal.MEDIUM);
        wrapper.add(content);

        final NumberFormat numberFormat = getNumberFormat(getLocale(), 2);

        final var title = new H2(getTranslation("calculator.title"));
        title.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.LARGE, LumoUtility.Margin.Bottom.MEDIUM);
        final var spotAverage = PriceCalculatorService.calculateSpotAveragePriceThisYear();
        final var spotAverageThisYear = new DoubleLabel(getTranslation("Spot average this year"), numberFormat.format(spotAverage) + " " + getTranslation("c/kWh"));
        spotAverageThisYear.setAlignLeft();

        final var spotAverageMonth = PriceCalculatorService.calculateSpotAveragePriceThisMonth();
        final var spotAverageThisMonth = new DoubleLabel(getTranslation("Spot average this month"), numberFormat.format(spotAverageMonth) + " " + getTranslation("c/kWh"));
        spotAverageThisMonth.setAlignLeft();
        final var spotDateRange = new DoubleLabel(getTranslation("calculator.spot.prices.available"), format(spotDataStart, getLocale()) + " - " + format(spotDataEnd, getLocale()));
        spotDateRange.setAlignLeft();

        final var topDiv = new Div(title, spotAverageThisYear, spotAverageThisMonth, spotDateRange);
        topDiv.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);

        topRowDiv = new Div(topDiv);
        topRowDiv.addClassNames(LumoUtility.Display.FLEX, LumoUtility.JustifyContent.BETWEEN, LumoUtility.Gap.MEDIUM);
        content.add(topRowDiv);
        addAd();

        calculationsCheckboxGroup = new CheckboxGroup<>(getTranslation("Select calculations"));
        calculationsCheckboxGroup.setId("calculationsCheckboxGroup");
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
        final var datahubAnchor = new Anchor("https://oma.datahub.fi", image, openDatahub);
        datahubAnchor.setTarget(AnchorTarget.BLANK);
        image.addClassNames(LumoUtility.Margin.Vertical.SMALL, LumoUtility.BorderRadius.MEDIUM, LumoUtility.Border.ALL, LumoUtility.BoxShadow.MEDIUM);
        image.getStyle().set("background-color", "#d4121e");
        image.getStyle().set("border-color", "#d4121e");
        image.setMaxWidth("100%");
        image.setWidth("200px");
        final var checkBoxesAndDatahubLink = new Div(calculationsCheckboxGroup, datahubAnchor);
        checkBoxesAndDatahubLink.addClassNames(LumoUtility.Display.FLEX, LumoUtility.AlignItems.CENTER, LumoUtility.JustifyContent.BETWEEN, LumoUtility.Margin.Top.MEDIUM);
        content.add(checkBoxesAndDatahubLink);

        // Layouts
        createHelpLayout(content);
        final var resultLayout = new Div();
        resultLayout.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.Margin.Vertical.MEDIUM, LumoUtility.Padding.MEDIUM);
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
        addConsumptionSucceededListener(consumptionFileBuffer, consumptionUpload);
        addErrorHandling(consumptionUpload);

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
        addProductionSucceededListener(productionFileBuffer, productionUpload);
        addErrorHandling(productionUpload);

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
        fixedPriceField = new SuperDoubleField(null, getTranslation("Fixed price"));
        fixedPriceField.setId("fixedPriceField");
        fixedPriceField.addValueChangeListener(item -> saveFieldValue(fixedPriceField));
        fixedPriceField.setLocale(getLocale());
        fixedPriceField.setHelperText(getTranslation("for.example") + " " + numberFormat.format(5.41));
        fixedPriceField.setRequiredIndicatorVisible(true);
        fixedPriceField.setSuffixComponent(new Span(getTranslation("c/kWh")));
        fixedPriceField.addClassNames(LumoUtility.Flex.GROW);
        fixedPriceField.setVisible(false);
        fieldRow.add(fixedPriceField);

        // Spot margin field
        spotMarginField = new SuperDoubleField(null, getTranslation("Spot margin"));
        spotMarginField.setId("spotMarginField");
        spotMarginField.addValueChangeListener(item -> saveFieldValue(spotMarginField));
        spotMarginField.setLocale(getLocale());
        spotMarginField.setHelperText(getTranslation("for.example") + " " + numberFormat.format(0.30) + " " + getTranslation("calculator.with.helen"));
        spotMarginField.setRequiredIndicatorVisible(true);
        spotMarginField.setSuffixComponent(new Span(getTranslation("c/kWh")));
        spotMarginField.addClassNames(LumoUtility.Flex.GROW);
        fieldRow.add(spotMarginField);

        // Spot production field
        spotProductionMarginField = new SuperDoubleField(null, getTranslation("Production margin"));
        spotProductionMarginField.setId("spotProductionMarginField");
        spotProductionMarginField.addValueChangeListener(item -> saveFieldValue(spotProductionMarginField));
        spotProductionMarginField.setLocale(getLocale());
        spotProductionMarginField.setHelperText(getTranslation("for.example") + " " + numberFormat.format(0.3));
        spotProductionMarginField.setRequiredIndicatorVisible(true);
        spotProductionMarginField.setSuffixComponent(new Span(getTranslation("c/kWh")));
        spotProductionMarginField.addClassNames(LumoUtility.Flex.GROW);
        spotProductionMarginField.setVisible(false);
        fieldRow.add(spotProductionMarginField);

        // general transfer field
        generalTransferField = new SuperDoubleField(null, getTranslation("calculator.general-transfer"));
        generalTransferField.setId("generalTransferField");
        generalTransferField.addValueChangeListener(item -> saveFieldValue(generalTransferField));
        generalTransferField.setMaximumFractionDigits(6);
        generalTransferField.setLocale(getLocale());
        generalTransferField.setHelperText(getTranslation("for.example") + " " + numberFormat.format(5.26) + " " + getTranslation("calculator.with.caruna"));
        generalTransferField.setRequiredIndicatorVisible(true);
        generalTransferField.setSuffixComponent(new Span(getTranslation("c/kWh")));
        generalTransferField.addClassNames(LumoUtility.Flex.GROW);
        transferMonthlyPriceField = new SuperDoubleField(null, getTranslation("calculator.general-transfer.monthly-price"));
        transferMonthlyPriceField.setId("transferMonthlyPriceField");
        transferMonthlyPriceField.addValueChangeListener(item -> saveFieldValue(transferMonthlyPriceField));
        transferMonthlyPriceField.setMaximumFractionDigits(6);
        transferMonthlyPriceField.setLocale(getLocale());
        transferMonthlyPriceField.setHelperText(getTranslation("calculator.general-transfer.monthly-price-helper"));
        transferMonthlyPriceField.setRequiredIndicatorVisible(true);
        transferMonthlyPriceField.setSuffixComponent(new Span("€"));
        transferMonthlyPriceField.addClassNames(LumoUtility.Flex.GROW);
        final var transferDiv = new Div(generalTransferField, transferMonthlyPriceField);
        transferDiv.setVisible(false);
        transferDiv.addClassNames(LumoUtility.Display.FLEX, LumoUtility.Gap.Column.MEDIUM, LumoUtility.FlexWrap.WRAP);
        content.add(transferDiv);

        // night transfer
        nightTransferDayPriceField = new SuperDoubleField(null, getTranslation("calculator.night-transfer.day-price"));
        nightTransferDayPriceField.setId("nightTransferDayPriceField");
        nightTransferDayPriceField.addValueChangeListener(item -> saveFieldValue(nightTransferDayPriceField));
        nightTransferDayPriceField.setMaximumFractionDigits(6);
        nightTransferDayPriceField.setLocale(getLocale());
        nightTransferDayPriceField.setHelperText(getTranslation("calculator.night-transfer.day-helper"));
        nightTransferDayPriceField.setRequiredIndicatorVisible(true);
        nightTransferDayPriceField.setSuffixComponent(new Span(getTranslation("c/kWh")));
        nightTransferDayPriceField.addClassNames(LumoUtility.Flex.GROW);
        nightTransferNightPriceField = new SuperDoubleField(null, getTranslation("calculator.night-transfer.night-price"));
        nightTransferNightPriceField.setId("nightTransferNightPriceField");
        nightTransferNightPriceField.addValueChangeListener(item -> saveFieldValue(nightTransferNightPriceField));
        nightTransferNightPriceField.setMaximumFractionDigits(6);
        nightTransferNightPriceField.setLocale(getLocale());
        nightTransferNightPriceField.setHelperText(getTranslation("calculator.night-transfer.night-helper"));
        nightTransferNightPriceField.setRequiredIndicatorVisible(true);
        nightTransferNightPriceField.setSuffixComponent(new Span(getTranslation("c/kWh")));
        nightTransferNightPriceField.addClassNames(LumoUtility.Flex.GROW);
        nightTransferMonthlyPriceField = new SuperDoubleField(null, getTranslation("calculator.night-transfer.monthly-price"));
        nightTransferMonthlyPriceField.setId("nightTransferMonthlyPriceField");
        nightTransferMonthlyPriceField.addValueChangeListener(item -> saveFieldValue(nightTransferMonthlyPriceField));
        nightTransferMonthlyPriceField.setMaximumFractionDigits(6);
        nightTransferMonthlyPriceField.setLocale(getLocale());
        nightTransferMonthlyPriceField.setHelperText(getTranslation("calculator.night-transfer.monthly-price-helper"));
        nightTransferMonthlyPriceField.setRequiredIndicatorVisible(true);
        nightTransferMonthlyPriceField.setSuffixComponent(new Span("€"));
        nightTransferMonthlyPriceField.addClassNames(LumoUtility.Flex.GROW);
        final var nightTransferDiv = new Div(nightTransferDayPriceField, nightTransferNightPriceField, nightTransferMonthlyPriceField);
        nightTransferDiv.setVisible(false);
        nightTransferDiv.addClassNames(LumoUtility.Display.FLEX, LumoUtility.Gap.Column.MEDIUM, LumoUtility.FlexWrap.WRAP);
        content.add(nightTransferDiv);

        // seasonal transfer
        seasonalTransferWinterPriceField = new SuperDoubleField(null, getTranslation("calculator.seasonal-transfer.winter-price"));
        seasonalTransferWinterPriceField.setId("seasonalTransferWinterPriceField");
        seasonalTransferWinterPriceField.addValueChangeListener(item -> saveFieldValue(seasonalTransferWinterPriceField));
        seasonalTransferWinterPriceField.setMaximumFractionDigits(6);
        seasonalTransferWinterPriceField.setLocale(getLocale());
        seasonalTransferWinterPriceField.setHelperText(getTranslation("calculator.seasonal-transfer.winter-helper"));
        seasonalTransferWinterPriceField.setRequiredIndicatorVisible(true);
        seasonalTransferWinterPriceField.setSuffixComponent(new Span(getTranslation("c/kWh")));
        seasonalTransferWinterPriceField.addClassNames(LumoUtility.Flex.GROW);
        seasonalTransferOtherPriceField = new SuperDoubleField(null, getTranslation("calculator.seasonal-transfer.other-price"));
        seasonalTransferOtherPriceField.setId("seasonalTransferNightPriceField");
        seasonalTransferOtherPriceField.addValueChangeListener(item -> saveFieldValue(seasonalTransferOtherPriceField));
        seasonalTransferOtherPriceField.setMaximumFractionDigits(6);
        seasonalTransferOtherPriceField.setLocale(getLocale());
        seasonalTransferOtherPriceField.setHelperText(getTranslation("calculator.seasonal-transfer.other-helper"));
        seasonalTransferOtherPriceField.setRequiredIndicatorVisible(true);
        seasonalTransferOtherPriceField.setSuffixComponent(new Span(getTranslation("c/kWh")));
        seasonalTransferOtherPriceField.addClassNames(LumoUtility.Flex.GROW);
        seasonalTransferMonthlyPriceField = new SuperDoubleField(null, getTranslation("calculator.seasonal-transfer.monthly-price"));
        seasonalTransferMonthlyPriceField.setId("seasonalTransferMonthlyPriceField");
        seasonalTransferMonthlyPriceField.addValueChangeListener(item -> saveFieldValue(seasonalTransferMonthlyPriceField));
        seasonalTransferMonthlyPriceField.setMaximumFractionDigits(6);
        seasonalTransferMonthlyPriceField.setLocale(getLocale());
        seasonalTransferMonthlyPriceField.setHelperText(getTranslation("calculator.seasonal-transfer.monthly-price-helper"));
        seasonalTransferMonthlyPriceField.setRequiredIndicatorVisible(true);
        seasonalTransferMonthlyPriceField.setSuffixComponent(new Span("€"));
        seasonalTransferMonthlyPriceField.addClassNames(LumoUtility.Flex.GROW);
        final var seasonalTransferDiv = new Div(seasonalTransferWinterPriceField, seasonalTransferOtherPriceField, seasonalTransferMonthlyPriceField);
        seasonalTransferDiv.setVisible(false);
        seasonalTransferDiv.addClassNames(LumoUtility.Display.FLEX, LumoUtility.Gap.Column.MEDIUM, LumoUtility.FlexWrap.WRAP);
        content.add(seasonalTransferDiv);

        // electricity taxes
        taxClassSelect = new Select<>();
        taxClassSelect.setId("taxClassSelect");
        taxClassSelect.setLabel(getTranslation("calculator.taxes"));
        taxClassSelect.setItemLabelGenerator(item -> getTranslation(item.getClassName()));
        taxClassSelect.setItems(TaxClass.values());
        taxClassSelect.setValue(TaxClass.CLASS_ONE);
        taxClassSelect.addClassNames(LumoUtility.Flex.GROW);
        taxClassSelect.setHelperText(getTranslation("calculator.tax.helper.text"));
        taxClassSelect.setVisible(false);
        //taxClassSelect.addValueChangeListener(item -> saveFieldValue(taxClassSelect));
        content.add(taxClassSelect);

        // Locked price
        lockedPriceField = new SuperDoubleField(null, getTranslation("locked.price"));
        lockedPriceField.setId("lockedPriceField");
        lockedPriceField.addValueChangeListener(item -> saveFieldValue(lockedPriceField));
        lockedPriceField.setLocale(getLocale());
        lockedPriceField.setHelperText(getTranslation("locked.price.example"));
        lockedPriceField.setRequiredIndicatorVisible(true);
        lockedPriceField.setSuffixComponent(new Span(getTranslation("c/kWh")));
        lockedPriceField.addClassNames(LumoUtility.Flex.GROW);
        lockedPriceField.setVisible(false);
        content.add(lockedPriceField);

        // battery as a service field
        baasPriceField = new SuperDoubleField(null, getTranslation("calculator.baas.price"));
        baasPriceField.setId("baasPriceField");
        baasPriceField.addValueChangeListener(item -> saveFieldValue(baasPriceField));
        baasPriceField.setMaximumFractionDigits(6);
        baasPriceField.setLocale(getLocale());
        baasPriceField.setHelperText(getTranslation("for.example") + " " + numberFormat.format(4) + " " + getTranslation("calculator.with.teravoima"));
        baasPriceField.setRequiredIndicatorVisible(true);
        baasPriceField.setSuffixComponent(new Span(getTranslation("c/kWh")));
        baasPriceField.addClassNames(LumoUtility.Flex.GROW);
        baasMonthlyPriceField = new SuperDoubleField(null, getTranslation("calculator.baas.monthly-price"));
        baasMonthlyPriceField.setId("baasMonthlyPriceField");
        baasMonthlyPriceField.addValueChangeListener(item -> saveFieldValue(baasMonthlyPriceField));
        baasMonthlyPriceField.setMaximumFractionDigits(6);
        baasMonthlyPriceField.setLocale(getLocale());
        baasMonthlyPriceField.setHelperText(getTranslation("calculator.baas.monthly-price-helper"));
        baasMonthlyPriceField.setRequiredIndicatorVisible(true);
        baasMonthlyPriceField.setSuffixComponent(new Span("€"));
        baasMonthlyPriceField.addClassNames(LumoUtility.Flex.GROW);
        final var baasDiv = new Div(baasPriceField, baasMonthlyPriceField);
        baasDiv.setVisible(false);
        baasDiv.addClassNames(LumoUtility.Display.FLEX, LumoUtility.Gap.Column.MEDIUM, LumoUtility.FlexWrap.WRAP);
        content.add(baasDiv);

        readFieldValues();

        calculationsCheckboxGroup.addValueChangeListener(e -> {
            fixedPriceField.setVisible(e.getValue().contains(Calculations.FIXED));
            fixedPriceField.setEnabled(e.getValue().contains(Calculations.FIXED));
            spotProductionMarginField.setVisible(e.getValue().contains(Calculations.SPOT_PRODUCTION));
            spotProductionMarginField.setEnabled(e.getValue().contains(Calculations.SPOT_PRODUCTION));
            transferDiv.setVisible(e.getValue().contains(Calculations.GENERAL_TRANSFER));
            transferDiv.setEnabled(e.getValue().contains(Calculations.GENERAL_TRANSFER));
            nightTransferDiv.setVisible(e.getValue().contains(Calculations.NIGHT_TRANSFER));
            nightTransferDiv.setEnabled(e.getValue().contains(Calculations.NIGHT_TRANSFER));
            seasonalTransferDiv.setVisible(e.getValue().contains(Calculations.SEASONAL_TRANSFER));
            seasonalTransferDiv.setEnabled(e.getValue().contains(Calculations.SEASONAL_TRANSFER));
            productionUpload.setVisible(e.getValue().contains(Calculations.SPOT_PRODUCTION));
            taxClassSelect.setVisible(e.getValue().contains(Calculations.TAXES));
            taxClassSelect.setEnabled(e.getValue().contains(Calculations.TAXES));
            lockedPriceField.setVisible(e.getValue().contains(Calculations.LOCKED_PRICE));
            lockedPriceField.setEnabled(e.getValue().contains(Calculations.LOCKED_PRICE));
            baasDiv.setVisible(e.getValue().contains(Calculations.BATTERY_AS_A_SERVICE));
            baasDiv.setEnabled(e.getValue().contains(Calculations.BATTERY_AS_A_SERVICE));
            updateCalculateButtonState();
            saveCheckboxGroupValues();
        });
        fields = Arrays.asList(fromDateTimePicker, toDateTimePicker, fixedPriceField, spotMarginField, transferDiv, nightTransferDiv, spotProductionMarginField, taxClassSelect, lockedPriceField, baasDiv);

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
                if (isCalculatingGeneralTransfer()) {
                    if (generalTransferField.getValue() == null) {
                        generalTransferField.setValue(0d);
                    }
                    if (transferMonthlyPriceField.getValue() == null) {
                        transferMonthlyPriceField.setValue(0d);
                    }
                }
                if (isCalculatingNightTransfer()) {
                    if (nightTransferDayPriceField.getValue() == null) {
                        nightTransferDayPriceField.setValue(0d);
                    }
                    if (nightTransferNightPriceField.getValue() == null) {
                        nightTransferNightPriceField.setValue(0d);
                    }
                    if (nightTransferMonthlyPriceField.getValue() == null) {
                        nightTransferMonthlyPriceField.setValue(0d);
                    }
                }
                if (isCalculatingSeasonalTransfer()) {
                    if (seasonalTransferWinterPriceField.getValue() == null) {
                        seasonalTransferWinterPriceField.setValue(0d);
                    }
                    if (seasonalTransferOtherPriceField.getValue() == null) {
                        seasonalTransferOtherPriceField.setValue(0d);
                    }
                    if (seasonalTransferMonthlyPriceField.getValue() == null) {
                        seasonalTransferMonthlyPriceField.setValue(0d);
                    }
                }
                if (isCalculatingLockedPrice()) {
                    if (lockedPriceField.getValue() == null) {
                        lockedPriceField.setValue(0d);
                    }
                }
                if (isCalculatingProduction()) {
                    if (spotProductionMarginField.getValue() == null) {
                        spotProductionMarginField.setValue(0d);
                    }
                }
                if (isCalculatingBaasPrice()) {
                    if (baasMonthlyPriceField.getValue() == null) {
                        baasMonthlyPriceField.setValue(0d);
                    }
                    if (baasPriceField.getValue() == null) {
                        baasPriceField.setValue(0d);
                    }
                }
                final var consumptionData = getFingridUsageData(lastConsumptionData);
                final var spotCalculation = calculateSpotElectricityPriceDetails(consumptionData.data(), spotMarginField.getValue(), true, fromDateTimePicker.getValue().atZone(fiZoneID).toInstant(), toDateTimePicker.getValue().atZone(fiZoneID).toInstant());
                resultLayout.removeAll();
                chartLayout.removeAll();

                final var start = format(spotCalculation.start, getLocale());
                final var end = format(spotCalculation.end, getLocale());

                final NumberFormat sixDecimals = getNumberFormat(getLocale(), 6);
                final var twoDecimalsWithPlusPrefix = getNumberFormatMaxTwoDecimalsWithPlusPrefix(getLocale());
                final NumberFormat threeDecimals = getNumberFormat(getLocale(), 3);

                final Div overviewDiv = addSection(resultLayout, getTranslation("Spot price"));

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
                final var priceWithoutMargin = spotCalculation.totalCostWithoutMargin / spotCalculation.totalConsumption * 100;
                final var loweredCost = (priceWithoutMargin - spotCalculation.averagePriceWithoutMargin) / Math.abs(spotCalculation.averagePriceWithoutMargin) * 100;
                final var formattedOwnSpotVsAverage = twoDecimalsWithPlusPrefix.format(loweredCost);
                overviewDiv.add(new DoubleLabel(getTranslation("calculator.spot.difference.percentage"), formattedOwnSpotVsAverage + " %", true));
                final var costEffect = (spotCalculation.totalCostWithoutMargin * 100 - spotCalculation.averagePriceWithoutMargin * spotCalculation.totalConsumption) / spotCalculation.totalConsumption;
                final var costEffectFormatted = twoDecimalsWithPlusPrefix.format(costEffect);
                overviewDiv.add(new DoubleLabel(getTranslation("calculator.spot.difference.cents"), costEffectFormatted + " " + getTranslation("c/kWh"), true));

                final var summaryDTO = new SummaryDTO();

                if (isCalculatingFixed()) {
                    final Div fixedPriceDiv = addSection(resultLayout, getTranslation("Fixed Price details"));
                    fixedPriceDiv.add(new DoubleLabel(getTranslation("Fixed price"), numberFormat.format(fixedPriceField.getValue()) + " " + getTranslation("c/kWh"), true));
                    var fixedCost = calculateFixedElectricityPrice(consumptionData.data(), fixedPriceField.getValue(), fromDateTimePicker.getValue().atZone(fiZoneID).toInstant(), toDateTimePicker.getValue().atZone(fiZoneID).toInstant());
                    fixedPriceDiv.add(new DoubleLabel(getTranslation("Fixed cost total"), numberFormat.format(fixedCost) + " €", true));

                    summaryDTO.setFixedCost(fixedCost);
                }

                if (isCalculatingLockedPrice()) {
                    final Div lockedPriceDiv = addSection(resultLayout, getTranslation("locked.price"));
                    lockedPriceDiv.add(new DoubleLabel(getTranslation("locked.price"), threeDecimals.format(lockedPriceField.getValue()) + " " + getTranslation("c/kWh"), true));
                    lockedPriceDiv.add(new DoubleLabel(getTranslation("Spot margin"), numberFormat.format(spotMarginField.getValue()) + " " + getTranslation("c/kWh"), true));
                    final var lockedPriceTotal = lockedPriceField.getValue() + spotMarginField.getValue() + costEffect;
                    lockedPriceDiv.add(new DoubleLabel(getTranslation("locked.price.total"), numberFormat.format(lockedPriceTotal) + " " + getTranslation("c/kWh"), true));
                    final var lockedPriceCost = lockedPriceTotal * spotCalculation.totalConsumption / 100;
                    lockedPriceDiv.add(new DoubleLabel(getTranslation("locked.price.cost"), numberFormat.format(lockedPriceCost) + " " + "€", true));

                    summaryDTO.setLockedPriceCost(lockedPriceCost);
                }

                if (isCalculatingGeneralTransfer()) {
                    final Div generalTransferDiv = addSection(resultLayout, getTranslation("calculator.general-transfer"));
                    generalTransferDiv.add(new DoubleLabel(getTranslation("calculator.general-transfer"), numberFormat.format(generalTransferField.getValue()) + " " + getTranslation("c/kWh"), true));
                    var transferTotalCost = calculateFixedElectricityPrice(consumptionData.data(), generalTransferField.getValue(), fromDateTimePicker.getValue().atZone(fiZoneID).toInstant(), toDateTimePicker.getValue().atZone(fiZoneID).toInstant());
                    generalTransferDiv.add(new DoubleLabel(getTranslation("calculator.general-transfer.total"), numberFormat.format(transferTotalCost) + " €", true));
                    generalTransferDiv.add(new DoubleLabel(getTranslation("calculator.spot.cost.and.transfer"), numberFormat.format(spotCalculation.totalCost + transferTotalCost) + " €", true));

                    final var monthsInvolved = calculateMonthsInvolved(fromDateTimePicker.getValue().atZone(fiZoneID).toInstant(), toDateTimePicker.getValue().atZone(fiZoneID).toInstant());
                    final var monthlyCost = monthsInvolved * transferMonthlyPriceField.getValue();
                    generalTransferDiv.add(new DoubleLabel(getTranslation("calculator.general-transfer.montly-cost"), numberFormat.format(monthlyCost) + " €", true));
                    final var totalTransferCost = monthlyCost + transferTotalCost;
                    generalTransferDiv.add(new DoubleLabel(getTranslation("calculator.general-transfer.total-cost.including-monthly"), numberFormat.format(totalTransferCost) + " €", true));

                    summaryDTO.setGeneralTransferCost(totalTransferCost);
                }

                if (isCalculatingNightTransfer()) {
                    final Div nightTransferSection = addSection(resultLayout, getTranslation("calculator.night-transfer.title"));
                    nightTransferSection.add(new DoubleLabel(getTranslation("calculator.night-transfer.day-price"), numberFormat.format(nightTransferDayPriceField.getValue()) + " " + getTranslation("c/kWh"), true));
                    nightTransferSection.add(new DoubleLabel(getTranslation("calculator.night-transfer.night-price"), numberFormat.format(nightTransferNightPriceField.getValue()) + " " + getTranslation("c/kWh"), true));
                    nightTransferSection.add(new DoubleLabel(getTranslation("calculator.night-transfer.monthly-price"), numberFormat.format(nightTransferMonthlyPriceField.getValue()) + " €", true));
                    final var dayCost = calculateDayPrice(consumptionData.data(), nightTransferDayPriceField.getValue(), fromDateTimePicker.getValue().atZone(fiZoneID).toInstant(), toDateTimePicker.getValue().atZone(fiZoneID).toInstant());
                    final var nightCost = calculateNightPrice(consumptionData.data(), nightTransferNightPriceField.getValue(), fromDateTimePicker.getValue().atZone(fiZoneID).toInstant(), toDateTimePicker.getValue().atZone(fiZoneID).toInstant());
                    final var dayAndNightCost = dayCost + nightCost;
                    final var dayConsumption = calculateDayConsumption(consumptionData.data(), fromDateTimePicker.getValue().atZone(fiZoneID).toInstant(), toDateTimePicker.getValue().atZone(fiZoneID).toInstant());
                    final var nightConsumption = calculateNightConsumption(consumptionData.data(), fromDateTimePicker.getValue().atZone(fiZoneID).toInstant(), toDateTimePicker.getValue().atZone(fiZoneID).toInstant());
                    final var totalConsumption = dayConsumption + nightConsumption;
                    final var dayPercentage = dayConsumption / totalConsumption * 100;
                    final var nightPercentage = nightConsumption / totalConsumption * 100;
                    nightTransferSection.add(new DoubleLabel(getTranslation("calculator.night-transfer.day-cost"), numberFormat.format(dayCost) + " €", true));
                    nightTransferSection.add(new DoubleLabel(getTranslation("calculator.night-transfer.night-cost"), numberFormat.format(nightCost) + " €", true));
                    nightTransferSection.add(new DoubleLabel(getTranslation("calculator.night-transfer.total-cost"), numberFormat.format(dayAndNightCost) + " €", true));
                    nightTransferSection.add(new DoubleLabel(getTranslation("calculator.night-transfer.day-consumption"), numberFormat.format(dayConsumption) + " kWh", true));
                    nightTransferSection.add(new DoubleLabel(getTranslation("calculator.night-transfer.night-consumption"), numberFormat.format(nightConsumption) + " kWh", true));
                    nightTransferSection.add(new DoubleLabel(getTranslation("calculator.night-transfer.percentage"), "%s %% / %s %%".formatted(numberFormat.format(dayPercentage), numberFormat.format(nightPercentage)), true));
                    final var monthsInvolved = calculateMonthsInvolved(fromDateTimePicker.getValue().atZone(fiZoneID).toInstant(), toDateTimePicker.getValue().atZone(fiZoneID).toInstant());
                    final var monthlyCost = monthsInvolved * nightTransferMonthlyPriceField.getValue();
                    nightTransferSection.add(new DoubleLabel(getTranslation("calculator.night-transfer.montly-cost"), numberFormat.format(monthlyCost) + " €", true));
                    final var totalNightTransferCost = monthlyCost + dayAndNightCost;
                    nightTransferSection.add(new DoubleLabel(getTranslation("calculator.night-transfer.total-cost.including-monthly"), numberFormat.format(totalNightTransferCost) + " €", true));

                    summaryDTO.setNighTransferCost(totalNightTransferCost);
                }

                if (isCalculatingSeasonalTransfer()) {
                    final Div seasonalTransferSection = addSection(resultLayout, getTranslation("calculator.seasonal-transfer.title"));
                    seasonalTransferSection.add(new DoubleLabel(getTranslation("calculator.seasonal-transfer.winter-price"), numberFormat.format(seasonalTransferWinterPriceField.getValue()) + " " + getTranslation("c/kWh"), true));
                    seasonalTransferSection.add(new DoubleLabel(getTranslation("calculator.seasonal-transfer.other-price"), numberFormat.format(seasonalTransferOtherPriceField.getValue()) + " " + getTranslation("c/kWh"), true));
                    seasonalTransferSection.add(new DoubleLabel(getTranslation("calculator.seasonal-transfer.monthly-price"), numberFormat.format(seasonalTransferMonthlyPriceField.getValue()) + " €", true));
                    final var winterCost = calculateSeasonalWinterPrice(consumptionData.data(), seasonalTransferWinterPriceField.getValue(), fromDateTimePicker.getValue().atZone(fiZoneID).toInstant(), toDateTimePicker.getValue().atZone(fiZoneID).toInstant());
                    final var otherCost = calculateSeasonalOtherPrice(consumptionData.data(), seasonalTransferOtherPriceField.getValue(), fromDateTimePicker.getValue().atZone(fiZoneID).toInstant(), toDateTimePicker.getValue().atZone(fiZoneID).toInstant());
                    final var winterAndOtherCost = winterCost + otherCost;
                    final var winterConsumption = calculateSeasonalWinterConsumption(consumptionData.data(), fromDateTimePicker.getValue().atZone(fiZoneID).toInstant(), toDateTimePicker.getValue().atZone(fiZoneID).toInstant());
                    final var otherConsumption = calculateSeasonalOtherConsumption(consumptionData.data(), fromDateTimePicker.getValue().atZone(fiZoneID).toInstant(), toDateTimePicker.getValue().atZone(fiZoneID).toInstant());
                    final var totalConsumption = winterConsumption + otherConsumption;
                    final var winterPercentage = winterConsumption / totalConsumption * 100;
                    final var otherPercentage = otherConsumption / totalConsumption * 100;
                    seasonalTransferSection.add(new DoubleLabel(getTranslation("calculator.seasonal-transfer.winter-cost"), numberFormat.format(winterCost) + " €", true));
                    seasonalTransferSection.add(new DoubleLabel(getTranslation("calculator.seasonal-transfer.other-cost"), numberFormat.format(otherCost) + " €", true));
                    seasonalTransferSection.add(new DoubleLabel(getTranslation("calculator.seasonal-transfer.total-cost"), numberFormat.format(winterAndOtherCost) + " €", true));
                    seasonalTransferSection.add(new DoubleLabel(getTranslation("calculator.seasonal-transfer.winter-consumption"), numberFormat.format(winterConsumption) + " kWh", true));
                    seasonalTransferSection.add(new DoubleLabel(getTranslation("calculator.seasonal-transfer.other-consumption"), numberFormat.format(otherConsumption) + " kWh", true));
                    seasonalTransferSection.add(new DoubleLabel(getTranslation("calculator.seasonal-transfer.percentage"), "%s %% / %s %%".formatted(numberFormat.format(winterPercentage), numberFormat.format(otherPercentage)), true));
                    final var monthsInvolved = calculateMonthsInvolved(fromDateTimePicker.getValue().atZone(fiZoneID).toInstant(), toDateTimePicker.getValue().atZone(fiZoneID).toInstant());
                    final var monthlyCost = monthsInvolved * seasonalTransferMonthlyPriceField.getValue();
                    seasonalTransferSection.add(new DoubleLabel(getTranslation("calculator.seasonal-transfer.montly-cost"), numberFormat.format(monthlyCost) + " €", true));
                    final var totalSeasonalTransferCost = monthlyCost + winterAndOtherCost;
                    seasonalTransferSection.add(new DoubleLabel(getTranslation("calculator.seasonal-transfer.total-cost.including-monthly"), numberFormat.format(totalSeasonalTransferCost) + " €", true));

                    summaryDTO.setSeasonalTransferCost(totalSeasonalTransferCost);
                }

                if (isCalculatingTax()) {
                    final Div taxSection = addSection(resultLayout, getTranslation("calculator.taxes"));
                    final var taxPrice = taxClassSelect.getValue().getTaxPrice();
                    final NumberFormat fiveDecimals = getNumberFormat(getLocale(), 5);
                    // TODO: this currently reports only one tax value. Either remove it or report all the used tax values.
                    //taxSection.add(new DoubleLabel(getTranslation("calculator.taxes"), fiveDecimals.format(taxPrice) + " " + getTranslation("c/kWh"), true));
                    var taxCost = calculateElectricityTaxPrice(consumptionData.data(), taxPrice, fromDateTimePicker.getValue().atZone(fiZoneID).toInstant(), toDateTimePicker.getValue().atZone(fiZoneID).toInstant());
                    taxSection.add(new DoubleLabel(getTranslation("calculator.tax.total"), numberFormat.format(taxCost) + " €", true));

                    summaryDTO.setTaxCost(taxCost);
                }

                if (isCalculatingBaasPrice()) {
                    final Div baasResultDiv = addSection(resultLayout, getTranslation("calculator.baas"));
                    FingridUsageData productionData = new FingridUsageData(null, null, null);
                    if (isCalculatingProduction()) {
                        productionData = getFingridUsageData(lastProductionData);
                    }
                    baasResultDiv.add(new DoubleLabel(getTranslation("calculator.baas"), numberFormat.format(baasPriceField.getValue()) + " " + getTranslation("c/kWh"), true));
                    var costList = calculateFixedElectricityPriceWithPastProductionReduced(consumptionData.data(), productionData.data(), baasPriceField.getValue(), fromDateTimePicker.getValue().atZone(fiZoneID).toInstant(), toDateTimePicker.getValue().atZone(fiZoneID).toInstant());
                    var transferTotalCost = costList.get(0);
                    baasResultDiv.add(new DoubleLabel(getTranslation("calculator.baas.consumption.total"), numberFormat.format(transferTotalCost) + " €", true));

                    var productionSavings = costList.get(1);
                    var unusedExcessProduction = costList.get(2);

                    if (isCalculatingProduction()) {
                        baasResultDiv.add(new DoubleLabel(getTranslation("calculator.baas.production.total"), numberFormat.format(productionSavings) + " €", true));
                        baasResultDiv.add(new DoubleLabel(getTranslation("calculator.baas.production.excess"), numberFormat.format(unusedExcessProduction) + " €", true));

                    }

                    final var monthsInvolved = calculateMonthsInvolved(fromDateTimePicker.getValue().atZone(fiZoneID).toInstant(), toDateTimePicker.getValue().atZone(fiZoneID).toInstant());
                    final var monthlyCost = monthsInvolved * baasMonthlyPriceField.getValue();
                    baasResultDiv.add(new DoubleLabel(getTranslation("calculator.baas.montly-cost"), numberFormat.format(monthlyCost) + " €", true));

                    final var totalBaasCost = monthlyCost + Math.max(transferTotalCost - productionSavings, 0);
                    baasResultDiv.add(new DoubleLabel(getTranslation("calculator.baas.total-cost.including-monthly"), numberFormat.format(totalBaasCost) + " €", true));

                    summaryDTO.setBaasCost(totalBaasCost);
                }

                if (isCalculatingProduction()) {
                    final var productionData = getFingridUsageData(lastProductionData);
                    final var spotProductionCalculation = calculateSpotElectricityPriceDetails(productionData.data(), -spotProductionMarginField.getValue(), false, fromDateTimePicker.getValue().atZone(fiZoneID).toInstant(), toDateTimePicker.getValue().atZone(fiZoneID).toInstant());
                    final Div productionDiv = addSection(resultLayout, getTranslation("Surplus production"));

                    productionDiv.add(new DoubleLabel(getTranslation("Surplus production over period"), numberFormat.format(spotProductionCalculation.totalConsumption) + " kWh", true));
                    productionDiv.add(new DoubleLabel(getTranslation("Net spot cost (consumption - production)"), numberFormat.format(spotCalculation.totalCost - spotProductionCalculation.totalCost) + " €", true));
                    productionDiv.add(new DoubleLabel(getTranslation("Net usage (consumption - production)"), numberFormat.format(spotCalculation.totalConsumption - spotProductionCalculation.totalConsumption) + " kWh", true));
                    productionDiv.add(new DoubleLabel(getTranslation("Average production price (incl. margin)"), threeDecimals.format(spotProductionCalculation.totalCost / spotProductionCalculation.totalConsumption * 100) + " " + getTranslation("c/kWh"), true));
                    productionDiv.add(new DoubleLabel(getTranslation("Production value (incl. margin)"), numberFormat.format(spotProductionCalculation.totalCost) + " €", true));
                    productionDiv.add(new DoubleLabel(getTranslation("Production value (without margin)"), numberFormat.format(spotProductionCalculation.totalCostWithoutMargin) + " €", true));
                }

                // summary section
                {
                    final Div summarySection = addSection(resultLayout, getTranslation("calculator.summary"));
                    summarySection.removeClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexWrap.WRAP, LumoUtility.Margin.Top.MEDIUM, LumoUtility.Padding.Left.SMALL);
                    final var summaryDiv = createWrapDiv();
                    summarySection.add(summaryDiv);
                    summaryDiv.add(new DoubleLabel(getTranslation("Calculation period (start times)"), start + " - " + end, true));
                    summaryDiv.add(new DoubleLabel(getTranslation("Total consumption over period"), numberFormat.format(spotCalculation.totalConsumption) + " kWh", true));

                    { // spot summary
                        var spotTotal = spotCalculation.totalCost;
                        String spotText = getTranslation("Spot + margin");
                        final var spotDiv = createWrapDiv();
                        summarySection.add(spotDiv);
                        spotDiv.add(new DoubleLabel(spotText, numberFormat.format(spotTotal) + " €", true));
                        if (summaryDTO.getTaxCost() != null) {
                            spotTotal += summaryDTO.getTaxCost();
                            spotText += " + %s".formatted(getTranslation("calculator.taxes").toLowerCase());
                            spotDiv.add(new DoubleLabel(spotText, numberFormat.format(spotTotal) + " €", true));
                        }
                        if (summaryDTO.getGeneralTransferCost() != null) {
                            addCostsAndCreateLabel(spotTotal, summaryDTO.getGeneralTransferCost(), spotText, "calculator.general-transfer", spotDiv, numberFormat);
                        }
                        if (summaryDTO.getNighTransferCost() != null) {
                            addCostsAndCreateLabel(spotTotal, summaryDTO.getNighTransferCost(), spotText, "calculator.night-transfer.title", spotDiv, numberFormat);
                        }
                        if (summaryDTO.getSeasonalTransferCost() != null) {
                            addCostsAndCreateLabel(spotTotal, summaryDTO.getSeasonalTransferCost(), spotText, "calculator.seasonal-transfer.title", spotDiv, numberFormat);
                        }
                    }

                    // Fixed summary
                    if (summaryDTO.getFixedCost() != null) {
                        var fixedCost = summaryDTO.getFixedCost();
                        String fixedCostText = getTranslation("Fixed");
                        final var fixedDiv = createWrapDiv();
                        summarySection.add(fixedDiv);
                        fixedDiv.add(new DoubleLabel(fixedCostText, numberFormat.format(fixedCost) + " €", true));
                        if (summaryDTO.getTaxCost() != null) {
                            fixedCost += summaryDTO.getTaxCost();
                            fixedCostText += " + %s".formatted(getTranslation("calculator.taxes").toLowerCase());
                            fixedDiv.add(new DoubleLabel(fixedCostText, numberFormat.format(fixedCost) + " €", true));
                        }
                        if (summaryDTO.getGeneralTransferCost() != null) {
                            addCostsAndCreateLabel(fixedCost, summaryDTO.getGeneralTransferCost(), fixedCostText, "calculator.general-transfer", fixedDiv, numberFormat);
                        }
                        if (summaryDTO.getNighTransferCost() != null) {
                            addCostsAndCreateLabel(fixedCost, summaryDTO.getNighTransferCost(), fixedCostText, "calculator.night-transfer.title", fixedDiv, numberFormat);
                        }
                        if (summaryDTO.getSeasonalTransferCost() != null) {
                            addCostsAndCreateLabel(fixedCost, summaryDTO.getSeasonalTransferCost(), fixedCostText, "calculator.seasonal-transfer.title", fixedDiv, numberFormat);
                        }
                    }

                    // Locked price summary
                    if (summaryDTO.getLockedPriceCost() != null) {
                        var lockedPriceCost = summaryDTO.getLockedPriceCost();
                        String fixedCostText = getTranslation("locked.price");
                        final var lockedPriceDiv = createWrapDiv();
                        summarySection.add(lockedPriceDiv);
                        lockedPriceDiv.add(new DoubleLabel(fixedCostText, numberFormat.format(lockedPriceCost) + " €", true));
                        if (summaryDTO.getTaxCost() != null) {
                            lockedPriceCost += summaryDTO.getTaxCost();
                            fixedCostText += " + %s".formatted(getTranslation("calculator.taxes").toLowerCase());
                            lockedPriceDiv.add(new DoubleLabel(fixedCostText, numberFormat.format(lockedPriceCost) + " €", true));
                        }
                        if (summaryDTO.getGeneralTransferCost() != null) {
                            addCostsAndCreateLabel(lockedPriceCost, summaryDTO.getGeneralTransferCost(), fixedCostText, "calculator.general-transfer", lockedPriceDiv, numberFormat);
                        }
                        if (summaryDTO.getNighTransferCost() != null) {
                            addCostsAndCreateLabel(lockedPriceCost, summaryDTO.getNighTransferCost(), fixedCostText, "calculator.night-transfer.title", lockedPriceDiv, numberFormat);
                        }
                        if (summaryDTO.getSeasonalTransferCost() != null) {
                            addCostsAndCreateLabel(lockedPriceCost, summaryDTO.getSeasonalTransferCost(), fixedCostText, "calculator.seasonal-transfer.title", lockedPriceDiv, numberFormat);
                        }
                    }

                    // BaaS summary
                    if (summaryDTO.getBaasCost() != null) {
                        var baasCost = summaryDTO.getBaasCost();
                        String baasCostText = getTranslation("calculator.baas");
                        final var baasSummaryDiv = createWrapDiv();
                        summarySection.add(baasSummaryDiv);
                        baasSummaryDiv.add(new DoubleLabel(baasCostText, numberFormat.format(baasCost) + " €", true));
                    }

                }

                // Create spot consumption chart
                chartLayout.add(createChart(spotCalculation, isCalculatingFixed(), getTranslation("Consumption / cost per hour"), getTranslation("Consumption"), getTranslation("Spot cost")));

                if (isCalculatingProduction()) {
                    final var productionData = getFingridUsageData(lastProductionData);
                    final var spotProductionCalculation = calculateSpotElectricityPriceDetails(productionData.data(), -spotProductionMarginField.getValue(), false, fromDateTimePicker.getValue().atZone(fiZoneID).toInstant(), toDateTimePicker.getValue().atZone(fiZoneID).toInstant());
                    // Create spot production chart
                    chartLayout.add(createChart(spotProductionCalculation, false, getTranslation("Production / value per hour"), "Production", "Production value"));
                }

            } catch (IOException | ParseException | CsvValidationException ex) {
                throw new RuntimeException(ex);
            }
        });
        calculateButton.addClassNames(LumoUtility.Margin.Top.MEDIUM);

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

    private void addCostsAndCreateLabel(Double cost, Double addedCost, String text, String translate, Div div, NumberFormat numberFormat) {
        var spotAndGeneralTransferCost = cost + addedCost;
        final var formatted = "%s + %s".formatted(text, getTranslation(translate).toLowerCase());
        div.add(new DoubleLabel(formatted, numberFormat.format(spotAndGeneralTransferCost) + " €", true));
    }

    private Div createWrapper() {
        final var wrapper = new Div();
        wrapper.addClassNames(LumoUtility.Margin.Horizontal.AUTO);
        wrapper.setWidthFull();
        wrapper.setMaxWidth(1024, Unit.PIXELS);
        add(wrapper);
        return wrapper;
    }

    private Div createWrapDiv(Component... components) {
        final var wrapDiv = new Div(components);
        wrapDiv.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexWrap.WRAP, LumoUtility.Margin.Top.MEDIUM, LumoUtility.Padding.Left.SMALL);
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
        final var details = new Details(overviewHeader, overviewDiv);
        details.setOpened(true);
        div.add(details);
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

    private boolean isCalculatingGeneralTransfer() {
        return calculationsCheckboxGroup.getValue().contains(Calculations.GENERAL_TRANSFER);
    }

    private boolean isCalculatingNightTransfer() {
        return calculationsCheckboxGroup.getValue().contains(Calculations.NIGHT_TRANSFER);
    }

    private boolean isCalculatingSeasonalTransfer() {
        return calculationsCheckboxGroup.getValue().contains(Calculations.SEASONAL_TRANSFER);
    }

    private boolean isCalculatingProduction() {
        return calculationsCheckboxGroup.getValue().contains(Calculations.SPOT_PRODUCTION);
    }

    private boolean isCalculatingTax() {
        return calculationsCheckboxGroup.getValue().contains(Calculations.TAXES);
    }

    private boolean isCalculatingLockedPrice() {
        return calculationsCheckboxGroup.getValue().contains(Calculations.LOCKED_PRICE);
    }

    private boolean isCalculatingBaasPrice() {
        return calculationsCheckboxGroup.getValue().contains(Calculations.BATTERY_AS_A_SERVICE);
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
        consumptionUpload.addFailedListener(e -> setEnabled(false, fixedPriceField, spotMarginField, generalTransferField, spotProductionMarginField, fromDateTimePicker, toDateTimePicker, calculateButton));
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
        productionUpload.addFailedListener(e -> setEnabled(false, fixedPriceField, spotMarginField, generalTransferField, spotProductionMarginField, fromDateTimePicker, toDateTimePicker, calculateButton));
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
        spotCostHoursSeries.setVisible(false);

        // Weighted spot average series
        final var spotAverageSeries = new ListSeries(getTranslation("Spot average (without margin)"));
        for (int i = 0; i < spotCalculation.spotAverage.length; ++i) {
            final var consumptionHour = spotCalculation.consumptionHours[i];
            final var costHoursWithoutMargin = spotCalculation.costHoursWithoutMargin[i];
            spotAverageSeries.addData(costHoursWithoutMargin / consumptionHour * 100);
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

    public void addAd() {
        final var priimaImage = new Image("images/priimabannerivalkoinenpohja (002).png", "PKS Priima");
        priimaImage.addClassNames(LumoUtility.Margin.Vertical.SMALL, LumoUtility.BorderRadius.MEDIUM, LumoUtility.Border.ALL, LumoUtility.BoxShadow.MEDIUM);
        priimaImage.setMaxWidth("100%");
        priimaImage.setWidth("200px");
        priimaImage.setMinWidth("50px");
        priimaImage.getStyle().set("border-color", "#FFFFFF");

        final var pksAd = new Span(getTranslation("PKS.ad"));
        pksAd.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.AlignItems.CENTER);
        final var priimaAnchor = new Anchor("https://bit.ly/priima-sahkosopimus", priimaImage, pksAd);
        final var tooltip = com.vaadin.flow.component.shared.Tooltip.forComponent(priimaAnchor);

        tooltip.setText(getTranslation("PKS.ad.tooltip"));
        priimaAnchor.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);
        priimaAnchor.setMaxWidth(200, Unit.PIXELS);

        priimaAnchor.setTarget(AnchorTarget.BLANK);
        final var wrapper = new Div(priimaAnchor);
        wrapper.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.JustifyContent.CENTER, LumoUtility.Flex.GROW_NONE);

        topRowDiv.add(wrapper);
    }

    @Getter
    enum Calculations {
        SPOT("Spot price"),
        FIXED("Fixed price"),
        LOCKED_PRICE("locked.price"),
        GENERAL_TRANSFER("calculator.general-transfer"),
        NIGHT_TRANSFER("calculator.night-transfer.title"),
        SEASONAL_TRANSFER("calculator.seasonal-transfer.title"),
        TAXES("calculator.taxes"),
        SPOT_PRODUCTION("Spot production price"),
        BATTERY_AS_A_SERVICE("calculator.baas");

        private final String name;

        Calculations(String name) {
            this.name = name;
        }

    }

    @Getter
    enum TaxClass {
        CLASS_ONE("tax.class.one", 2.253),
        CLASS_TWO("tax.class.two", 0.063);

        private final String className;
        private final double taxPrice;

        TaxClass(String className, double taxPrice) {
            this.className = className;
            this.taxPrice = taxPrice;
        }

    }

    @NoArgsConstructor
    @Getter
    @Setter
    private static class SummaryDTO {
        private Double fixedCost;
        private Double generalTransferCost;
        private Double nighTransferCost;
        private Double seasonalTransferCost;
        private Double taxCost;
        private Double lockedPriceCost;
        private Double baasCost;
    }

    ObjectMapper mapper = new ObjectMapper();

    public void saveCheckboxGroupValues() {
        try {
            WebStorage.setItem(calculationsCheckboxGroup.getId().orElseThrow(), mapper.writeValueAsString(calculationsCheckboxGroup.getValue()));
        } catch (JsonProcessingException e) {
            log.info("Could not save values: %s".formatted(e.toString()));
        }
    }

    public <C extends AbstractField<C, T>, T> void saveFieldValue(AbstractField<C, T> field) {
        try {
            WebStorage.setItem(field.getId().orElseThrow(), mapper.writeValueAsString(field.getValue()));
        } catch (JsonProcessingException e) {
            log.info("Could not save value: %s".formatted(e.toString()));
        }
    }

    public void readFieldValues() {
        WebStorage.getItem(calculationsCheckboxGroup.getId().orElseThrow(), item -> {
            if (item == null) {
                return;
            }
            try {
                Set<Calculations> calculationsSet = mapper.readValue(item, new TypeReference<>() {
                });
                calculationsCheckboxGroup.setValue(calculationsSet);
            } catch (IOException e) {
                log.info("Could not read values: %s".formatted(e.toString()));
            }
        });
        WebStorage.getItem(fixedPriceField.getId().orElseThrow(), item -> readValue(item, fixedPriceField));
        WebStorage.getItem(spotMarginField.getId().orElseThrow(), item -> readValue(item, spotMarginField));
        WebStorage.getItem(spotProductionMarginField.getId().orElseThrow(), item -> readValue(item, spotProductionMarginField));
        WebStorage.getItem(generalTransferField.getId().orElseThrow(), item -> readValue(item, generalTransferField));
        WebStorage.getItem(transferMonthlyPriceField.getId().orElseThrow(), item -> readValue(item, transferMonthlyPriceField));
        WebStorage.getItem(nightTransferDayPriceField.getId().orElseThrow(), item -> readValue(item, nightTransferDayPriceField));
        WebStorage.getItem(nightTransferNightPriceField.getId().orElseThrow(), item -> readValue(item, nightTransferNightPriceField));
        WebStorage.getItem(nightTransferMonthlyPriceField.getId().orElseThrow(), item -> readValue(item, nightTransferMonthlyPriceField));
        WebStorage.getItem(seasonalTransferWinterPriceField.getId().orElseThrow(), item -> readValue(item, seasonalTransferWinterPriceField));
        WebStorage.getItem(seasonalTransferOtherPriceField.getId().orElseThrow(), item -> readValue(item, seasonalTransferOtherPriceField));
        WebStorage.getItem(seasonalTransferMonthlyPriceField.getId().orElseThrow(), item -> readValue(item, seasonalTransferMonthlyPriceField));
        // TODO: this didn't work for some reason
        //WebStorage.getItem(taxClassSelect.getId().orElseThrow(), item -> readValue(item, taxClassSelect));
        WebStorage.getItem(lockedPriceField.getId().orElseThrow(), item -> readValue(item, lockedPriceField));
        WebStorage.getItem(baasPriceField.getId().orElseThrow(), item -> readValue(item, baasPriceField));
        WebStorage.getItem(baasMonthlyPriceField.getId().orElseThrow(), item -> readValue(item, baasMonthlyPriceField));
    }

    public <C extends HasValue.ValueChangeEvent<T>, T> void readValue(String key, HasValue<C, T> hasValue) {
        if (key == null) {
            return;
        }
        try {
            T value = mapper.readValue(key, new TypeReference<>() {
            });
            hasValue.setValue(value);
        } catch (IOException e) {
            log.info("Could not read value: %s".formatted(e.toString()));
        }
    }

}
