package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
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

import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateFixedElectricityPrice;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateSpotElectricityPriceDetails;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.getFingridConsumptionData;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.getSpotData;
import static com.vesanieminen.froniusvisualizer.util.Utils.decimalFormat;
import static com.vesanieminen.froniusvisualizer.util.Utils.fiLocale;

@Route("price-calculator")
public class PriceCalculatorView extends Div {

    private String lastFile;

    public PriceCalculatorView() throws IOException, ParseException {
        addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.Margin.AUTO);
        setHeightFull();
        setMaxWidth(1024, Unit.PIXELS);
        final var content = new Div();
        content.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.Padding.Horizontal.MEDIUM);
        add(content);

        final var title = new Span("Spot price / fixed electricity price calculator");
        title.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.MEDIUM);
        content.add(title);
        final var spotAverage = PriceCalculatorService.calculateSpotAveragePrice2022();
        final var span = new Span("Spot average in 2022 so far: " + decimalFormat.format(spotAverage) + " c/kWh");
        span.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
        content.add(span);

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
        helpStepsLayout.add(new Span("3) Modify the data to your liking, e.g. remove all other rows except one month's data."));
        helpStepsLayout.add(new Span("4) Upload the file below."));
        helpStepsLayout.add(new Span("5) Enter your comparative fixed electricity cost in the field below."));
        helpStepsLayout.add(new Span("6) Enter your spot price margin."));
        helpStepsLayout.add(new Span("7) Click the calculate costs button."));
        helpButton.addClickListener(e -> helpLayout.setVisible(!helpLayout.isVisible()));

        final var anchor = new Anchor("https://raw.githubusercontent.com/vesanieminen/ElectricityCostDashboard/main/src/main/resources/META-INF/resources/data/consumption.csv", "Download example csv file here");
        anchor.addClassNames(LumoUtility.Margin.Top.MEDIUM);
        helpLayout.add(anchor);

        final var additionalInfo = new Span("Do note that the Fingrid csv data is in UTC timezone which is currently 3h earlier than the Finnish timezone. For the moment if you want to calculate costs e.g. for August 2022 you need to include 3h from end of July and remove the last three hours from August.");
        additionalInfo.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
        helpLayout.add(additionalInfo);

        final var numberField = new NumberField("Fixed price");
        numberField.setRequiredIndicatorVisible(true);
        numberField.setSuffixComponent(new Span("c/kWh"));
        numberField.setPlaceholder("Please enter e.g. 12.50");
        //numberField.addClassNames(LumoUtility.Padding.Top.NONE);
        numberField.setWidth(16, Unit.EM);
        content.add(numberField);

        final var spotMargin = new NumberField("Spot margin");
        spotMargin.setRequiredIndicatorVisible(true);
        spotMargin.setSuffixComponent(new Span("c/kWh"));
        spotMargin.setPlaceholder("Please enter e.g. 0.38");
        spotMargin.setWidth(16, Unit.EM);
        content.add(spotMargin);

        FileBuffer fileBuffer = new FileBuffer();
        final var uploadFingridConsumptionData = new Button("Upload Fingrid consumption.csv data");
        Upload upload = new Upload(fileBuffer);
        upload.setUploadButton(uploadFingridConsumptionData);
        upload.setDropAllowed(true);
        upload.addClassNames(LumoUtility.Margin.Vertical.MEDIUM);
        content.add(upload);
        final var total = new Div();
        total.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.Margin.Top.MEDIUM);
        final var container = new Div();
        container.addClassNames(LumoUtility.Display.FLEX, LumoUtility.JustifyContent.EVENLY, LumoUtility.Gap.SMALL);
        final var spot = new Div();
        spot.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);
        final var fixed = new Div();
        fixed.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);
        //container.add(spot, fixed);
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
                //final var spotCost = calculateSpotElectricityPrice(getSpotData(), consumptionData, spotMargin.getValue());
                final var spotCalculation = calculateSpotElectricityPriceDetails(getSpotData(), consumptionData, spotMargin.getValue());
                final var fixedCost = calculateFixedElectricityPrice(consumptionData, numberField.getValue());
                total.removeAll();
                spot.removeAll();
                fixed.removeAll();
                final var start = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(fiLocale).format(spotCalculation.start);
                final var end = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(fiLocale).format(spotCalculation.end);
                total.add(new DoubleLabel("Calculation period (start times)", start + " - " + end, true));
                total.add(new DoubleLabel("Total consumption over period", decimalFormat.format(spotCalculation.totalConsumption) + "kWh", true));
                spot.add(new DoubleLabel("Average spot price (incl. margin)", decimalFormat.format(spotCalculation.totalCost / spotCalculation.totalConsumption * 100) + " c/kWh", true));
                spot.add(new DoubleLabel("Total spot cost (incl. margin)", decimalFormat.format(spotCalculation.totalCost) + "€", true));
                spot.add(new DoubleLabel("Total spot cost (without margin)", decimalFormat.format(spotCalculation.totalCostWithoutMargin) + "€", true));
                fixed.add(new DoubleLabel("Fixed price", numberField.getValue() + " c/kWh", true));
                fixed.add(new DoubleLabel("Fixed cost total", decimalFormat.format(fixedCost) + "€", true));
            } catch (IOException | ParseException ex) {
                throw new RuntimeException(ex);
            }
        });
        upload.addSucceededListener(event -> {
            FileData savedFileData = fileBuffer.getFileData();
            lastFile = savedFileData.getFile().getAbsolutePath();
            System.out.printf("File saved to: %s%n", lastFile);
            updateCalculateButtonState(button, numberField.getValue(), spotMargin.getValue());
        });
        numberField.addValueChangeListener(e -> updateCalculateButtonState(button, numberField.getValue(), spotMargin.getValue()));
        spotMargin.addValueChangeListener(e -> updateCalculateButtonState(button, numberField.getValue(), spotMargin.getValue()));
        button.setEnabled(false);
        content.add(button);
        content.add(total);
        content.add(container, spot, fixed);
        add(new Spacer());
        add(new Footer());
    }

    private void updateCalculateButtonState(Button button, Double fixedPrice, Double spotPrice) {
        button.setEnabled(fixedPrice != null && spotPrice != null && lastFile != null);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        final var backButton = new Button("Back to electricity price graph");
        backButton.addClassNames(LumoUtility.Height.MEDIUM, LumoUtility.Margin.NONE, LumoUtility.Margin.Bottom.MEDIUM, LumoUtility.BorderColor.CONTRAST_10, LumoUtility.Border.ALL, LumoUtility.BorderRadius.NONE);
        backButton.addClickListener(e -> attachEvent.getUI().navigate(NordpoolspotView.class));
        addComponentAsFirst(backButton);
    }

}
