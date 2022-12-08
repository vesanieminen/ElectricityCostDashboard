package com.vesanieminen.froniusvisualizer.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.PropertyDescriptor;
import com.vaadin.flow.component.PropertyDescriptors;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.getPricesToday;
import static java.time.LocalDate.now;

@Tag("chart-template")
@JsModule("src/chart-template.ts")
public class ChartTemplate extends Component {

    private static final PropertyDescriptor<String, String> CHART_TITLE = PropertyDescriptors.propertyWithDefault("chartTitle", "");
    private static final PropertyDescriptor<String, String> SUBTITLE = PropertyDescriptors.propertyWithDefault("subtitle", "");
    private static final PropertyDescriptor<String, String> SERIES_TITLE = PropertyDescriptors.propertyWithDefault("seriesTitle", "");
    private static final PropertyDescriptor<String, String> UNIT = PropertyDescriptors.propertyWithDefault("unit", "");
    private static final PropertyDescriptor<String, String> POST_FIX = PropertyDescriptors.propertyWithDefault("postfix", "");

    public ChartTemplate() {
        String format = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(getLocale()).format(now());
        set(CHART_TITLE, "Prices");
        set(SUBTITLE, format);
        set(SERIES_TITLE, "Series title");
        set(UNIT, "Series unit");
        set(POST_FIX, getTranslation("c/kWh"));
        setSeriesList(getPricesToday());
    }

    public void setSeriesList(List<Double> list) {
        getElement().setPropertyList("values", list);
    }

}
