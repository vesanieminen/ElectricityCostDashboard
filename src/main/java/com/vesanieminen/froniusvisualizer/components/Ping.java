package com.vesanieminen.froniusvisualizer.components;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vesanieminen.froniusvisualizer.util.css.Animate;
import com.vesanieminen.froniusvisualizer.util.css.Opacity;

public class Ping extends Span {

    public Ping(String label, String bgColorClassname) {
        addClassNames(
                LumoUtility.Display.INLINE_FLEX,
                LumoUtility.Position.RELATIVE
        );
        getElement().setAttribute("aria-label", label);
        setTitle(label);
        setHeight(8, Unit.PIXELS);
        setWidth(8, Unit.PIXELS);

        Span ping = new Span();
        ping.addClassNames(
                Animate.PING,
                LumoUtility.Background.PRIMARY_50,
                LumoUtility.BorderRadius.LARGE,
                LumoUtility.Display.FLEX,
                Opacity._75,
                LumoUtility.Position.ABSOLUTE
        );
        ping.setSizeFull();

        Span dot = new Span();
        dot.addClassNames(
                bgColorClassname,
                LumoUtility.BorderRadius.LARGE,
                LumoUtility.Display.FLEX,
                LumoUtility.Position.RELATIVE
        );
        dot.setSizeFull();

        add(ping, dot);
    }

}
