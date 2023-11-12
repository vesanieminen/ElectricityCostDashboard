package com.vesanieminen.froniusvisualizer.components.list;

import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.theme.lumo.LumoUtility;

public class PriceListItem extends ListItem {

    public PriceListItem(Span timeSpan, Span priceSpan) {
        add(timeSpan, priceSpan);
        addClassNames(
                LumoUtility.Border.BOTTOM,
                LumoUtility.Display.FLEX,
                LumoUtility.JustifyContent.BETWEEN,
                LumoUtility.Padding.SMALL
        );
    }

}
