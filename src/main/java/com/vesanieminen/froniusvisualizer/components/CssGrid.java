package com.vesanieminen.froniusvisualizer.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.theme.lumo.LumoUtility;

public class CssGrid extends Div {

    public CssGrid(Component... components) {
        addClassNames(
                LumoUtility.Display.GRID,
                LumoUtility.Grid.Column.COLUMNS_2,
                LumoUtility.Padding.Horizontal.MEDIUM,
                LumoUtility.Gap.Column.MEDIUM
        );
        add(components);
    }
}
