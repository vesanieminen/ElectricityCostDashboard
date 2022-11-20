package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vesanieminen.froniusvisualizer.components.Footer;
import com.vesanieminen.froniusvisualizer.components.Header;
import com.vesanieminen.froniusvisualizer.components.Spacer;
import org.vaadin.googleanalytics.tracking.EnableGoogleAnalytics;
import org.vaadin.googleanalytics.tracking.TrackerConfiguration;
import org.vaadin.googleanalytics.tracking.TrackerConfigurator;

@EnableGoogleAnalytics(value = "G-K36G4GM72K")
public class MainLayout extends Div implements RouterLayout, TrackerConfigurator {

    public MainLayout() {
        addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);
        setHeightFull();
        add(new Header());
        add(new Spacer());
        add(new Footer());
    }

    @Override
    public void showRouterLayoutContent(HasElement content) {
        Component target = null;
        if (content != null) {
            target = content.getElement().getComponent().orElseThrow(() -> new IllegalArgumentException("Content must be a Component"));
        }
        addComponentAtIndex(1, target);
    }

    @Override
    public void configureTracker(TrackerConfiguration trackerConfiguration) {
        trackerConfiguration.setCreateField("allowAnchor", Boolean.FALSE);
        trackerConfiguration.setInitialValue("transport", "beacon");
    }

}
