package com.vesanieminen.froniusvisualizer.util;

import org.openjdk.jol.info.GraphLayout;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

public class Utils {

    public static final NumberFormat numberFormat = NumberFormat.getInstance(Locale.FRANCE);
    public static final DecimalFormat decimalFormat = new DecimalFormat("#0.00");
    public static final ZoneId fiZoneID = ZoneId.of("Europe/Helsinki");
    public static final ZoneId nordpoolZoneID = ZoneId.of("Europe/Oslo");
    public static final Locale fiLocale = new Locale("fi", "FI");

    public static boolean notNull(Object... objects) {
        return Arrays.stream(objects).allMatch(Objects::nonNull);
    }


    public static LocalDateTime getCurrentTimeWithHourPrecision() {
        final var now = LocalDateTime.now(ZoneId.of("Europe/Helsinki"));
        return now.minusMinutes(now.getMinute()).minusSeconds(now.getSecond()).minusNanos(now.getNano());
    }

    public static LocalDateTime convertNordpoolLocalDateTimeToFinnish(LocalDateTime dataLocalDataTime) {
        return dataLocalDataTime.atZone(nordpoolZoneID).withZoneSameInstant(fiZoneID).toLocalDateTime();
    }

    public static void printSizeOf(Object object) {
        System.out.println(GraphLayout.parseInstance(object).toFootprint());
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

}
