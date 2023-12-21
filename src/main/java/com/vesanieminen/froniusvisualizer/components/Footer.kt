package com.vesanieminen.froniusvisualizer.components;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.theme.lumo.LumoUtility;

public class Footer extends Div {

    public Footer() {
        addClassNames(LumoUtility.Display.FLEX, LumoUtility.Background.CONTRAST_5, LumoUtility.Padding.Vertical.SMALL, LumoUtility.FlexWrap.WRAP);
        addClassNames(LumoUtility.AlignItems.CENTER, LumoUtility.JustifyContent.CENTER, LumoUtility.Gap.MEDIUM);

        // Vaadin link
        final var icon = new Icon(VaadinIcon.VAADIN_H);
        icon.addClassNames(LumoUtility.TextColor.PRIMARY, LumoUtility.Margin.Left.SMALL);
        final var vaadinLink = new Anchor("http://vaadin.com", getTranslation("Built with Vaadin"));
        vaadinLink.addClassNames(LumoUtility.Display.FLEX);
        vaadinLink.add(icon);

        // Ko-Fi link
        final var image = new Image("https://cdn.ko-fi.com/cdn/kofi2.png?v=3", getTranslation("Buy Me a Coffee at ko-fi.com"));
        image.setHeight("36px");
        image.getStyle().set("border", "0px");
        image.getElement().setAttribute("height", "36");
        image.getElement().setAttribute("border", "0");
        final var kofiLink = new Anchor("https://ko-fi.com/F2F4FU50T");
        kofiLink.addClassNames(LumoUtility.Display.FLEX);
        kofiLink.add(image);

        // GitHub link
        final var githubIcon = new Image("images/GitHub-Mark-32px.png", "GitHub icon");
        githubIcon.addClassNames("footer-icon");
        final var githubLink = new Anchor("https://github.com/vesanieminen/ElectricityCostDashboard", getTranslation("Fork me on GitHub"));
        githubLink.addClassNames(LumoUtility.Display.FLEX);
        githubLink.add(githubIcon);
        githubIcon.addClassNames(LumoUtility.IconSize.MEDIUM, LumoUtility.TextColor.PRIMARY, LumoUtility.Margin.Left.SMALL);

        final var discordIcon = new Image("icons/discord-mark-black.png", "Discord icon");
        discordIcon.addClassNames("footer-icon");
        final var discordLink = new Anchor("https://discord.com/invite/WHcY2UkWVp", getTranslation("Join Discord"));
        discordLink.addClassNames(LumoUtility.Display.FLEX, LumoUtility.AlignItems.CENTER);
        discordLink.add(discordIcon);
        discordIcon.addClassNames(LumoUtility.IconSize.MEDIUM, LumoUtility.TextColor.PRIMARY, LumoUtility.Margin.Left.SMALL);

        add(vaadinLink, kofiLink, githubLink, discordLink);
    }

}
