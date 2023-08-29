package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
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

import static com.vesanieminen.froniusvisualizer.services.NordpoolSpotService.getLatest7Days;
import static com.vesanieminen.froniusvisualizer.services.NordpoolSpotService.getLatest7DaysList;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateSpotAveragePriceThisMonth;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateSpotAveragePriceToday;
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

        final var span = new H2(" - Liukuri.fi");
        span.addClassNames(LumoUtility.FontSize.MEDIUM, LumoUtility.TextColor.SECONDARY, LumoUtility.Display.FLEX, LumoUtility.AlignItems.CENTER);
        final var h2 = new H2("%s".formatted(day));
        h2.addClassNames(LumoUtility.FontSize.LARGE);
        final var title = new Div(h2, span);
        title.addClassNames(LumoUtility.Display.FLEX, LumoUtility.JustifyContent.CENTER, LumoUtility.Margin.Top.SMALL, LumoUtility.Gap.SMALL);
        add(title);

        add(new BarChartTemplateTimo());

        final NumberFormat numberFormat = getNumberFormat(getLocale(), 2);
        numberFormat.setMinimumFractionDigits(2);
        final var latest7DaysList = getLatest7DaysList();
        final var averageTodayLabel = new DoubleLabel(getTranslation("Average today"), numberFormat.format(calculateSpotAveragePriceToday()) + " " + getTranslation("c/kWh"));
        final var averageThisMonthLabel = new DoubleLabel(getTranslation("Average this month"), numberFormat.format(calculateSpotAveragePriceThisMonth()) + " " + getTranslation("c/kWh"));
        averageThisMonthLabel.getSpanBottom().getStyle().set("border-color", "rgb(242, 182, 50)");
        averageThisMonthLabel.getSpanBottom().getStyle().set("border-width", "medium");
        averageThisMonthLabel.getSpanBottom().addClassNames(LumoUtility.Border.BOTTOM);

        final var div = new Div(averageTodayLabel, averageThisMonthLabel);
        div.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexWrap.WRAP, LumoUtility.Width.FULL/*, LumoUtility.BorderRadius.LARGE, LumoUtility.Border.ALL, LumoUtility.BorderColor.CONTRAST_10*/);
        add(div);
    }

    private DoubleLabel getLowestAndHighestPriceToday() {
        return null;
    }

}
