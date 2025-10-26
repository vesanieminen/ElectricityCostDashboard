package com.vesanieminen.froniusvisualizer.util;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;
import com.vesanieminen.froniusvisualizer.components.SettingsDialog;
import com.vesanieminen.froniusvisualizer.services.model.FingridLiteResponse;
import com.vesanieminen.froniusvisualizer.services.model.FingridRealtimeResponse;
import org.openjdk.jol.info.GraphLayout;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.vesanieminen.froniusvisualizer.services.NordpoolSpotService.getLatest7DaysMap;
import static com.vesanieminen.froniusvisualizer.services.PriceCalculatorService.getSpotData;

public class Utils {

    public static final NumberFormat numberFormat = NumberFormat.getInstance(Locale.FRANCE);
    public static final DecimalFormat decimalFormat = new DecimalFormat("#0.00");

    public static final ZoneId fiZoneID = ZoneId.of("Europe/Helsinki");
    public static final ZoneId nordpoolZoneID = ZoneId.of("Europe/Oslo");
    public static final ZoneId utcZone = ZoneId.of("UTC");
    public static final Locale fiLocale = new Locale("fi", "FI");
    public static final Locale enLocale = new Locale("en", "GB");
    public static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    public static final Instant vat10InstantStart = Instant.from(ZonedDateTime.of(2022, 12, 1, 0, 0, 0, 0, fiZoneID));
    public static final Instant vat10InstantEnd = Instant.from(ZonedDateTime.of(2023, 5, 1, 0, 0, 0, 0, fiZoneID));
    public static final Instant vat25_5InstantStart = Instant.from(ZonedDateTime.of(2024, 9, 1, 0, 0, 0, 0, fiZoneID));
    public static final Double vat25_5Value = 1.255d;
    public static final Double vat24Value = 1.24d;
    public static final Double vat10Value = 1.10d;

    public static boolean notNull(Object... objects) {
        return Arrays.stream(objects).allMatch(Objects::nonNull);
    }


    public static LocalDateTime getCurrentTimeWithHourPrecision() {
        final var now = LocalDateTime.now(fiZoneID);
        return now.withMinute(0).withSecond(0).withNano(0);
    }

    public static Instant getCurrentInstantHourPrecision() {
        return Instant.now().truncatedTo(ChronoUnit.HOURS);
    }

    public static Instant getCurrentInstant15MinPrecision() {
        Instant now = Instant.now();
        long epochSeconds = now.getEpochSecond();
        long secondsIn15Min = 15 * 60;

        // Floor to nearest multiple of 15 minutes
        long truncated = epochSeconds - (epochSeconds % secondsIn15Min);
        return Instant.ofEpochSecond(truncated);
    }

    public static ZonedDateTime getCurrentZonedDateTimeHourPrecision() {
        return ZonedDateTime.now(utcZone).truncatedTo(ChronoUnit.HOURS);
    }

    public static Instant getCurrentInstantMonthPrecision() {
        return Instant.now().truncatedTo(ChronoUnit.MONTHS);
    }

    public static LocalDateTime getCurrentLocalDateTimeHourPrecisionFinnishZone() {
        return Instant.now().truncatedTo(ChronoUnit.HOURS).atZone(fiZoneID).toLocalDateTime();
    }

    public static LocalDateTime getCurrentLocalDateTime15MinPrecisionFinnishZone() {
        LocalDateTime now = LocalDateTime.now(fiZoneID);
        int minute = now.getMinute();
        int quarter = (minute / 15) * 15; // round down to nearest 15
        return now.withMinute(quarter).withSecond(0).withNano(0);
    }

    public static LocalDateTime getCurrentLocalDateTimeDayPrecisionFinnishZone() {
        return Instant.now().atZone(fiZoneID).truncatedTo(ChronoUnit.DAYS).toLocalDateTime();
    }

