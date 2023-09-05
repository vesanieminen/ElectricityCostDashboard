package com.vesanieminen.froniusvisualizer.components;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.PropertyDescriptor;
import com.vaadin.flow.component.PropertyDescriptors;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vesanieminen.froniusvisualizer.services.model.NordpoolPrice;
import com.vesanieminen.froniusvisualizer.util.Utils;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.stream.Collectors;

import static com.vesanieminen.froniusvisualizer.services.NordpoolSpotService.getDateOfLatestFullDayData;
import static com.vesanieminen.froniusvisualizer.services.NordpoolSpotService.getLatest7DaysList;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.getPricesToday;
import static com.vesanieminen.froniusvisualizer.util.Utils.fiZoneID;

@Tag("bar-chart-template-timo")
@JsModule("src/bar-chart-template-timo.ts")
public class BarChartTemplateTimo extends Component {

    private static final PropertyDescriptor<String, String> CHART_TITLE = PropertyDescriptors.propertyWithDefault("chartTitle", "");
    private static final PropertyDescriptor<String, String> SERIES_TITLE = PropertyDescriptors.propertyWithDefault("seriesTitle", "");
    private static final PropertyDescriptor<String, String> UNIT = PropertyDescriptors.propertyWithDefault("unit", "");
    private static final PropertyDescriptor<String, String> POST_FIX = PropertyDescriptors.propertyWithDefault("postfix", "");
    private static final PropertyDescriptor<String, String> AVERAGE_TEXT = PropertyDescriptors.propertyWithDefault("averageText", "");
    private static final PropertyDescriptor<Double, Double> AVERAGE = PropertyDescriptors.propertyWithDefault("average", -100d);
    private static final PropertyDescriptor<Integer, Integer> CURRENT_HOUR = PropertyDescriptors.propertyWithDefault("currentHour", 0);
    private static final PropertyDescriptor<String, String> LANGUAGE = PropertyDescriptors.propertyWithDefault("language", "en");

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        final var epochOfLatestFullData = getDateOfLatestFullDayData().atZone(fiZoneID).toInstant().toEpochMilli();

        set(LANGUAGE, getLocale().getLanguage());
        String format = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(getLocale()).format(Utils.getCurrentLocalDateTimeHourPrecisionFinnishZone());
        set(CHART_TITLE, format);
        set(SERIES_TITLE, getTranslation("column-chart.series.title"));
        set(UNIT, getTranslation("column-chart.series.unit"));
        set(POST_FIX, getTranslation("c/kWh"));
        set(AVERAGE_TEXT, getTranslation("column-chart.month.average"));
        final var pricesToday = getPricesToday();
        var data = getLatest7DaysList().stream().filter(item -> item.time >= epochOfLatestFullData).collect(Collectors.toList());
        setNordpoolDataList(data);
        final var hour = (int) Utils.getCurrentInstantHourPrecision().getEpochSecond();
        set(CURRENT_HOUR, hour);
        //getElement().setProperty("height", "calc(100vh - 5rem)");


        //final var combinedSpotData = getCombinedSpotData();
        //final var test = getPricesForMonth(getDateOfLatestFullDayData().toLocalDate(), combinedSpotData);
        //int i = 0;
    }

    public void setNordpoolDataList(List<NordpoolPrice> list) {
        getElement().setPropertyList("values", list);
    }

    public void setAverage(Double average) {
        //set(AVERAGE, average);
        getElement().setProperty("average", average);
    }

    //public static List<Double> getPricesForMonth(LocalDate localDate, LinkedHashMap<Instant, Double> data) {
    //    final var month = localDate.getMonthValue();
    //    final var year = localDate.getYear();
    //    return data.entrySet().stream().filter(monthFilter(month, year)).map(Map.Entry::getValue).collect(Collectors.toList());
    //}


}
