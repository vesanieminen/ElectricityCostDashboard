package com.vesanieminen.froniusvisualizer.views;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vesanieminen.froniusvisualizer.components.BarChartTemplateTimo;
import com.vesanieminen.froniusvisualizer.components.DoubleLabel;
import com.vesanieminen.froniusvisualizer.components.MaterialIcon;
import com.vesanieminen.froniusvisualizer.components.SettingsDialog;
import com.vesanieminen.froniusvisualizer.services.Nordpool60MinSpotService;
import com.vesanieminen.froniusvisualizer.services.NordpoolSpotService;
import com.vesanieminen.froniusvisualizer.services.model.Plotline;

import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.vesanieminen.froniusvisualizer.util.Utils.calculateAverageOfDay;
import static com.vesanieminen.froniusvisualizer.util.Utils.calculateCheapest3HoursOfDay;
import static com.vesanieminen.froniusvisualizer.util.Utils.calculateCheapest3HoursOfDay_15min;
import static com.vesanieminen.froniusvisualizer.util.Utils.calculateMaximumOfDay;
import static com.vesanieminen.froniusvisualizer.util.Utils.calculateMinimumOfDay;
import static com.vesanieminen.froniusvisualizer.util.Utils.calculateSpotAveragePriceOfMonth;
import static com.vesanieminen.froniusvisualizer.util.Utils.fiLocale;
import static com.vesanieminen.froniusvisualizer.util.Utils.fiZoneID;
import static com.vesanieminen.froniusvisualizer.util.Utils.getCombinedSpotData;
import static com.vesanieminen.froniusvisualizer.util.Utils.getNumberFormat;
import static com.vesanieminen.froniusvisualizer.util.Utils.is15MinPrice;
import static com.vesanieminen.froniusvisualizer.views.MainLayout.URL_SUFFIX;

@Route(value = "porssisahkoryhma", layout = MainLayout.class)
@RouteAlias(value = "pörssisähköryhmä", layout = MainLayout.class)
@PageTitle("Market electricity group" + URL_SUFFIX)
public class ChartTemplateViewForTimo extends Main {

    private final Button previousButton;
    private final Button nextButton;
    private final BarChartTemplateTimo barChartTemplateTimo;
    private final DoubleLabel averageTodayLabel;
    private final DoubleLabel averageThisMonthLabel;
    private final DoubleLabel lowestHighestToday;
    private final DoubleLabel cheapestPeriod;
    private final NumberFormat decimalFormat;
    private final NumberFormat numberFormat;
    private final H2 dayH2;
    private final LocalDateTime dateOfLatestFullData;
    private final Checkbox showTheMonthlyAverageLineCheckbox;
    private final SettingsDialog.SettingsState settingsState;
    private LocalDateTime selectedDate;

