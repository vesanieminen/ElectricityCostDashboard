package com.vesanieminen.froniusvisualizer.util;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;

import javax.servlet.http.Cookie;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

import static java.lang.System.setProperty;

public class ApplicationServiceInitListener implements VaadinServiceInitListener {

    @Override
    public void serviceInit(ServiceInitEvent serviceInitEvent) {
        setProperty("vaadin.i18n.provider", TranslationProvider.class.getName());
        serviceInitEvent.getSource().addUIInitListener(uiInitEvent -> {
            // Whenever a new user arrives, determine locale
            initLanguage(uiInitEvent.getUI());
            //uiInitEvent.getUI().setLocale(fiLocale);
        });
    }

    private void initLanguage(UI ui) {
        Optional<Cookie> localeCookie = Optional.empty();
        Cookie[] cookies = VaadinService.getCurrentRequest().getCookies();
        if (cookies != null) {
            localeCookie = Arrays.stream(cookies).filter(cookie -> "locale".equals(cookie.getName())).findFirst();
        }
        if (localeCookie.isPresent() && !"".equals(localeCookie.get().getValue())) {
            // Cookie found, use that
            ui.setLocale(Locale.forLanguageTag(localeCookie.get().getValue()));
        } else {
            // Try to use Vaadin's browser locale detection
            ui.setLocale(VaadinService.getCurrentRequest().getLocale());
        }
    }

}