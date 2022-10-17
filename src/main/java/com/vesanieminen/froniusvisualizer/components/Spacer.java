package com.vesanieminen.froniusvisualizer.components;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.theme.lumo.LumoUtility;

public class Spacer extends Div {

    public Spacer() {
        addClassNames(LumoUtility.Display.FLEX, LumoUtility.Flex.GROW);
    }

}
