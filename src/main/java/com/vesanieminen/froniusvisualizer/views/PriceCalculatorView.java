package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.AttachEvent;
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
import com.vesanieminen.froniusvisualizer.util.Utils;

import java.io.IOException;
import java.text.ParseException;

import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateFixedElectricityPrice;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateSpotElectricityPrice;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.getFingridConsumptionData;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.getSpotData;

@Route("price-calculator")
public class PriceCalculatorView extends Div {

    private String lastFile;

    public PriceCalculatorView() throws IOException, ParseException {
        addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.Margin.Horizontal.MEDIUM);

        final var title = new Span("Spot price / fixed electricity price calculator");
        title.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.MEDIUM);
        add(title);
        final var help = new Span("Usage:");
        help.addClassNames(LumoUtility.Margin.Top.MEDIUM);
        add(help);
        final var helpLayout = new Div();
        helpLayout.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.Margin.Left.LARGE);
        add(helpLayout);
        final var help2 = new Span(new Span("1) login to "), new Anchor("https://www.fingrid.fi/en/electricity-market/datahub/sign-in-to-datahub-customer-portal/", "Fingrid Datahub"));
        final var help3 = new Span("2) Download your consumption data csv file");
        final var help4 = new Span("3) Upload the file below");
        final var help5 = new Span("4) Enter your comparative fixed electricity cost in the field below");
        final var help6 = new Span("5) Click the calculate costs button");
        helpLayout.add(help2, help3, help4, help5, help6);

        final var anchor = new Anchor("https://raw.githubusercontent.com/vesanieminen/ElectricityCostDashboard/main/src/main/resources/META-INF/resources/data/consumption.csv", "Download example csv file here");
        anchor.addClassNames(LumoUtility.Margin.Vertical.MEDIUM);
        add(anchor);

        final var spotAverage = PriceCalculatorService.calculateSpotAveragePrice2022();
        final var span = new Span("Spot average in 2022 so far: " + Utils.decimalFormat.format(spotAverage) + " c/kWh");
        //span.addClassNames(LumoUtility.Margin.Top.MEDIUM);
        add(span);


        final var numberField = new NumberField("Fixed price for calculation");
        numberField.setRequiredIndicatorVisible(true);
        numberField.setSuffixComponent(new Span("c/kWh"));
        numberField.setPlaceholder("Please enter e.g. 12.50");
        numberField.setWidthFull();
        //numberField.addClassNames(LumoUtility.Padding.Top.NONE);
        add(numberField);

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
        final var container = new Div();
        container.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);
        final var button = new Button("Calculate costs", e -> {
            try {
                if (numberField.getValue() == null) {
                    Notification.show("Please add fixed price to compare with!", 3000, Notification.Position.MIDDLE);
                    return;
                }
                final var consumptionData = getFingridConsumptionData(lastFile);
                final var spotCost = calculateSpotElectricityPrice(getSpotData(), consumptionData);
                final var fixedCost = calculateFixedElectricityPrice(consumptionData, numberField.getValue());
                container.removeAll();
                container.add(new Pre("Spot cost total: " + Utils.decimalFormat.format(spotCost) + "€"));
                container.add(new Pre("Fixed price: " + numberField.getValue() + " c/kWh"));
                container.add(new Pre("Fixed cost total: " + Utils.decimalFormat.format(fixedCost) + "€"));
            } catch (IOException | ParseException ex) {
                throw new RuntimeException(ex);
            }
        });
        numberField.addValueChangeListener(e -> button.setEnabled(e.getValue() != null));
        button.setEnabled(false);
        add(button);
        add(container);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        final var backButton = new Button("Back");
        backButton.addClassNames(LumoUtility.Height.MEDIUM, LumoUtility.Background.BASE, LumoUtility.Margin.NONE, LumoUtility.Margin.Bottom.MEDIUM, LumoUtility.BorderColor.CONTRAST_10, LumoUtility.Border.ALL);
        backButton.addClickListener(e -> attachEvent.getUI().navigate(NordpoolspotView.class));
        addComponentAsFirst(backButton);
    }
}
