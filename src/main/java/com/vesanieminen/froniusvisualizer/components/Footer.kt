package com.vesanieminen.froniusvisualizer.components

import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Image
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.theme.lumo.LumoUtility

class Footer : Div() {
    init {
        addClassNames(LumoUtility.Display.FLEX, LumoUtility.Background.CONTRAST_5, LumoUtility.Padding.Vertical.SMALL, LumoUtility.FlexWrap.WRAP)
        addClassNames(LumoUtility.AlignItems.CENTER, LumoUtility.JustifyContent.CENTER, LumoUtility.Gap.MEDIUM)

        // Vaadin link
        val icon = Icon(VaadinIcon.VAADIN_H)
        icon.addClassNames(LumoUtility.TextColor.PRIMARY, LumoUtility.Margin.Left.SMALL)
        val vaadinLink = Anchor("http://vaadin.com", getTranslation("Built with Vaadin"))
        vaadinLink.addClassNames(LumoUtility.Display.FLEX)
        vaadinLink.add(icon)

        // Ko-Fi link
        val image = Image("https://cdn.ko-fi.com/cdn/kofi2.png?v=3", getTranslation("Buy Me a Coffee at ko-fi.com"))
        image.height = "36px"
        image.style["border"] = "0px"
        image.element.setAttribute("height", "36")
        image.element.setAttribute("border", "0")
        val kofiLink = Anchor("https://ko-fi.com/F2F4FU50T")
        kofiLink.addClassNames(LumoUtility.Display.FLEX)
        kofiLink.add(image)

        // GitHub link
        val githubIcon = Image("images/GitHub-Mark-32px.png", "GitHub icon")
        githubIcon.addClassNames("footer-icon")
        val githubLink = Anchor("https://github.com/vesanieminen/ElectricityCostDashboard", getTranslation("Fork me on GitHub"))
        githubLink.addClassNames(LumoUtility.Display.FLEX)
        githubLink.add(githubIcon)
        githubIcon.addClassNames(LumoUtility.IconSize.MEDIUM, LumoUtility.TextColor.PRIMARY, LumoUtility.Margin.Left.SMALL)
        val discordIcon = Image("icons/discord-mark-black.png", "Discord icon")
        discordIcon.addClassNames("footer-icon")
        val discordLink = Anchor("https://discord.com/invite/WHcY2UkWVp", getTranslation("Join Discord"))
        discordLink.addClassNames(LumoUtility.Display.FLEX, LumoUtility.AlignItems.CENTER)
        discordLink.add(discordIcon)
        discordIcon.addClassNames(LumoUtility.IconSize.MEDIUM, LumoUtility.TextColor.PRIMARY, LumoUtility.Margin.Left.SMALL)
        add(vaadinLink, kofiLink, githubLink, discordLink)
    }
}