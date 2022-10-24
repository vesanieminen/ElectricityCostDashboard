package com.vesanieminen.froniusvisualizer.components;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.theme.lumo.LumoUtility;

public class DoubleLabel extends Div {
    private final Span spanTop;
    private final Span spanBottom;

    public DoubleLabel(String titleTop, String titleBottom) {
        spanTop = new Span(titleTop);
        spanTop.addClassNames(LumoUtility.FontSize.MEDIUM, LumoUtility.TextColor.PRIMARY);
        spanBottom = new Span(titleBottom);
        spanBottom.addClassNames(LumoUtility.FontSize.MEDIUM, LumoUtility.TextColor.SECONDARY);
        add(spanTop, spanBottom);
        addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.Flex.GROW, LumoUtility.Flex.SHRINK_NONE, LumoUtility.AlignItems.CENTER);
        addClassNames(LumoUtility.Border.BOTTOM, LumoUtility.BorderColor.CONTRAST_10, LumoUtility.Padding.SMALL, LumoUtility.Padding.Horizontal.MEDIUM);
    }

    public DoubleLabel(String titleTop, String titleBottom, boolean noBorders) {
        this(titleTop, titleBottom);
        if (noBorders) {
            removeClassNames(LumoUtility.Border.BOTTOM);
        }
        getStyle().set("flex-basis", "300px");
    }

    public Span getSpanTop() {
        return spanTop;
    }

    public Span getSpanBottom() {
        return spanBottom;
    }


    public void setTitleTop(String title) {
        spanTop.setText(title);
    }

    public void setTitleBottom(String title) {
        spanBottom.setText(title);
    }

    public void addClassNamesToSpans(String... classNames) {
        spanTop.addClassNames(classNames);
        spanBottom.addClassNames(classNames);
    }

}
