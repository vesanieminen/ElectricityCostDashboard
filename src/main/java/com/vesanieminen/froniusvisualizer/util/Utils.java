package com.vesanieminen.froniusvisualizer.util;

import org.openjdk.jol.info.GraphLayout;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.OptionalDouble;

import static com.vesanieminen.froniusvisualizer.views.NordpoolspotView.vat10Value;
import static com.vesanieminen.froniusvisualizer.views.NordpoolspotView.vat24Value;

public class Utils {

    public static final NumberFormat numberFormat = NumberFormat.getInstance(Locale.FRANCE);
    public static final DecimalFormat decimalFormat = new DecimalFormat("#0.00");
    public static final DecimalFormat threeDecimals = new DecimalFormat("#0.000");

    public static final ZoneId fiZoneID = ZoneId.of("Europe/Helsinki");
    public static final ZoneId nordpoolZoneID = ZoneId.of("Europe/Oslo");
    public static final ZoneId utcZone = ZoneId.of("UTC");
    public static final Locale fiLocale = new Locale("fi", "FI");
    public static final Locale enLocale = new Locale("en", "GB");
    public static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    public static final Instant vat10Instant = Instant.from(ZonedDateTime.of(2022, 12, 1, 0, 0, 0, 0, fiZoneID));

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

    public static LocalDateTime getCurrentInstantHourPrecisionFinnishZone() {
        return Instant.now().truncatedTo(ChronoUnit.HOURS).atZone(fiZoneID).toLocalDateTime();
    }

    public static Instant getCurrentInstantDayPrecisionFinnishZone() {
        return Instant.now().atZone(fiZoneID).truncatedTo(ChronoUnit.DAYS).toInstant();
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

    public static String encodeUrl(String url) throws UnsupportedEncodingException {
        return URLEncoder.encode(url, StandardCharsets.UTF_8);
    }

    public static ZonedDateTime getNextHour() {
        return ZonedDateTime.now(fiZoneID).withMinute(0).withSecond(0).plusHours(1);
    }

    public static long getSecondsToNextEvenHour() {
        ZonedDateTime now = ZonedDateTime.now(Utils.fiZoneID);
        ZonedDateTime nextHour = Utils.getNextHour();
        return Duration.between(now, nextHour).getSeconds();
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

    public static double getVAT(Instant instant, double vat) {
        if (vat == 1) {
            return 1;
        }
        return getVAT(instant);
    }

    public static double getVAT(Instant instant) {
        return 0 <= instant.compareTo(vat10Instant) ? vat10Value : vat24Value;
    }

    public static OptionalDouble average(List<Double> list) {
        return list.stream().mapToDouble(d -> d).average();
    }

}
