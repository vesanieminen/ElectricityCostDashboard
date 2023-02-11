package com.vesanieminen.froniusvisualizer.components;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.PropertyDescriptor;
import com.vaadin.flow.component.PropertyDescriptors;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vesanieminen.froniusvisualizer.services.model.NordpoolPrice;
import com.vesanieminen.froniusvisualizer.util.Utils;

import java.util.List;

import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.getPrices;

@Tag("history-template")
@JsModule("src/history-template.ts")
public class HistoryTemplate extends Component {

    private static final PropertyDescriptor<String, String> SERIES_TITLE = PropertyDescriptors.propertyWithDefault("seriesTitle", "");
    private static final PropertyDescriptor<String, String> UNIT = PropertyDescriptors.propertyWithDefault("unit", "");
    private static final PropertyDescriptor<String, String> POST_FIX = PropertyDescriptors.propertyWithDefault("postfix", "");
    private static final PropertyDescriptor<Integer, Integer> CURRENT_HOUR = PropertyDescriptors.propertyWithDefault("currentHour", 0);
    private static final PropertyDescriptor<String, String> LANGUAGE = PropertyDescriptors.propertyWithDefault("language", "en");

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        set(LANGUAGE, getLocale().getLanguage());
        set(SERIES_TITLE, getTranslation("column-chart.series.title"));
        set(UNIT, getTranslation("column-chart.series.unit"));
        set(POST_FIX, getTranslation("c/kWh"));
        var data = getPrices();
        setNordpoolDataList(data);
        final var hour = (int) Utils.getCurrentInstantHourPrecision().getEpochSecond();
        set(CURRENT_HOUR, hour);
    }

    public void setNordpoolDataList(List<NordpoolPrice> list) {
        getElement().setPropertyList("values", list);
    }
}
