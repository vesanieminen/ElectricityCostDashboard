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
        addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.Background.CONTRAST_5, LumoUtility.Padding.SMALL);
        addClassNames(LumoUtility.AlignItems.CENTER, LumoUtility.JustifyContent.CENTER, LumoUtility.Flex.SHRINK_NONE);

        final var icon = new Icon(VaadinIcon.VAADIN_H);
        icon.addClassNames(LumoUtility.TextColor.PRIMARY);
        final var vaadinLink = new Anchor("http://vaadin.com", "Built with Vaadin ");
        vaadinLink.add(icon);
        final var image = new Image("https://cdn.ko-fi.com/cdn/kofi2.png?v=3", "Buy Me a Coffee at ko-fi.com");
        image.setHeight("36px");
        image.getStyle().set("border", "0px");
        image.getElement().setAttribute("height", "36");
        image.getElement().setAttribute("border", "0");
        final var kofiLink = new Anchor("https://ko-fi.com/F2F4FU50T");
        kofiLink.addClassNames(LumoUtility.Display.FLEX);
        kofiLink.add(image);
        final var githubIcon = new Image("images/GitHub-Mark-32px.png", "GitHub icon");
        final var githubLink = new Anchor("https://github.com/vesanieminen/ElectricityCostDashboard", "Fork me on GitHub");
        githubLink.addClassNames(LumoUtility.Display.FLEX);
        githubLink.add(githubIcon);
        githubIcon.addClassNames(LumoUtility.IconSize.MEDIUM, LumoUtility.TextColor.PRIMARY, LumoUtility.Margin.Left.SMALL);
        final var spanLayout = new Span(vaadinLink, kofiLink, githubLink);
        spanLayout.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexWrap.WRAP, LumoUtility.JustifyContent.CENTER, LumoUtility.AlignItems.CENTER, LumoUtility.Gap.MEDIUM);
        add(spanLayout);
    }

}
