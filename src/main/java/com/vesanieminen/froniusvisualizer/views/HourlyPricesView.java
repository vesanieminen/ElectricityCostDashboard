package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vesanieminen.froniusvisualizer.components.CssGrid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.getHourlyAveragePrices;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.spotDataEnd;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.spotDataStart;
import static com.vesanieminen.froniusvisualizer.util.Utils.fiZoneID;
import static com.vesanieminen.froniusvisualizer.views.MainLayout.URL_SUFFIX;

@PageTitle("Hourly Prices" + URL_SUFFIX)
@Route(value = "tuntihinnat", layout = MainLayout.class)
public class HourlyPricesView extends Main {

    public HourlyPricesView(PreservedState preservedState) {
        addClassNames(LumoUtility.Overflow.AUTO, LumoUtility.Padding.Horizontal.SMALL, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);
        // Added fix for iOS Safari header height that changes when scrolling
        setHeight("var(--fullscreen-height)");

        final var fromDatePicker = new DatePicker(getTranslation("Start period"));
        fromDatePicker.setWeekNumbersVisible(true);
        fromDatePicker.setI18n(new DatePicker.DatePickerI18n().setFirstDayOfWeek(1));
        fromDatePicker.setRequiredIndicatorVisible(true);
        fromDatePicker.setLocale(getLocale());
        fromDatePicker.setMin(spotDataStart.atZone(fiZoneID).toLocalDate());
        fromDatePicker.setMax(spotDataEnd.atZone(fiZoneID).toLocalDate());
        preservedState.selection.setStartDate(spotDataEnd.atZone(fiZoneID).minusWeeks(1).toLocalDate());
        final var datePickerI18n = new DatePicker.DatePickerI18n();
        datePickerI18n.setFirstDayOfWeek(1);
        datePickerI18n.setDateFormat("EEE dd.MM.yyyy");
        fromDatePicker.setI18n(datePickerI18n);

        final var toDatePicker = new DatePicker(getTranslation("End period"));
        toDatePicker.setWeekNumbersVisible(true);
        toDatePicker.setI18n(datePickerI18n);
        toDatePicker.setRequiredIndicatorVisible(true);
        toDatePicker.setLocale(getLocale());
        toDatePicker.setMin(spotDataStart.atZone(fiZoneID).toLocalDate());
        toDatePicker.setMax(spotDataEnd.atZone(fiZoneID).toLocalDate());
        preservedState.selection.setEndDate(spotDataEnd.atZone(fiZoneID).toLocalDate());

        final var binder = new Binder<Selection>();
        binder.bind(fromDatePicker, Selection::getStartDate, Selection::setStartDate);
        binder.bind(toDatePicker, Selection::getEndDate, Selection::setEndDate);
        binder.setBean(preservedState.selection);

        binder.addValueChangeListener(e -> {
            final var hourlyAveragePrices = getHourlyAveragePrices(fromDatePicker.getValue().atStartOfDay(fiZoneID).toInstant(), toDatePicker.getValue().atStartOfDay(fiZoneID).toInstant(), true);
        });

        final var cssGrid = new CssGrid(fromDatePicker, toDatePicker);
        add(cssGrid);
    }

    @Setter
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    static class Selection {
        LocalDate startDate;
        LocalDate endDate;
    }

    @VaadinSessionScope
    @Component
    public static class PreservedState {
        Selection selection = new Selection();
    }

}
