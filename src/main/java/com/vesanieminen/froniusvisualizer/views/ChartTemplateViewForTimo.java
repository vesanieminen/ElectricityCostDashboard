package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vesanieminen.froniusvisualizer.components.BarChartTemplateTimo;
import com.vesanieminen.froniusvisualizer.components.DoubleLabel;

import java.text.NumberFormat;

import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateSpotAveragePriceThisMonth;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateSpotAveragePriceToday;
import static com.vesanieminen.froniusvisualizer.util.Utils.getNumberFormat;
import static com.vesanieminen.froniusvisualizer.views.MainLayout.URL_SUFFIX;

@Route(value = "pörssisähkö", layout = MainLayout.class)
@PageTitle("Market electricity" + URL_SUFFIX)
public class ChartTemplateViewForTimo extends Main {

    public ChartTemplateViewForTimo() {
        //setHeight("var(--fullscreen-height)");
        //setMinHeight("300px");
        final var h2 = new H2("Liukuri.fi");
        h2.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.TextAlignment.CENTER, LumoUtility.Margin.Top.SMALL);
        add(h2);
        add(new BarChartTemplateTimo());


        final NumberFormat numberFormat = getNumberFormat(getLocale(), 2);
        numberFormat.setMinimumFractionDigits(2);
        final var averageTodayLabel = new DoubleLabel(getTranslation("Average today"), numberFormat.format(calculateSpotAveragePriceToday()) + " " + getTranslation("c/kWh"));
        final var averageThisMonthLabel = new DoubleLabel(getTranslation("Average this month"), numberFormat.format(calculateSpotAveragePriceThisMonth()) + " " + getTranslation("c/kWh"));

        final var div = new Div(averageTodayLabel, averageThisMonthLabel);
        div.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexWrap.WRAP, LumoUtility.Width.FULL/*, LumoUtility.BorderRadius.LARGE, LumoUtility.Border.ALL, LumoUtility.BorderColor.CONTRAST_10*/);
        add(div);
    }

}
