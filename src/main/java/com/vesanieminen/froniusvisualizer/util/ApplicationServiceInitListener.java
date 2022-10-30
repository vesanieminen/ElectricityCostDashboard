package com.vesanieminen.froniusvisualizer.util;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;

import static java.lang.System.setProperty;

public class ApplicationServiceInitListener implements VaadinServiceInitListener {

    @Override
    public void serviceInit(ServiceInitEvent e) {
        setProperty("vaadin.i18n.provider", TranslationProvider.class.getName());
    }

}