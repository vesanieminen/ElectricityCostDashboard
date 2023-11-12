package com.vesanieminen.froniusvisualizer.components.list;

import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vesanieminen.froniusvisualizer.util.css.FontFamily;

public class UnorderedPriceList extends UnorderedList {

    public UnorderedPriceList() {
        addClassNames(
                FontFamily.MONO,
                LumoUtility.ListStyleType.NONE,
                LumoUtility.Margin.Horizontal.AUTO,
                LumoUtility.Margin.Vertical.NONE,
                LumoUtility.MaxWidth.SCREEN_SMALL,
                LumoUtility.Padding.NONE
        );
    }

}
