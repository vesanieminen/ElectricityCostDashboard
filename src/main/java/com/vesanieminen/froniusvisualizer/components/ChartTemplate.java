package com.vesanieminen.froniusvisualizer.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.PropertyDescriptor;
import com.vaadin.flow.component.PropertyDescriptors;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vesanieminen.froniusvisualizer.util.Utils;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.getPricesToday;

@Tag("chart-template")
@JsModule("src/chart-template.ts")
public class ChartTemplate extends Component {

    private static final PropertyDescriptor<String, String> CHART_TITLE = PropertyDescriptors.propertyWithDefault("chartTitle", "");
    private static final PropertyDescriptor<String, String> SERIES_TITLE = PropertyDescriptors.propertyWithDefault("seriesTitle", "");
    private static final PropertyDescriptor<String, String> UNIT = PropertyDescriptors.propertyWithDefault("unit", "");
    private static final PropertyDescriptor<String, String> POST_FIX = PropertyDescriptors.propertyWithDefault("postfix", "");
    private static final PropertyDescriptor<Double, Double> AVERAGE = PropertyDescriptors.propertyWithDefault("average", -10d);
    private static final PropertyDescriptor<Integer, Integer> CURRENT_HOUR = PropertyDescriptors.propertyWithDefault("currentHour", 0);

    public ChartTemplate() {
        String format = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(getLocale()).format(Utils.getCurrentInstantHourPrecisionFinnishZone());
        set(CHART_TITLE, format);
        set(SERIES_TITLE, getTranslation("column-chart.series.title"));
        set(UNIT, getTranslation("column-chart.series.unit"));
        set(POST_FIX, getTranslation("c/kWh"));
        final var pricesToday = getPricesToday();
        setSeriesList(pricesToday);
        Utils.average(pricesToday).ifPresent(value -> set(AVERAGE, value));
        final var hour = Utils.getCurrentTimeWithHourPrecision().getHour();
        set(CURRENT_HOUR, hour);
    }

    public void setSeriesList(List<Double> list) {
        getElement().setPropertyList("values", list);
    }

}
