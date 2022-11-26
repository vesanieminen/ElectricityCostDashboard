package com.vesanieminen.froniusvisualizer.components;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vesanieminen.froniusvisualizer.views.NordpoolspotView;
import com.vesanieminen.froniusvisualizer.views.PriceCalculatorView;
import com.vesanieminen.froniusvisualizer.views.PriceListView;

import javax.servlet.http.Cookie;

import static com.vesanieminen.froniusvisualizer.util.Utils.enLocale;
import static com.vesanieminen.froniusvisualizer.util.Utils.fiLocale;

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
        Button changeLanguage = createChangeLanguageButton(attachEvent);
        add(graphButton, priceListButton, priceCalculationButton, changeLanguage);
    }

    private static Button createButton(String text) {
        Button button = new Button(text);
        button.setWidthFull();
        button.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.BorderRadius.NONE, LumoUtility.Margin.Vertical.NONE, LumoUtility.Height.MEDIUM, LumoUtility.BorderColor.CONTRAST_10, LumoUtility.Border.ALL);
        return button;
    }

    private Button createChangeLanguageButton(AttachEvent attachEvent) {
        var changeLanguage = createButton(null);
        changeLanguage.setSizeUndefined();
        changeLanguage.addClassNames(LumoUtility.Padding.Horizontal.LARGE);
        updateChangeLanguageButtonIcon(attachEvent.getUI(), changeLanguage);
        changeLanguage.addClickListener(e -> {
            VaadinService.getCurrentResponse().addCookie(new Cookie("locale", fiLocale.equals(attachEvent.getUI().getLocale()) ? enLocale.toLanguageTag() : fiLocale.toLanguageTag()));
            updateChangeLanguageButtonIcon(attachEvent.getUI(), changeLanguage);
            attachEvent.getUI().getPage().reload();
        });
        return changeLanguage;
    }

    private void updateChangeLanguageButtonIcon(UI ui, Button changeLanguage) {
        //changeLanguage.setText(fiLocale.equals(ui.getLocale()) ? getTranslation("to.english") : getTranslation("to.finnish"));
        if (fiLocale.equals(ui.getLocale())) {
            final var finnishIcon = new Image("icons/finland.png", "Finnish");
            finnishIcon.getElement().setAttribute("height", "32");
            finnishIcon.getElement().setAttribute("width", "32");
            changeLanguage.setIcon(finnishIcon);
        } else {
            final var ukIcon = new Image("icons/united-kingdom.png", "English");
            ukIcon.getElement().setAttribute("height", "32");
            ukIcon.getElement().setAttribute("width", "32");
            changeLanguage.setIcon(ukIcon);
        }
    }

}
