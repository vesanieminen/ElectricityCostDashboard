package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vesanieminen.froniusvisualizer.services.NordpoolSpotService;
import com.vesanieminen.froniusvisualizer.services.PakastinSpotService;
import com.vesanieminen.froniusvisualizer.services.model.NordpoolResponse;
import com.vesanieminen.froniusvisualizer.services.model.PakastinResponse;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static com.vesanieminen.froniusvisualizer.services.PakastinSpotService.getLatest7Days;
import static com.vesanieminen.froniusvisualizer.util.Utils.convertNordpoolLocalDateTimeToFinnish;
import static com.vesanieminen.froniusvisualizer.util.Utils.fiZoneID;
import static com.vesanieminen.froniusvisualizer.util.Utils.getCurrentInstantHourPrecisionFinnishZone;
import static com.vesanieminen.froniusvisualizer.util.Utils.getCurrentTimeWithHourPrecision;
import static com.vesanieminen.froniusvisualizer.util.Utils.threeDecimals;

@Route(value = "lista", layout = MainLayout.class)
@RouteAlias(value = "hintalista", layout = MainLayout.class)
@RouteAlias(value = "price-list", layout = MainLayout.class)
@PageTitle("List")
public class PriceListView extends Div {

    private static final double expensiveLimit = 10;
    private static final double cheapLimit = 2;

    public PriceListView() {
        addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.JustifyContent.CENTER, LumoUtility.Margin.AUTO);
        setWidthFull();
        setMaxWidth(1024, Unit.PIXELS);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        renderView(attachEvent.getUI().getLocale());
        //renderViewPakastin(attachEvent.getUI().getLocale());

        //final var realtimeDataForMonth = getRealtimeDataForMonth();

