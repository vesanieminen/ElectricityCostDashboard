package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vesanieminen.froniusvisualizer.components.Ping;
import com.vesanieminen.froniusvisualizer.components.list.PriceListItem;
import com.vesanieminen.froniusvisualizer.services.model.NordpoolPrice;
import com.vesanieminen.froniusvisualizer.util.css.Background;
import com.vesanieminen.froniusvisualizer.util.css.BorderColor;
import com.vesanieminen.froniusvisualizer.util.css.FontFamily;
import com.vesanieminen.froniusvisualizer.util.css.Layout;
import com.vesanieminen.froniusvisualizer.util.css.Transform;
import com.vesanieminen.froniusvisualizer.util.css.Transition;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static com.vesanieminen.froniusvisualizer.services.NordpoolSpotService.getLatest7DaysList;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.calculateSpotAveragePriceThisMonth;
import static com.vesanieminen.froniusvisualizer.util.Utils.fiZoneID;
import static com.vesanieminen.froniusvisualizer.util.Utils.getCurrentLocalDateTimeHourPrecisionFinnishZone;
import static com.vesanieminen.froniusvisualizer.util.Utils.getNumberFormat;
import static com.vesanieminen.froniusvisualizer.util.Utils.getVAT;
import static com.vesanieminen.froniusvisualizer.views.MainLayout.URL_SUFFIX;

@PageTitle("List" + URL_SUFFIX)
@Route(value = "lista", layout = MainLayout.class)
@RouteAlias(value = "hintalista", layout = MainLayout.class)
@RouteAlias(value = "price-list", layout = MainLayout.class)
public class PriceListView extends Main {

    private final double expensiveLimit;
    private final double cheapLimit;

    public PriceListView() {
        addClassNames(LumoUtility.Overflow.AUTO, LumoUtility.Padding.Horizontal.SMALL);
        // Set height to correctly position sticky dates
        // Added fix for iOS Safari header height that changes when scrolling
        setHeight("var(--fullscreen-height-list)");
        expensiveLimit = calculateSpotAveragePriceThisMonth();
        cheapLimit = expensiveLimit / 2;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        renderListView(attachEvent);
    }

    private void setPriceTextColor(double price, Span span) {
        if (price <= cheapLimit) {
            span.addClassName(LumoUtility.TextColor.SUCCESS);
        }
        if (price > cheapLimit && price < expensiveLimit) {
            span.addClassName(LumoUtility.TextColor.PRIMARY);
        }
        if (price >= expensiveLimit) {
            span.addClassName(LumoUtility.TextColor.ERROR);
        }
    }

    private String getPriceBackgroundColor(double price) {
        if (price <= cheapLimit) {
            return LumoUtility.Background.SUCCESS;
        }
        if (price < expensiveLimit) {
            return LumoUtility.Background.PRIMARY;
        }
        if (price >= expensiveLimit) {
            return LumoUtility.Background.ERROR;
        }
        return LumoUtility.Background.PRIMARY;
    }

    private String getPriceBackgroundColor_10(double price) {
        if (price <= cheapLimit) {
            return LumoUtility.Background.SUCCESS_10;
        }
        if (price < expensiveLimit) {
            return LumoUtility.Background.PRIMARY_10;
        }
        if (price >= expensiveLimit) {
            return LumoUtility.Background.ERROR_10;
        }
        return LumoUtility.Background.PRIMARY_10;
    }

    private String getPriceBorderColor(double price) {
        if (price <= cheapLimit) {
            return LumoUtility.BorderColor.SUCCESS;
        }
        if (price < expensiveLimit) {
            return LumoUtility.BorderColor.PRIMARY;

        }
        if (price >= expensiveLimit) {
            return LumoUtility.BorderColor.ERROR;
        }
        return LumoUtility.BorderColor.PRIMARY;
    }

    void renderListView(AttachEvent attachEvent) {
        var data = getLatest7DaysList();
        if (data.isEmpty()) {
            return;
        }
        addRows(data, attachEvent);
    }

