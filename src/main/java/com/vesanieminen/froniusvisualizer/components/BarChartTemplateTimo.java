package com.vesanieminen.froniusvisualizer.components;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.PropertyDescriptor;
import com.vaadin.flow.component.PropertyDescriptors;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vesanieminen.froniusvisualizer.services.model.NordpoolPrice;
import com.vesanieminen.froniusvisualizer.services.model.Plotline;
import com.vesanieminen.froniusvisualizer.util.Utils;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

@Tag("bar-chart-template-timo")
@JsModule("./src/bar-chart-template-timo.ts")
public class BarChartTemplateTimo extends Component {

    private static final PropertyDescriptor<String, String> CHART_TITLE = PropertyDescriptors.propertyWithDefault("chartTitle", "");
    private static final PropertyDescriptor<String, String> SERIES_TITLE = PropertyDescriptors.propertyWithDefault("seriesTitle", "");
    private static final PropertyDescriptor<String, String> UNIT = PropertyDescriptors.propertyWithDefault("unit", "");
    private static final PropertyDescriptor<String, String> POST_FIX = PropertyDescriptors.propertyWithDefault("postfix", "");
    private static final PropertyDescriptor<String, String> AVERAGE_TEXT = PropertyDescriptors.propertyWithDefault("averageText", "");
    private static final PropertyDescriptor<Double, Double> AVERAGE = PropertyDescriptors.propertyWithDefault("average", -100d);
    private static final PropertyDescriptor<String, String> LANGUAGE = PropertyDescriptors.propertyWithDefault("language", "en");
    private static final PropertyDescriptor<Boolean, Boolean> MOBILE_MODE = PropertyDescriptors.propertyWithDefault("mobileMode", false);

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        set(LANGUAGE, getLocale().getLanguage());
        String format = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(getLocale()).format(Utils.getCurrentLocalDateTimeHourPrecisionFinnishZone());
        set(CHART_TITLE, format);
        set(SERIES_TITLE, getTranslation("column-chart.series.title"));
        set(UNIT, getTranslation("column-chart.series.unit"));
        set(POST_FIX, getTranslation("c/kWh"));
        set(AVERAGE_TEXT, getTranslation("column-chart.month.average"));
    }

    public void setNordpoolDataList(List<NordpoolPrice> list) {
        getElement().setPropertyList("values", list);
    }

    public void setAverage(Double average) {
        getElement().setProperty("average", average);
    }

    public void setPlotline(List<Plotline> plotlines) {
        getElement().setPropertyList("plotLines", plotlines);
    }

    public void setMobileMode(boolean mobileMode) {
        set(MOBILE_MODE, mobileMode);
    }

    public void setMin(double min) {
        getElement().setProperty("min", min);
    }

    public void setMax(double max) {
        getElement().setProperty("max", max);
    }

}
