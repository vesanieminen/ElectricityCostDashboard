package com.vesanieminen.froniusvisualizer.util;

import java.util.Arrays;
import java.util.Objects;

public class Utils {

    public static boolean notNull(Object... objects) {
        return Arrays.stream(objects).allMatch(Objects::nonNull);
    }

}