    public static LocalDateTime getCurrentLocaleDateTimeMonthPrecisionFinnishZone() {
        return LocalDateTime.now(fiZoneID).truncatedTo(ChronoUnit.HOURS).withDayOfMonth(1).withHour(0);
    }

    public static Instant getCurrentInstantDayPrecisionFinnishZone() {
        return Instant.now().atZone(fiZoneID).truncatedTo(ChronoUnit.DAYS).toInstant();
    }

    public static Instant getCurrentInstantHourPrecisionFinnishZone() {
        return Instant.now().atZone(fiZoneID).truncatedTo(ChronoUnit.HOURS).toInstant();
    }

    public static Instant getCurrentInstant15minPrecisionFinnishZone() {
        ZonedDateTime now = Instant.now().atZone(fiZoneID);

        int minute = now.getMinute();
        int quarter = (minute / 15) * 15; // floor to nearest 15

        ZonedDateTime truncated = now
                .withMinute(quarter)
                .withSecond(0)
                .withNano(0);

        return truncated.toInstant();
    }

    public static LocalDateTime convertNordpoolLocalDateTimeToFinnish(LocalDateTime localDateTime) {
        return localDateTime.atZone(nordpoolZoneID).withZoneSameInstant(fiZoneID).toLocalDateTime();
    }

    public static String sizeOf(Object object) {
        return GraphLayout.parseInstance(object).toFootprint();
    }

    public static double[] sum(double[] first, double[] second) {
        final var array = new double[first.length];
        for (int i = 0; i < first.length; ++i) {
            array[i] = first[i] + second[i];
        }
        return array;
    }

    public static void divide(double[] array, double divisor) {
        for (int i = 0; i < array.length; ++i) {
            final var value = array[i] / divisor;
            array[i] = value;
        }
    }

    public static double[] multiply(double[] array, double multiplier) {
        final var result = new double[array.length];
        for (int i = 0; i < array.length; ++i) {
            final var value = array[i] * multiplier;
            result[i] = value;
        }
        return result;
    }

    public static String encodeUrl(String url) throws UnsupportedEncodingException {
        return URLEncoder.encode(url, StandardCharsets.UTF_8);
    }

    public static ZonedDateTime getNextHour() {
        return ZonedDateTime.now(fiZoneID).withMinute(0).withSecond(0).plusHours(1);
    }

    public static ZonedDateTime getNextMinute() {
        return ZonedDateTime.now(fiZoneID).withSecond(0).plusMinutes(1);
    }

    public static long getSecondsToNextEvenHour() {
        ZonedDateTime now = ZonedDateTime.now(Utils.fiZoneID);
        ZonedDateTime nextHour = Utils.getNextHour();
        return Duration.between(now, nextHour).getSeconds();
    }

    public static long getSecondsToNextEvenMinute() {
        ZonedDateTime now = ZonedDateTime.now(Utils.fiZoneID);
        ZonedDateTime nextMinute = Utils.getNextMinute();
        return Duration.between(now, nextMinute).getSeconds();
    }

    public static ZonedDateTime getNextTimeAt(int hour, int minute) {
        final var now = ZonedDateTime.now(fiZoneID);
        final var todayAt = ZonedDateTime.now(fiZoneID).withHour(hour).withMinute(minute);
        return now.isAfter(todayAt) ? todayAt.plusDays(1) : todayAt;
    }

    public static long getSecondsToNextTimeAt(int hour, int minute) {
        ZonedDateTime now = ZonedDateTime.now(Utils.fiZoneID);
        ZonedDateTime nextTime = Utils.getNextTimeAt(hour, minute);
        return Duration.between(now, nextTime).getSeconds();
    }

    public static boolean isAfter_13_45(ZonedDateTime zonedDateTime) {
        return zonedDateTime.isAfter(zonedDateTime.withHour(13).withMinute(45));
    }

    public static Instant getStartOfDay(int year, int month, int day) {
        return ZonedDateTime.of(year, month, day, 0, 00, 0, 0, fiZoneID).withZoneSameInstant(utcZone).toInstant();
    }

