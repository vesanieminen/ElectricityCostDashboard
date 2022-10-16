package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.router.Route;
import com.vesanieminen.froniusvisualizer.services.PriceCalculatorService;
import com.vesanieminen.froniusvisualizer.util.Utils;

import java.io.IOException;
import java.text.ParseException;

import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.getFingridConsumptionData;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.getSpotData;

@Route("price-calculator")
public class PriceCalculatorView extends Div {

    public PriceCalculatorView() throws IOException, ParseException {
        final var spotData = getSpotData();
        final var fingridConsumptionData = getFingridConsumptionData();
        final var spotAverage = PriceCalculatorService.calculateSpotAveragePrice2022(spotData);
        final var spotPrice = PriceCalculatorService.calculateSpotElectricityPrice(spotData, fingridConsumptionData);
        final var fixed = 12;
        final var fixedPrice = PriceCalculatorService.calculateFixedElectricityPrice(fingridConsumptionData, fixed);
        add(new Pre("Spot average 2022: " + Utils.decimalFormat.format(spotAverage) + " c/kWh"));
        add(new Pre("Spot price total: " + Utils.decimalFormat.format(spotPrice) + "€"));
        add(new Pre("Fixed price: " + fixed + " c/kWh"));
        add(new Pre("Fixed price total: " + Utils.decimalFormat.format(fixedPrice) + "€"));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
    }

}
