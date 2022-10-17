package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.FileBuffer;
import com.vaadin.flow.component.upload.receivers.FileData;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vesanieminen.froniusvisualizer.services.PriceCalculatorService;

import java.io.IOException;
import java.text.ParseException;

import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateFixedElectricityPrice;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateSpotElectricityPriceDetails;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.getFingridConsumptionData;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.getSpotData;
import static com.vesanieminen.froniusvisualizer.util.Utils.decimalFormat;

@Route("price-calculator")
public class PriceCalculatorView extends Div {

    private String lastFile;

    public PriceCalculatorView() throws IOException, ParseException {
        addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.Padding.Horizontal.MEDIUM, LumoUtility.Margin.AUTO);
        setMaxWidth(1024, Unit.PIXELS);

        final var title = new Span("Spot price / fixed electricity price calculator");
        title.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.MEDIUM);
        add(title);
        final var spotAverage = PriceCalculatorService.calculateSpotAveragePrice2022();
        final var span = new Span("Spot average in 2022 so far: " + decimalFormat.format(spotAverage) + " c/kWh");
        span.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
        add(span);

        final var helpButton = new Button("Click to show/hide help");
        helpButton.addClassNames(LumoUtility.Margin.Top.SMALL, LumoUtility.Background.BASE);
        add(helpButton);
        final var helpLayout = new Div();
        helpLayout.setVisible(false);
        helpLayout.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);
        final var helpStepsLayout = new Div();
        helpStepsLayout.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.Margin.Left.LARGE);
        helpLayout.add(helpStepsLayout);
        add(helpLayout);
        final var help2 = new Span(new Span("1) login to "), new Anchor("https://www.fingrid.fi/en/electricity-market/datahub/sign-in-to-datahub-customer-portal/", "Fingrid Datahub"));
        final var help3 = new Span("2) Download your consumption data csv file");
        final var help4 = new Span("3) Upload the file below.");
        final var help5 = new Span("4) Enter your comparative fixed electricity cost in the field below.");
        final var help6 = new Span("5) Click the calculate costs button.");
        helpStepsLayout.add(help2, help3, help4, help5, help6);
        helpButton.addClickListener(e -> helpLayout.setVisible(!helpLayout.isVisible()));

        final var anchor = new Anchor("https://raw.githubusercontent.com/vesanieminen/ElectricityCostDashboard/main/src/main/resources/META-INF/resources/data/consumption.csv", "Download example csv file here");
        anchor.addClassNames(LumoUtility.Margin.Top.MEDIUM);
        helpLayout.add(anchor);

        final var additionalInfo = new Span("Do note that the Fingrid data is 3h ahead of the Finnish timezone. For the moment if you want to calculate costs e.g. for August 2022 you need to include 3h from end of July and remove the last three hours from August.");
        additionalInfo.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);
        helpLayout.add(additionalInfo);

        final var numberField = new NumberField("Fixed price");
        numberField.setRequiredIndicatorVisible(true);
        numberField.setSuffixComponent(new Span("c/kWh"));
        numberField.setPlaceholder("Please enter e.g. 12.50");
        //numberField.addClassNames(LumoUtility.Padding.Top.NONE);
        numberField.setWidth(16, Unit.EM);
        add(numberField);

        final var spotMargin = new NumberField("Spot margin");
        spotMargin.setRequiredIndicatorVisible(true);
        spotMargin.setSuffixComponent(new Span("c/kWh"));
        spotMargin.setPlaceholder("Please enter e.g. 0.38");
        spotMargin.setWidth(16, Unit.EM);
        add(spotMargin);

        FileBuffer fileBuffer = new FileBuffer();
        final var uploadFingridConsumptionData = new Button("Upload Fingrid consumption.csv data");
        Upload upload = new Upload(fileBuffer);
        upload.setUploadButton(uploadFingridConsumptionData);
        upload.setDropAllowed(true);
        upload.addSucceededListener(event -> {
            FileData savedFileData = fileBuffer.getFileData();
            lastFile = savedFileData.getFile().getAbsolutePath();
            System.out.printf("File saved to: %s%n", lastFile);
        });
        upload.addClassNames(LumoUtility.Margin.Vertical.MEDIUM);
        add(upload);
        final var total = new Div();
        total.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.Margin.Top.MEDIUM);
        final var container = new Div();
        container.addClassNames(LumoUtility.Display.FLEX, LumoUtility.JustifyContent.EVENLY, LumoUtility.Gap.SMALL);
        final var spot = new Div();
        spot.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);
        final var fixed = new Div();
        fixed.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);
        container.add(spot, fixed);
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
                final var totalConsumption = new Pre("Total consumption over period: " + decimalFormat.format(spotCalculation.totalConsumption) + "kWh");
                totalConsumption.addClassNames(LumoUtility.AlignSelf.CENTER);
                total.add(totalConsumption);
                spot.add(new Pre("Average spot price over period: \t" + decimalFormat.format(spotCalculation.averagePrice) + " c/kWh"));
                spot.add(new Pre("Total spot cost (incl. margin): \t" + decimalFormat.format(spotCalculation.totalCost) + "€"));
                spot.add(new Pre("Total spot cost (without margin): \t" + decimalFormat.format(spotCalculation.totalCostWithoutMargin) + "€"));
                fixed.add(new Pre("Fixed price: \t\t" + numberField.getValue() + " c/kWh"));
                fixed.add(new Pre("Fixed cost total: \t" + decimalFormat.format(fixedCost) + "€"));
            } catch (IOException | ParseException ex) {
                throw new RuntimeException(ex);
            }
        });
        numberField.addValueChangeListener(e -> button.setEnabled(e.getValue() != null));
        button.setEnabled(false);
        add(button);
        add(total);
        add(container);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        final var backButton = new Button("Back to electricity price view");
        backButton.addClassNames(LumoUtility.Height.MEDIUM, LumoUtility.Margin.NONE, LumoUtility.Margin.Bottom.MEDIUM, LumoUtility.BorderColor.CONTRAST_10, LumoUtility.Border.ALL);
        backButton.addClickListener(e -> attachEvent.getUI().navigate(NordpoolspotView.class));
        addComponentAsFirst(backButton);
    }

}
