package com.vesanieminen.froniusvisualizer.util;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinSession;
import jakarta.servlet.http.Cookie;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

import static java.lang.System.setProperty;

public class ApplicationServiceInitListener implements VaadinServiceInitListener {

    @Override
    public void serviceInit(ServiceInitEvent serviceInitEvent) {
        setProperty("vaadin.i18n.provider", TranslationProvider.class.getName());

        serviceInitEvent.addIndexHtmlRequestListener(indexHtmlResponse -> {
            String lang = readCookie(indexHtmlResponse.getVaadinRequest().getCookies(), "locale").orElse("en-GB");
            Locale locale = Locale.forLanguageTag(lang);
            indexHtmlResponse.getDocument().getElementsByTag("html")
                    .attr("lang", locale.getLanguage());

            VaadinSession.getCurrent().setLocale(locale);
        });
    }

    public Optional<String> readCookie(Cookie[] cookies, String key) {
        return Arrays.stream(cookies)
                .filter(c -> key.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}