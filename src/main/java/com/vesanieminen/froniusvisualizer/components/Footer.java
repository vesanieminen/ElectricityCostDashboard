package com.vesanieminen.froniusvisualizer.components;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.theme.lumo.LumoUtility;

public class Footer extends Div {

    public Footer() {
        final var icon = new Icon(VaadinIcon.VAADIN_H);
        icon.addClassNames(LumoUtility.Height.MEDIUM, LumoUtility.TextColor.PRIMARY);
        final var vaadin = new Anchor("http://vaadin.com", "Built with Vaadin ");
        final var anchor = new Anchor("https://github.com/vesanieminen/ElectricityCostDashboard", "Fork me on GitHub");
        final var githubIcon = new Image("images/GitHub-Mark-32px.png", "GitHub icon");
        githubIcon.addClassNames(LumoUtility.IconSize.MEDIUM, LumoUtility.TextColor.PRIMARY);
        final var divider = new Div();
        divider.setWidth("1px");
        divider.addClassNames(LumoUtility.Background.CONTRAST_10, LumoUtility.Height.FULL, LumoUtility.Margin.Horizontal.SMALL);
        final var spanLayout = new Span(vaadin, icon, divider, anchor, githubIcon);
        spanLayout.addClassNames(LumoUtility.Display.FLEX, LumoUtility.JustifyContent.CENTER, LumoUtility.AlignItems.CENTER, LumoUtility.Gap.SMALL, LumoUtility.Height.FULL);
        add(spanLayout);
        addClassNames(LumoUtility.Display.FLEX, LumoUtility.Background.CONTRAST_5, LumoUtility.Width.FULL, LumoUtility.Height.LARGE);
        addClassNames(LumoUtility.AlignItems.CENTER, LumoUtility.JustifyContent.CENTER, LumoUtility.Flex.SHRINK_NONE);
    }

}
