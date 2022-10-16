package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
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
        title.addClassNames(LumoUtility.TextColor.PRIMARY, LumoUtility.FontSize.MEDIUM);
        add(title);

        final var spotAverage = PriceCalculatorService.calculateSpotAveragePrice2022();
        add(new Span("Spot average in 2022 so far: " + Utils.decimalFormat.format(spotAverage) + " c/kWh"));

        final var numberField = new NumberField("Fixed price for calculation");
        numberField.setRequiredIndicatorVisible(true);
        numberField.setSuffixComponent(new Span("c/kWh"));
        numberField.setPlaceholder("Please enter e.g. 12.50");
        numberField.setWidthFull();
        add(numberField);

        //final var anchor = new Anchor("/data/consumption.csv", "Example consumption.csv file");
        //add(anchor);

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