        //final var saveCsv = new Button("Save csv", e -> writeToCSVFile());
        //add(saveCsv);
    }

    void renderView(Locale locale) {
        var data = getData();
        if (data == null) {
            return;
        }
        addRows(data, locale);
    }

    void renderViewPakastin(Locale locale) {
        PakastinSpotService.updateData();
        var data = getLatest7Days();
        if (data == null || data.isEmpty()) {
            return;
        }
        addRows(data, locale);
    }

    private void addRows(List<PakastinResponse.Price> data, Locale locale) {
        Collection<Component> containerList = new ArrayList<>();
        Div currentTimeDiv = null;
        final var nowLocalDateTime = getCurrentInstantHourPrecisionFinnishZone();
        Div dayDiv = null;
        Span daySpan;
        var day = data.get(0).date;
        boolean first = true;
        for (PakastinResponse.Price price : data) {
            var currentDay = price.date;
            if (currentDay.atZone(fiZoneID).truncatedTo(ChronoUnit.DAYS).isAfter(day.atZone(fiZoneID).truncatedTo(ChronoUnit.DAYS)) || first) {
                first = false;
                day = currentDay;
                dayDiv = createDayDiv();
                daySpan = createDaySpan();
                daySpan.setText(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale).format(price.date.atZone(fiZoneID)));
                dayDiv.add(daySpan);
                containerList.add(dayDiv);
            }
            final var timeDiv = createTimeDiv();
            final var localDateTime = price.date.atZone(fiZoneID).toLocalDateTime();
            final var timeSpan = new Span(DateTimeFormatter.ofPattern("HH:mm").format(localDateTime));
            final var vatPrice = price.value * 1.24 / 10;
            final var priceSpan = new Span(threeDecimals.format(vatPrice) + "¢");
            timeDiv.add(timeSpan, priceSpan);
            dayDiv.add(timeDiv);
            if (vatPrice <= cheapLimit) {
                priceSpan.addClassName("color-green");
            }
            if (vatPrice > cheapLimit && vatPrice < expensiveLimit) {
                priceSpan.addClassName("list-blue");
            }
            if (vatPrice >= expensiveLimit) {
                priceSpan.addClassName("list-red");
            }
            if (Objects.equals(localDateTime, nowLocalDateTime)) {
                timeDiv.addClassNames(LumoUtility.Background.CONTRAST_10);
            }
            // Due to the sticky position of some elements we need to scroll to the position of -2h
            if (Objects.equals(localDateTime, nowLocalDateTime.minusHours(2))) {
                currentTimeDiv = timeDiv;
            }
        }
        add(containerList);
        currentTimeDiv.scrollIntoView();
    }

    private Div createDayDiv() {
        final var dayDiv = new Div();
        dayDiv.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.JustifyContent.CENTER);
        return dayDiv;
    }

    private Span createDaySpan() {
        final var daySpan = new Span();
        daySpan.addClassNames(LumoUtility.Display.FLEX, LumoUtility.JustifyContent.CENTER, LumoUtility.Padding.MEDIUM, LumoUtility.Background.BASE);
        daySpan.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL, LumoUtility.Border.BOTTOM, LumoUtility.BorderColor.CONTRAST_10, "sticky-date");
        return daySpan;
    }

    private Div createTimeDiv() {
        final var div = new Div();
        div.addClassNames(LumoUtility.Display.FLEX, LumoUtility.JustifyContent.BETWEEN, LumoUtility.Border.BOTTOM, LumoUtility.BorderColor.CONTRAST_10, LumoUtility.Padding.SMALL, LumoUtility.Padding.Horizontal.MEDIUM);
        return div;
    }

    private void addRows(NordpoolResponse data, Locale locale) {
        Collection<Component> containerList = new ArrayList<>();
        Div currentTimeDiv = null;

        var now = getCurrentTimeWithHourPrecision();
        NumberFormat format = NumberFormat.getInstance(Locale.FRANCE);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
        final var rows = data.data.Rows;
        int columnIndex = 6;
        while (columnIndex >= 0) {
            final var dayDiv = new Div();
            dayDiv.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN, LumoUtility.JustifyContent.CENTER);
            final var daySpan = new Span();
            daySpan.addClassNames(LumoUtility.Display.FLEX, LumoUtility.JustifyContent.CENTER, LumoUtility.Padding.SMALL, LumoUtility.Background.BASE);
            daySpan.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL, LumoUtility.Border.BOTTOM, LumoUtility.BorderColor.CONTRAST_10, "sticky-date");
            dayDiv.add(daySpan);
            containerList.add(dayDiv);
            for (NordpoolResponse.Row row : rows.subList(0, rows.size() - 6)) {
                final var time = row.StartTime.toString().split("T")[1];
                NordpoolResponse.Column column = row.Columns.get(columnIndex);
                final var dateTimeString = column.Name + " " + time;
                final var dataLocalDataTime = LocalDateTime.parse(dateTimeString, dateTimeFormatter);
                // Convert the Nordpool timezone (Norwegian) to Finnish time zone
                final var localDateTime = convertNordpoolLocalDateTimeToFinnish(dataLocalDataTime);
                daySpan.setText(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale).format(dataLocalDataTime));
                try {
                    final var price = format.parse(column.Value).doubleValue() * 1.24 / 10;
                    final var div = new Div();
                    div.addClassNames(LumoUtility.Display.FLEX, LumoUtility.JustifyContent.BETWEEN, LumoUtility.Border.BOTTOM, LumoUtility.BorderColor.CONTRAST_10, LumoUtility.Padding.SMALL, LumoUtility.Padding.Horizontal.MEDIUM);
                    final var timeSpan = new Span(DateTimeFormatter.ofPattern("HH:mm").withLocale(locale).format(localDateTime));
                    final var df = new DecimalFormat("#0.000");
                    final var priceSpan = new Span(df.format(price) + "¢");
                    if (price <= cheapLimit) {
                        priceSpan.addClassName("color-green");
                    }
                    if (price > cheapLimit && price < expensiveLimit) {
                        priceSpan.addClassName("list-blue");
                    }
                    if (price >= expensiveLimit) {
                        priceSpan.addClassName("list-red");
                    }
                    div.add(timeSpan, priceSpan);
                    dayDiv.add(div);
                    if (Objects.equals(localDateTime, now)) {
                        div.addClassNames(LumoUtility.Background.CONTRAST_10);
                    }
                    // Due to the sticky position of some elements we need to scroll to the position of -2h
                    if (Objects.equals(localDateTime, now.minusHours(1))) {
                        currentTimeDiv = div;
                    }
                } catch (ParseException e) {
                }
            }
            --columnIndex;
        }

        add(containerList);
        currentTimeDiv.scrollIntoView();
    }

    private static NordpoolResponse getData() {
        NordpoolResponse nordpoolResponse = null;
        try {
            nordpoolResponse = NordpoolSpotService.getLatest7Days();
        } catch (URISyntaxException | IOException | InterruptedException e) {
            return nordpoolResponse;
        }
        return nordpoolResponse;
    }

}
