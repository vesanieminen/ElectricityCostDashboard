package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.charts.model.AxisType;
import com.vaadin.flow.component.charts.model.ButtonPosition;
import com.vaadin.flow.component.charts.model.Crosshair;
import com.vaadin.flow.component.charts.model.DataSeries;
import com.vaadin.flow.component.charts.model.DataSeriesItem;
import com.vaadin.flow.component.charts.model.Legend;
import com.vaadin.flow.component.charts.model.Navigator;
import com.vaadin.flow.component.charts.model.PlotLine;
import com.vaadin.flow.component.charts.model.RangeSelector;
import com.vaadin.flow.component.charts.model.RangeSelectorButton;
import com.vaadin.flow.component.charts.model.RangeSelectorTimespan;
import com.vaadin.flow.component.charts.model.Time;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vesanieminen.froniusvisualizer.components.HistoryTemplate;
import org.vaadin.addons.parttio.lightchart.LightChart;

import java.time.Instant;

import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.getPrices;
import static com.vesanieminen.froniusvisualizer.views.MainLayout.URL_SUFFIX;

@Route(value = "historiakaavio", layout = MainLayout.class)
@PageTitle("History" + URL_SUFFIX)
public class HistoryView extends Main {

    public HistoryView() {
        LightChart c = new LightChart();
        c.setHeightFull();

        c.setTimeline(true);

        DataSeries data = new DataSeries();
        getPrices().stream().map(p -> new DataSeriesItem(p.time, p.price))
                .forEach(data::add);
        c.getConfiguration().addSeries(data);

        c.getConfiguration().getxAxis().setType(AxisType.DATETIME);
        var now = new PlotLine();
        now.setValue(Instant.now().getEpochSecond());
        c.getConfiguration().getxAxis().setPlotLines(now);
        c.getConfiguration().getyAxis().setSoftMin(0);

        var rangeSelector = c.getConfiguration().getRangeSelector();
        rangeSelector.setEnabled(true);
        rangeSelector.setSelected(2);
        rangeSelector.setButtons(
                new RangeSelectorButton(RangeSelectorTimespan.DAY, 7, "1d" ),
        new RangeSelectorButton(RangeSelectorTimespan.DAY, 14, "14d" ),
        new RangeSelectorButton(RangeSelectorTimespan.MONTH, 1, "1m" ),
                new RangeSelectorButton(RangeSelectorTimespan.MONTH, 3, "3m" ),
                new RangeSelectorButton(RangeSelectorTimespan.MONTH, 6, "6m" ),
                new RangeSelectorButton(RangeSelectorTimespan.YEAR_TO_DATE, 1, "ytd" ),
                new RangeSelectorButton(RangeSelectorTimespan.MONTH, 12, "12m" )
        );

        c.getConfiguration().setNavigator(new Navigator(true));
        c.getConfiguration().setLegend(new Legend(false));
        var time = new Time();
        time.setUseUTC(false);
        time.setTimezoneOffset(-120); // No way to set "Europe/Helsinki" in Vaadin Charts ?
        c.getConfiguration().setTime(time);

        add(c);

    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        attachEvent.getUI().getPage().retrieveExtendedClientDetails(details -> {
            if (!details.isTouchDevice()) {
                setHeight("var(--fullscreen-height)");
                setMinHeight("300px");
            }
        });
    }

}
