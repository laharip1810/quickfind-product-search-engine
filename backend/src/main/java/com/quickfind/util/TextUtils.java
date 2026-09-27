package com.quickfind.util;

public final class TextUtils {

    private TextUtils() {
    }

    /** Trims and collapses internal whitespace; returns null for null or blank input. */
    public static String clean(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim().replaceAll("\\s+", " ");
        return cleaned.isEmpty() ? null : cleaned;
    }

    public static double round(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }
}
