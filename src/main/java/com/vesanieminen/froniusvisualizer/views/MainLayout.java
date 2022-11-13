package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vesanieminen.froniusvisualizer.components.Footer;
import com.vesanieminen.froniusvisualizer.components.Spacer;

public class MainLayout extends Div implements RouterLayout {

    public MainLayout() {
        addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);
        setHeightFull();
        add(new Spacer());
        add(new Footer());
    }

    @Override
    public void showRouterLayoutContent(HasElement content) {
        Component target = null;
        if (content != null) {
            target = content.getElement().getComponent().orElseThrow(() -> new IllegalArgumentException("Content must be a Component"));
        }
        addComponentAsFirst(target);
    }

}
