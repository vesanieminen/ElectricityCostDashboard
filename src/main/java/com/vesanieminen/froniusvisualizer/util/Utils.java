package com.vesanieminen.froniusvisualizer.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Objects;

public class Utils {

    public static boolean notNull(Object... objects) {
        return Arrays.stream(objects).allMatch(Objects::nonNull);
    }


    public static LocalDateTime getCurrentTimeWithHourPrecision() {
        final var now = LocalDateTime.now(ZoneId.of("Europe/Helsinki"));
        return now.minusMinutes(now.getMinute()).minusSeconds(now.getSecond()).minusNanos(now.getNano());
    }

}