    public static Instant getEndOfDay(int year, int month, int day) {
        return ZonedDateTime.of(year, month, day, 23, 59, 0, 0, fiZoneID).withZoneSameInstant(utcZone).toInstant();
    }


    public static String format(LocalDateTime localDateTime, Locale locale) {
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(locale).format(localDateTime);
    }

    public static String format(Instant instant, Locale locale) {
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(locale).format(instant.atZone(fiZoneID));
    }

    public static NumberFormat getNumberFormat(Locale locale, int decimals) {
        final var numberFormat = NumberFormat.getInstance(locale);
        numberFormat.setMaximumFractionDigits(decimals);
        return numberFormat;
    }

    public static DecimalFormat getNumberFormatMaxTwoDecimalsWithPlusPrefix(Locale locale) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(locale);
        DecimalFormat format = new DecimalFormat("#.##", symbols);
        format.setPositivePrefix("+");
        return format;
    }

    public static double getVAT(Instant instant, boolean vat) {
        if (!vat) {
            return 1;
        }
        return getVAT(instant);
    }

    public static double getVAT(Instant instant) {
        double vat = vat24Value;
        if (0 <= instant.compareTo(vat10InstantStart) && instant.compareTo(vat10InstantEnd) < 0) {
            vat = vat10Value;
        } else if (0 <= instant.compareTo(vat25_5InstantStart)) {
            vat = vat25_5Value;
        }
        return vat;
    }

    public static OptionalDouble average(List<Double> list) {
        return list.stream().mapToDouble(d -> d).average();
    }

    // night transfer utils

    public static Predicate<Map.Entry<Instant, Double>> isBetweenHours(int startHour, int endHour) {
        return item -> startHour <= item.getKey().atZone(fiZoneID).getHour() && item.getKey().atZone(fiZoneID).getHour() < endHour;
    }

    public static Predicate<Map.Entry<Instant, Double>> isAfter(int hour) {
        return item -> hour <= item.getKey().atZone(fiZoneID).getHour();
    }

    public static Predicate<Map.Entry<Instant, Double>> isBefore(int hour) {
        return item -> item.getKey().atZone(fiZoneID).getHour() < hour;
    }

    // seasonal transfer utils

    // Predicate to check if the date is between 1st November and 31st March
    public static Predicate<Map.Entry<Instant, Double>> isBetweenNovAndMar() {
        return item -> {
            ZonedDateTime dateTime = item.getKey().atZone(fiZoneID);
            int monthValue = dateTime.getMonthValue();
            // Months from November (11) to March (3) across the year boundary
            return (monthValue >= Month.NOVEMBER.getValue() || monthValue <= Month.MARCH.getValue());
        };
    }

    // Predicate to check if the day is Monday to Saturday
    public static Predicate<Map.Entry<Instant, Double>> isMondayToSaturday() {
        return item -> {
            DayOfWeek dayOfWeek = item.getKey().atZone(fiZoneID).getDayOfWeek();
            return dayOfWeek.getValue() >= DayOfWeek.MONDAY.getValue() && dayOfWeek.getValue() <= DayOfWeek.SATURDAY.getValue();
        };
    }

    public static Predicate<Map.Entry<Instant, Double>> isAfter(int month, int year) {
        return item -> item.getKey().atZone(fiZoneID).getMonthValue() == month && item.getKey().atZone(fiZoneID).getYear() == year;
    }

    public static Predicate<Map.Entry<Instant, Double>> monthFilter(int month, int year) {
        return item -> item.getKey().atZone(fiZoneID).getMonthValue() == month && item.getKey().atZone(fiZoneID).getYear() == year;
    }

    public static Predicate<Map.Entry<Instant, Double>> dayFilter(int day, int month, int year) {
        return item -> item.getKey().atZone(fiZoneID).getDayOfMonth() == day && item.getKey().atZone(fiZoneID).getMonthValue() == month && item.getKey().atZone(fiZoneID).getYear() == year;
    }

    public static double calculateMinimumOfDay(LocalDate localDate, LinkedHashMap<Instant, Double> data) {
        final var day = localDate.getDayOfMonth();
        final var month = localDate.getMonthValue();
        final var year = localDate.getYear();
        return data.entrySet().stream().filter(dayFilter(day, month, year)).map(item -> item.getValue() * getVAT(item.getKey())).min(Comparator.comparingDouble(p -> p)).orElse(0d);
    }

    public static double calculateMaximumOfDay(LocalDate localDate, LinkedHashMap<Instant, Double> data) {
        final var day = localDate.getDayOfMonth();
        final var month = localDate.getMonthValue();
        final var year = localDate.getYear();
        return data.entrySet().stream().filter(dayFilter(day, month, year)).map(item -> item.getValue() * getVAT(item.getKey())).max(Comparator.comparingDouble(p -> p)).orElse(0d);
    }

    public static CheapestHours calculateCheapest3HoursOfDay(LocalDate localDate, LinkedHashMap<Instant, Double> data) {
        final var day = localDate.getDayOfMonth();
        final var month = localDate.getMonthValue();
        final var year = localDate.getYear();
        final var list = data.entrySet().stream().filter(dayFilter(day, month, year)).map(
                item -> Map.entry(item.getKey(), item.getValue() * getVAT(item.getKey()))
        ).toList();
        var cheapestHours = new CheapestHours(list.get(0).getKey(), list.get(2).getKey(), (list.get(0).getValue() + list.get(1).getValue() + list.get(2).getValue()) / 3);
        for (int i = 1; i < list.size() - 2; ++i) {
            final var candidate = new CheapestHours(list.get(i).getKey(), list.get(i + 2).getKey(), (list.get(i).getValue() + list.get(i + 1).getValue() + list.get(i + 2).getValue()) / 3);
            if (candidate.averagePrice() < cheapestHours.averagePrice()) {
                cheapestHours = candidate;
            }
        }
        return cheapestHours;
    }

    public static CheapestHours calculateCheapest3HoursOfDay_15min(LocalDate localDate, LinkedHashMap<Instant, Double> data) {
        final var day = localDate.getDayOfMonth();
        final var month = localDate.getMonthValue();
        final var year = localDate.getYear();
        final var list = data.entrySet().stream().filter(dayFilter(day, month, year)).map(
                item -> Map.entry(item.getKey(), item.getValue() * getVAT(item.getKey()))
        ).toList();
        var cheapestHours = new CheapestHours(list.get(0).getKey(), list.get(11).getKey(), (
                list.get(0).getValue() +
                        list.get(1).getValue() +
                        list.get(2).getValue() +
                        list.get(3).getValue() +
                        list.get(4).getValue() +
                        list.get(5).getValue() +
                        list.get(6).getValue() +
                        list.get(7).getValue() +
                        list.get(8).getValue() +
                        list.get(9).getValue() +
                        list.get(10).getValue() +
                        list.get(11).getValue()
        ) / 12);
        for (int i = 1; i < list.size() - 11; ++i) {
            final var candidate = new CheapestHours(list.get(i).getKey(), list.get(i + 11).getKey(), (
                    list.get(i).getValue() +
                            list.get(i + 1).getValue() +
                            list.get(i + 2).getValue() +
                            list.get(i + 3).getValue() +
                            list.get(i + 4).getValue() +
                            list.get(i + 5).getValue() +
                            list.get(i + 6).getValue() +
                            list.get(i + 7).getValue() +
                            list.get(i + 8).getValue() +
                            list.get(i + 9).getValue() +
                            list.get(i + 10).getValue() +
                            list.get(i + 11).getValue()
            ) / 12);
            if (candidate.averagePrice() < cheapestHours.averagePrice()) {
                cheapestHours = candidate;
            }
        }
        return cheapestHours;
    }

    public record CheapestHours(Instant from, Instant to, double averagePrice) {
    }

    public static double calculateAverageOfDay(LocalDate localDate, LinkedHashMap<Instant, Double> data) {
        final var day = localDate.getDayOfMonth();
        final var month = localDate.getMonthValue();
        final var year = localDate.getYear();
        return data.entrySet().stream().filter(dayFilter(day, month, year)).map(item -> item.getValue() * getVAT(item.getKey())).reduce(0d, Double::sum) / data.entrySet().stream().filter(dayFilter(day, month, year)).count();
    }

    public static double calculateSpotAveragePriceOfMonth(LocalDate localDate, LinkedHashMap<Instant, Double> data) {
        final var month = localDate.getMonthValue();
        final var year = localDate.getYear();
        return data.entrySet().stream().filter(monthFilter(month, year)).map(item -> item.getValue() * getVAT(item.getKey())).reduce(0d, Double::sum) / data.entrySet().stream().filter(monthFilter(month, year)).count();
    }

    public static LinkedHashMap<Instant, Double> getCombinedSpotData() {
        final var spotData = getSpotData();
        final var combinedSpotData = new LinkedHashMap<>(spotData);
        final var latest7DaysMap = getLatest7DaysMap();
        combinedSpotData.putAll(latest7DaysMap);
        return combinedSpotData;
    }

    public static boolean isDaylightSavingsInFinland() {
        return fiZoneID.getRules().isDaylightSavings(getCurrentInstantHourPrecisionFinnishZone());
    }


    public static <T> List<T> keepEveryNthItem(List<T> input, int n, int offset) {
        return IntStream.range(0, input.size()).filter(item -> item % n == 0).mapToObj(i -> input.get(i + offset)).toList();
    }

    public static <T> List<T> keepEveryNthItem(List<T> input, int n) {
        return keepEveryNthItem(input, n, 0);
    }

    public static <T> List<T> keepEveryNthItemBackwards(List<T> input, int n) {
        return keepEveryNthItem(input, n, n - 1);
    }

    public static List<FingridRealtimeResponse.Data> keepEveryFirstItem(List<FingridRealtimeResponse.Data> input) {
        return input.stream().filter(item ->
                item.startTime.getMinute() == 0 ||
                        item.startTime.getMinute() == 15 ||
                        item.startTime.getMinute() == 30 ||
                        item.startTime.getMinute() == 45
        ).collect(Collectors.toList());
    }

    public static List<FingridLiteResponse> keepEveryFirstItemLite(List<FingridLiteResponse> input) {
        return input.stream().filter(item ->
                item.startTime.getMinute() == 0 ||
                        item.startTime.getMinute() == 15 ||
                        item.startTime.getMinute() == 30 ||
                        item.startTime.getMinute() == 45
        ).collect(Collectors.toList());
    }

    public static int calculateMonthsInvolved(Instant start, Instant end) {
        final var startZone = start.atZone(fiZoneID);
        final var endZone = end.atZone(fiZoneID);

        // Convert the local dates to year-month.
        final var startYearMonth = YearMonth.from(startZone);
        final var endYearMonth = YearMonth.from(endZone);

        // Calculate the difference in months.
        return (endYearMonth.getYear() - startYearMonth.getYear()) * 12 +
                (endYearMonth.getMonthValue() - startYearMonth.getMonthValue()) + 1;
    }

    public static Optional<SettingsDialog.ZoomLevel> getZoomLevel() {
        return Optional.ofNullable((SettingsDialog.ZoomLevel) VaadinSession.getCurrent().getAttribute(SettingsDialog.ZOOM));
    }

    public static void adjustRootFontSize(double scalePercentage) {
        UI.getCurrent().getPage().executeJs(
                "document.documentElement.style.fontSize = $0 + '%';",
                scalePercentage
        );
    }

    public static boolean is15MinPrice(SettingsDialog.SettingsState settingsState) {
        return SettingsDialog.PriceResolution.QUARTER_RESOLUTION.equals(settingsState.getSettings().getPriceResolution());
    }

}
