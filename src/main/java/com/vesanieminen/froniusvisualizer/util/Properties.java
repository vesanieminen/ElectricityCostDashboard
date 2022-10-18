package com.vesanieminen.froniusvisualizer.util;

public class Properties {

    private static final String fingridApiKey = "FINGRID_API_KEY";

    public static String getFingridAPIKey() {
        return isDevelopmentMode() ? System.getProperty(fingridApiKey) : System.getenv(fingridApiKey);
    }

    public static boolean isDevelopmentMode() {
        return "true".equals(System.getProperty("dev"));
    }

    public static boolean isStagingEnvironment() {
        return "true".equals(System.getenv("staging"));
    }

}