    public ChartTemplateViewForTimo(SettingsDialog.SettingsState settingsState) {
        this.settingsState = settingsState;

        dateOfLatestFullData = is15MinPrice(settingsState) ? NordpoolSpotService.getDateOfLatestFullDayData() : Nordpool60MinSpotService.getDateOfLatestFullDayData();
        selectedDate = dateOfLatestFullData;

        final var header = new Div();
        header.addClassNames(LumoUtility.Display.FLEX, LumoUtility.JustifyContent.CENTER, LumoUtility.AlignItems.CENTER);
        add(header);

        final var icon = new Image("icons/icon.png", "Liukuri");
        icon.setMaxWidth(30, Unit.PIXELS);
        icon.setMaxHeight(30, Unit.PIXELS);
        final var span = new H2("Liukuri.fi");
        span.addClassNames(LumoUtility.FontSize.MEDIUM, LumoUtility.TextColor.SECONDARY, LumoUtility.Display.FLEX, LumoUtility.AlignItems.CENTER);
        dayH2 = new H2();
        dayH2.addClassNames(LumoUtility.FontSize.LARGE);
        final var liukuriAd = new Div(span, icon);
        liukuriAd.addClassNames(LumoUtility.Display.FLEX, LumoUtility.Gap.SMALL, LumoUtility.JustifyContent.CENTER);
        final var title = new Div(dayH2, liukuriAd);
        title.addClassNames(LumoUtility.FlexDirection.COLUMN, LumoUtility.Display.FLEX, LumoUtility.AlignItems.CENTER, LumoUtility.Margin.Top.MEDIUM);

        previousButton = new Button(MaterialIcon.CHEVRON_LEFT.create(LumoUtility.IconSize.MEDIUM));
        previousButton.addClickListener(e -> {
            if (Duration.between(selectedDate, dateOfLatestFullData).toDays() <= 5) {
                selectedDate = selectedDate.minusDays(1);
                selectDate(selectedDate);
            }
        });
        previousButton.addClassNames(LumoUtility.Padding.Horizontal.SMALL);
        previousButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        nextButton = new Button(MaterialIcon.CHEVRON_RIGHT.create(LumoUtility.IconSize.MEDIUM));
        nextButton.addClickListener(e -> {
            if (selectedDate.isBefore(dateOfLatestFullData)) {
                selectedDate = selectedDate.plusDays(1);
                selectDate(selectedDate);
            }
        });
        nextButton.addClassNames(LumoUtility.Padding.Horizontal.SMALL);
        nextButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        header.add(previousButton, title, nextButton);

        barChartTemplateTimo = new BarChartTemplateTimo();
        add(barChartTemplateTimo);

        numberFormat = getNumberFormat(getLocale(), 2);
        numberFormat.setMinimumFractionDigits(2);
        decimalFormat = getNumberFormat(getLocale(), 2);
        decimalFormat.setMinimumFractionDigits(2);

        averageTodayLabel = new DoubleLabel(getTranslation("Day's average"), "");
        averageThisMonthLabel = new DoubleLabel(getTranslation("Average this month"), "");
        lowestHighestToday = new DoubleLabel(getTranslation("Lowest / highest today"), "");
        cheapestPeriod = new DoubleLabel(getTranslation("Cheapest 3h period"), "");
        final var labelDiv = new Div(averageTodayLabel, averageThisMonthLabel, lowestHighestToday, cheapestPeriod);
        labelDiv.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexWrap.WRAP, LumoUtility.Width.FULL/*, LumoUtility.BorderRadius.LARGE, LumoUtility.Border.ALL, LumoUtility.BorderColor.CONTRAST_10*/);
        labelDiv.addClassNames(LumoUtility.Margin.Bottom.MEDIUM);
        add(labelDiv);

        showTheMonthlyAverageLineCheckbox = new Checkbox(getTranslation("Show the monthly average line"));
        showTheMonthlyAverageLineCheckbox.addValueChangeListener(e -> {
            averageThisMonthLabel.getSpanBottom().getStyle().set("border-color", e.getValue() ? "rgb(242, 182, 50)" : "");
            averageThisMonthLabel.getSpanBottom().getStyle().set("border-width", e.getValue() ? "medium" : "");
            if (e.getValue()) {
                averageThisMonthLabel.getSpanBottom().addClassNames(LumoUtility.Border.BOTTOM);
            } else {
                averageThisMonthLabel.getSpanBottom().removeClassName(LumoUtility.Border.BOTTOM);
            }
            updateLabels(selectedDate);
        });
        showTheMonthlyAverageLineCheckbox.setValue(true);

        showTheMonthlyAverageLineCheckbox.addClassNames(LumoUtility.Display.FLEX, LumoUtility.JustifyContent.CENTER);
        add(showTheMonthlyAverageLineCheckbox);

