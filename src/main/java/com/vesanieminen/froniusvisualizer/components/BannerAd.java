package com.vesanieminen.froniusvisualizer.components;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;

public class BannerAd extends Div {

    public BannerAd() {
        addClassNames(
                LumoUtility.Margin.Vertical.NONE
        );

        setWidthFull();

        // Create text components
        Span title = new Span(getTranslation("liukuri.live.pks.interview"));
        title.addClassNames(
                LumoUtility.FontWeight.BOLD,
                LumoUtility.FontSize.MEDIUM
                //LumoUtility.Whitespace.NOWRAP,
                //LumoUtility.Overflow.HIDDEN,
                //LumoUtility.TextOverflow.ELLIPSIS
        );
        Span date = new Span(getTranslation("liukuri.live.pks.interview.datetime"));
        date.addClassNames(
                LumoUtility.TextColor.SECONDARY
        );

        // Arrange text vertically
        VerticalLayout textLayout = new VerticalLayout(title, date);
        textLayout.setSpacing(false);
        textLayout.setPadding(false);
        //textLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        //textLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        final var youTubeImage = new Image("images/YouTube_full-color_icon_(2017).svg.png", "YouTube");
        youTubeImage.setMaxHeight(30, Unit.PIXELS);
        final var horizontalLayout = new HorizontalLayout(textLayout, youTubeImage);
        horizontalLayout.addClassName("banner-ad");
        horizontalLayout.addClassNames(
                LumoUtility.AlignItems.CENTER,
                LumoUtility.Gap.MEDIUM,
                LumoUtility.BorderRadius.LARGE,
                LumoUtility.Padding.Horizontal.LARGE
        );
        horizontalLayout.setPadding(false);
        horizontalLayout.setSpacing(false);

        final var parent = new HorizontalLayout(horizontalLayout);
        parent.addClassNames(
                LumoUtility.JustifyContent.CENTER
        );

        // Make the banner clickable
        Anchor bannerLink = new Anchor("https://youtube.com/live/BPAdJQIYFiU", parent);
        bannerLink.setTarget("_blank"); // Open in new tab

        // Center the content
        add(bannerLink);
    }

}