    private void addRows(List<NordpoolPrice> data, AttachEvent attachEvent) {
        var locale = attachEvent.getUI().getLocale();
        Collection<Component> containerList = new ArrayList<>();
        ListItem currentItem = null;
        final var nowLocalDateTime = getCurrentLocalDateTimeHourPrecisionFinnishZone();
        H2 day = null;
        UnorderedList list = null;
        Span daySpan;
        var currentDayTime = data.get(0).timeInstant();
        boolean first = true;
        for (NordpoolPrice entry : data) {
            var currentDay = entry.timeInstant();
            if (currentDay.atZone(fiZoneID).truncatedTo(ChronoUnit.DAYS).isAfter(currentDayTime.atZone(fiZoneID).truncatedTo(ChronoUnit.DAYS)) || first) {
                first = false;
                currentDayTime = currentDay;
                day = createDayH2();
                containerList.add(day);
                list = createUnorderedList();
                containerList.add(list);
                daySpan = createDaySpan();
                daySpan.setText(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale).format(entry.timeInstant().atZone(fiZoneID)));
            }
            final var localDateTime = entry.timeInstant().atZone(fiZoneID).toLocalDateTime();
            day.setText(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale).format(localDateTime));
            final var timeSpan = new Span(DateTimeFormatter.ofPattern("HH:mm").withLocale(locale).format(localDateTime));
            final var vatPrice = entry.price() * getVAT(entry.timeInstant());

            final NumberFormat numberFormat = getNumberFormat(getLocale(), 2);
            numberFormat.setMinimumFractionDigits(2);
            final var priceSpan = new Span(numberFormat.format(vatPrice) + "¢");

            final ListItem item = new PriceListItem(timeSpan, priceSpan);

            setPriceTextColor(vatPrice, priceSpan);

            // Add the hover effect only for desktop browsers
            attachEvent.getUI().getPage().retrieveExtendedClientDetails(details -> {
                if (!details.isTouchDevice()) {
                    item.addClassNames(
                            Transform.Hover.SCALE_102,
                            Transition.TRANSITION
                    );
                    if (vatPrice <= cheapLimit) {
                        item.addClassNames(Background.Hover.SUCCESS_10, BorderColor.Hover.SUCCESS);
                    }
                    if (vatPrice > cheapLimit && vatPrice < expensiveLimit) {
                        item.addClassNames(Background.Hover.PRIMARY_10, BorderColor.Hover.PRIMARY);
                    }
                    if (vatPrice >= expensiveLimit) {
                        item.addClassNames(Background.Hover.ERROR_10, BorderColor.Hover.ERROR);
                    }
                }
            });

            // Current item
            if (Objects.equals(localDateTime, nowLocalDateTime)) {
                final var current = getTranslation("Current");
                Ping ping = new Ping(current, getPriceBackgroundColor(vatPrice));

                timeSpan.setText(timeSpan.getText() + " ");
                timeSpan.add(ping);
                setPriceTextColor(vatPrice, timeSpan);

                item.addClassNames(
                        getPriceBackgroundColor_10(vatPrice),
                        getPriceBorderColor(vatPrice),
                        LumoUtility.FontWeight.BOLD
                );
            } else {
                item.addClassNames(LumoUtility.BorderColor.CONTRAST_10);
            }
            list.add(item);

            // Due to the sticky position of some elements we need to scroll to the position of -2h
            if (Objects.equals(localDateTime, nowLocalDateTime.minusHours(2))) {
                currentItem = item;
            }
        }
        add(containerList);
        if (currentItem != null) {
            currentItem.scrollIntoView();
        }
    }


    private H2 createDayH2() {
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
        return day;
    }

    private static UnorderedList createUnorderedList() {
        var list = new UnorderedList();
        list.addClassNames(
                FontFamily.MONO,
                LumoUtility.ListStyleType.NONE,
                LumoUtility.Margin.Horizontal.AUTO,
                LumoUtility.Margin.Vertical.NONE,
                LumoUtility.MaxWidth.SCREEN_SMALL,
                LumoUtility.Padding.NONE
        );
        return list;
    }


    private Span createDaySpan() {
        final var daySpan = new Span();
        daySpan.addClassNames(LumoUtility.Display.FLEX, LumoUtility.JustifyContent.CENTER, LumoUtility.Padding.MEDIUM, LumoUtility.Background.BASE);
        daySpan.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL, LumoUtility.Border.BOTTOM, LumoUtility.BorderColor.CONTRAST_10, "sticky-date");
        return daySpan;
    }

}