        selectDate(dateOfLatestFullData);
    }


    private void updateButtonVisibility() {
        previousButton.setEnabled(Duration.between(selectedDate, dateOfLatestFullData).toDays() <= 5);
        nextButton.setEnabled(selectedDate.isBefore(dateOfLatestFullData));
    }

    private void selectDate(LocalDateTime selectedDay) {
        final var beginning = selectedDay.atZone(fiZoneID).truncatedTo(ChronoUnit.DAYS).toInstant().toEpochMilli();
        if (is15MinPrice(settingsState)) {
            final var end = selectedDay.atZone(fiZoneID).truncatedTo(ChronoUnit.DAYS).plusDays(1).plusMinutes(45).toInstant().toEpochMilli();
            var data = NordpoolSpotService.getPriceList().stream().filter(item -> beginning <= item.time && item.time <= end).collect(Collectors.toList());
            barChartTemplateTimo.setNordpoolDataList(data);
        } else {
            final var end = selectedDay.atZone(fiZoneID).truncatedTo(ChronoUnit.DAYS).plusDays(1).toInstant().toEpochMilli();
            var data = Nordpool60MinSpotService.getPriceList().stream().filter(item -> beginning <= item.time && item.time <= end).collect(Collectors.toList());
            barChartTemplateTimo.setNordpoolDataList(data);
        }
        updateLabels(selectedDay);
        updateButtonVisibility();
    }

    private void updateLabels(LocalDateTime selectedDay) {
        final var day = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(getLocale()).format(selectedDay);
        if (fiLocale.equals(getLocale())) {
            final var weekDay = selectedDate.getDayOfWeek().getDisplayName(TextStyle.SHORT, getLocale());
            dayH2.setText("%s %s".formatted(weekDay, day));
        } else {
            dayH2.setText("%s".formatted(day));
        }
        final var combinedSpotData = getCombinedSpotData();
        averageTodayLabel.setTitleBottom(numberFormat.format(calculateAverageOfDay(selectedDay.toLocalDate(), combinedSpotData)) + " " + getTranslation("c/kWh"));
        final var monthAverage = calculateSpotAveragePriceOfMonth(selectedDay.toLocalDate(), combinedSpotData);
        if (showTheMonthlyAverageLineCheckbox.getValue()) {
            barChartTemplateTimo.setAverage(monthAverage);
            final var plotlines = List.of(new Plotline("average-yellow", monthAverage));
            barChartTemplateTimo.setPlotline(plotlines);
        } else {
            barChartTemplateTimo.setAverage(-100d);
            barChartTemplateTimo.setPlotline(new ArrayList<>());
        }
        averageThisMonthLabel.setTitleBottom(numberFormat.format(monthAverage) + " " + getTranslation("c/kWh"));
        final var min = decimalFormat.format(calculateMinimumOfDay(selectedDay.toLocalDate(), combinedSpotData));
        final var max = decimalFormat.format(calculateMaximumOfDay(selectedDay.toLocalDate(), combinedSpotData));
        lowestHighestToday.setTitleBottom(min + " / " + max);

        if (is15MinPrice(settingsState)) {
            final var cheapestHours = calculateCheapest3HoursOfDay_15min(selectedDay.toLocalDate(), NordpoolSpotService.getLatest7DaysMap());
            final var fromZdt = cheapestHours.from().atZone(fiZoneID);
            final var toZdt = cheapestHours.to().atZone(fiZoneID);
            final var fmt = DateTimeFormatter.ofPattern("HH:mm");
            cheapestPeriod.setTitleBottom(
                    "%s - %s, %s %s".formatted(
                            fromZdt.format(fmt),
                            toZdt.plusMinutes(15).format(fmt),
                            getTranslation("avg."),
                            numberFormat.format(cheapestHours.averagePrice())
                    )
            );
        } else {
            final var cheapestHours = calculateCheapest3HoursOfDay(selectedDay.toLocalDate(), Nordpool60MinSpotService.getLatest7DaysMap());
            final var from = cheapestHours.from().atZone(fiZoneID).getHour();
            final var to = cheapestHours.to().atZone(fiZoneID).getHour() + 1;
            cheapestPeriod.setTitleBottom("%s:00 - %s:00, ".formatted(from, to) + getTranslation("avg.") + " " + numberFormat.format(cheapestHours.averagePrice()));

        }
    }

}
