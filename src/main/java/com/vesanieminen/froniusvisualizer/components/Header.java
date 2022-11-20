package com.vesanieminen.froniusvisualizer.components;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vesanieminen.froniusvisualizer.views.NordpoolspotView;
import com.vesanieminen.froniusvisualizer.views.PriceCalculatorView;
import com.vesanieminen.froniusvisualizer.views.PriceListView;

public class Header extends Div {

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        addClassNames(LumoUtility.Display.FLEX, LumoUtility.Width.FULL);
        final var graphButton = createButton(getTranslation("Graph"));
        final var priceListButton = createButton(getTranslation("List"));
        final var priceCalculationButton = createButton(getTranslation("Calculator"));
        graphButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(NordpoolspotView.class)));
        priceListButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(PriceListView.class)));
        priceCalculationButton.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(PriceCalculatorView.class)));
        add(graphButton, priceListButton, priceCalculationButton);
    }

    private static Button createButton(String text) {
        Button button = new Button(text);
        button.setWidthFull();
        button.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.BorderRadius.NONE, LumoUtility.Margin.Vertical.NONE, LumoUtility.Height.MEDIUM, LumoUtility.BorderColor.CONTRAST_10, LumoUtility.Border.ALL);
        return button;
    }

}
