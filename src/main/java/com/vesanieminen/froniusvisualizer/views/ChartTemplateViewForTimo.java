package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vesanieminen.froniusvisualizer.components.BarChartTemplateTimo;
import com.vesanieminen.froniusvisualizer.components.DoubleLabel;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.LinkedHashMap;

import static com.vesanieminen.froniusvisualizer.services.NordpoolSpotService.getLatest7Days;
import static com.vesanieminen.froniusvisualizer.services.NordpoolSpotService.getLatest7DaysMap;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateSpotAveragePriceThisMonth;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateSpotAveragePriceToday;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.getSpotData;
import static com.vesanieminen.froniusvisualizer.util.Utils.getNumberFormat;
import static com.vesanieminen.froniusvisualizer.views.MainLayout.URL_SUFFIX;

@Route(value = "porssisahkoryhma", layout = MainLayout.class)
@RouteAlias(value = "pörssisähköryhmä", layout = MainLayout.class)
@PageTitle("Market electricity group" + URL_SUFFIX)
public class ChartTemplateViewForTimo extends Main {

    public ChartTemplateViewForTimo() {
        //setHeight("var(--fullscreen-height)");
        //setMinHeight("300px");

        final var latest7Days = getLatest7Days();
        final var latestData = latest7Days.data.DataEnddate.minusDays(1);
        final var day = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(getLocale()).format(latestData);

        final var icon = new Image("icons/icon.png", "Liukuri");
        icon.setMaxWidth(30, Unit.PIXELS);
        icon.setMaxHeight(30, Unit.PIXELS);
        final var span = new H2("Liukuri.fi");
        span.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.TextColor.SECONDARY, LumoUtility.Display.FLEX, LumoUtility.AlignItems.CENTER);
        final var h2 = new H2("%s".formatted(day));
        h2.addClassNames(LumoUtility.FontSize.XLARGE);
        final var liukuriAd = new Div(span, icon);
        liukuriAd.addClassNames(LumoUtility.Display.FLEX, LumoUtility.Gap.SMALL, LumoUtility.JustifyContent.CENTER);
        final var title = new Div(h2, liukuriAd);
        title.addClassNames(LumoUtility.FlexDirection.COLUMN, LumoUtility.Display.FLEX, LumoUtility.AlignItems.CENTER, LumoUtility.Margin.Top.MEDIUM);
        add(title);

        add(new BarChartTemplateTimo());

        final NumberFormat numberFormat = getNumberFormat(getLocale(), 2);
        numberFormat.setMinimumFractionDigits(2);

        // Combine pakastin and nordpool data
        final var latest7DaysMap = getLatest7DaysMap();
        final var spotData = getSpotData();
        final var instantDoubleLinkedHashMap = new LinkedHashMap<>(spotData);
        final var size = instantDoubleLinkedHashMap.size();
        instantDoubleLinkedHashMap.putAll(latest7DaysMap);
        final var sizeAfter = instantDoubleLinkedHashMap.size();


        final var averageTodayLabel = new DoubleLabel(getTranslation("Average today"), numberFormat.format(calculateSpotAveragePriceToday()) + " " + getTranslation("c/kWh"));
        final var averageThisMonthLabel = new DoubleLabel(getTranslation("Average this month"), numberFormat.format(calculateSpotAveragePriceThisMonth()) + " " + getTranslation("c/kWh"));
        averageThisMonthLabel.getSpanBottom().getStyle().set("border-color", "rgb(242, 182, 50)");
        averageThisMonthLabel.getSpanBottom().getStyle().set("border-width", "medium");
        averageThisMonthLabel.getSpanBottom().addClassNames(LumoUtility.Border.BOTTOM);

        final var div = new Div(averageTodayLabel, averageThisMonthLabel);
        div.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexWrap.WRAP, LumoUtility.Width.FULL/*, LumoUtility.BorderRadius.LARGE, LumoUtility.Border.ALL, LumoUtility.BorderColor.CONTRAST_10*/);
        //add(div);
    }

    private DoubleLabel getLowestAndHighestPriceToday() {
        return null;
    }

}
