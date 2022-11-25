package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vesanieminen.froniusvisualizer.services.NordpoolSpotService;
import com.vesanieminen.froniusvisualizer.services.PakastinSpotService;
import com.vesanieminen.froniusvisualizer.services.model.NordpoolResponse;
import com.vesanieminen.froniusvisualizer.services.model.PakastinResponse;
import com.vesanieminen.froniusvisualizer.util.css.*;

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
import static com.vesanieminen.froniusvisualizer.views.MainLayout.URL_SUFFIX;

@PageTitle("Graph" + URL_SUFFIX)
@Route(value = "lista", layout = MainLayout.class)
@RouteAlias(value = "hintalista", layout = MainLayout.class)
@RouteAlias(value = "price-list", layout = MainLayout.class)
public class PriceListView extends Main {

    private static final double expensiveLimit = 10;
    private static final double cheapLimit = 2;

    public PriceListView() {
        addClassNames(LumoUtility.Overflow.AUTO, LumoUtility.Padding.Horizontal.SMALL);
        // Set height to correctly position sticky dates
        // 3.5 rem is the height of the app header.
        setHeight("calc(100vh - 3.5rem)");
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        renderView(attachEvent.getUI().getLocale());
    }

    void renderView(Locale locale) {
        var data = getData();
        if (data == null) {
            return;
        }
        addRows(data, locale);
    }

    private void addRows(NordpoolResponse data, Locale locale) {
        Collection<Component> containerList = new ArrayList<>();
        ListItem currentItem = null;

        var now = getCurrentTimeWithHourPrecision();
        NumberFormat format = NumberFormat.getInstance(Locale.FRANCE);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
        final var rows = data.data.Rows;
        int columnIndex = 6;
        while (columnIndex >= 0) {
            final var day = new H2();
            day.addClassNames(
                    LumoUtility.Background.BASE,
                    LumoUtility.Border.BOTTOM,
                    LumoUtility.BorderColor.CONTRAST_10,
                    LumoUtility.FontSize.XXLARGE,
                    Layout.TOP_0,
                    LumoUtility.Margin.Bottom.NONE,
                    LumoUtility.Margin.Horizontal.AUTO,
                    LumoUtility.Margin.Top.MEDIUM,
                    LumoUtility.MaxWidth.SCREEN_SMALL,
                    LumoUtility.Padding.SMALL,
                    LumoUtility.Position.STICKY,
                    Layout.Z_10
            );
            containerList.add(day);

            final var list = new UnorderedList();
            list.addClassNames(
                    FontFamily.MONO,
                    LumoUtility.ListStyleType.NONE,
                    LumoUtility.Margin.Horizontal.AUTO,
                    LumoUtility.Margin.Vertical.NONE,
                    LumoUtility.MaxWidth.SCREEN_SMALL,
                    LumoUtility.Padding.NONE
            );
            containerList.add(list);

            for (NordpoolResponse.Row row : rows.subList(0, rows.size() - 6)) {
                final var time = row.StartTime.toString().split("T")[1];
                NordpoolResponse.Column column = row.Columns.get(columnIndex);
                final var dateTimeString = column.Name + " " + time;
                final var dataLocalDataTime = LocalDateTime.parse(dateTimeString, dateTimeFormatter);
                // Convert the Nordpool timezone (Norwegian) to Finnish time zone
                final var localDateTime = convertNordpoolLocalDateTimeToFinnish(dataLocalDataTime);
                day.setText(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale).format(dataLocalDataTime));
                try {
                    final var price = format.parse(column.Value).doubleValue() * 1.24 / 10;
                    final var timeSpan = new Span(DateTimeFormatter.ofPattern("HH:mm").withLocale(locale).format(localDateTime));
                    final var df = new DecimalFormat("#0.000");
                    final var priceSpan = new Span(df.format(price) + "¢");

                    final var item = new ListItem(timeSpan, priceSpan);
                    item.addClassNames(
                            LumoUtility.Border.BOTTOM,
                            LumoUtility.Display.FLEX,
                            LumoUtility.JustifyContent.BETWEEN,
                            LumoUtility.Padding.SMALL,
                            Transform.Hover.SCALE_102,
                            Transition.TRANSITION
                    );

                    if (price <= cheapLimit) {
                        priceSpan.addClassName(LumoUtility.TextColor.SUCCESS);
                        item.addClassNames(Background.Hover.SUCCESS_10, BorderColor.Hover.SUCCESS);
                    }
                    if (price > cheapLimit && price < expensiveLimit) {
                        priceSpan.addClassName(LumoUtility.TextColor.PRIMARY);
                        item.addClassNames(Background.Hover.PRIMARY_10, BorderColor.Hover.PRIMARY);
                    }
                    if (price >= expensiveLimit) {
                        priceSpan.addClassName(LumoUtility.TextColor.ERROR);
                        item.addClassNames(Background.Hover.ERROR_10, BorderColor.Hover.ERROR);
                    }

                    // Current item
                    if (Objects.equals(localDateTime, now)) {
                        Span currentSpan = new Span("(current)");
                        currentSpan.addClassNames(
                                FontFamily.SANS,
                                LumoUtility.FontSize.XSMALL,
                                LumoUtility.FontWeight.NORMAL
                        );

                        timeSpan.setText(timeSpan.getText() + " ");
                        timeSpan.add(currentSpan);
                        timeSpan.addClassNames(LumoUtility.TextColor.PRIMARY);

                        item.addClassNames(
                                LumoUtility.Background.PRIMARY_10,
                                LumoUtility.BorderColor.PRIMARY,
                                LumoUtility.FontWeight.BOLD
                        );
                    } else {
                        item.addClassNames(LumoUtility.BorderColor.CONTRAST_10);
                    }
                    list.add(item);

                    // Due to the sticky position of some elements we need to scroll to the position of -2h
                    if (Objects.equals(localDateTime, now.minusHours(2))) {
                        currentItem = item;
                    }
                } catch (ParseException e) {
                }
            }
            --columnIndex;
        }

        add(containerList);
        currentItem.scrollIntoView();
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
