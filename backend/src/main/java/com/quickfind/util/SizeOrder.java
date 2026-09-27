package com.quickfind.util;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Orders size labels the way a shopper expects: XS, S, M, L, XL, then numeric sizes
 * ("UK 7" before "UK 10", "30" before "32"), then anything else alphabetically, with
 * "One Size" last.
 */
public final class SizeOrder {

    private static final List<String> LETTER_SIZES = List.of("XXS", "XS", "S", "M", "L", "XL", "XXL", "XXXL");

    public static final Comparator<String> COMPARATOR = Comparator
            .comparingInt(SizeOrder::group)
            .thenComparingDouble(SizeOrder::numericValue)
            .thenComparing(String.CASE_INSENSITIVE_ORDER);

    private SizeOrder() {
    }

    private static int group(String size) {
        String upper = size.toUpperCase(Locale.ROOT).trim();
        if (LETTER_SIZES.contains(upper)) {
            return 0;
        }
        if (!Double.isNaN(parseNumber(upper))) {
            return 1;
        }
        if (upper.equals("ONE SIZE")) {
            return 3;
        }
        return 2;
    }

    private static double numericValue(String size) {
        String upper = size.toUpperCase(Locale.ROOT).trim();
        int letterIndex = LETTER_SIZES.indexOf(upper);
        if (letterIndex >= 0) {
            return letterIndex;
        }
        double number = parseNumber(upper);
        return Double.isNaN(number) ? 0 : number;
    }

    /** Extracts the number from "UK 9", "32" or "EU 42.5"; NaN if there is none. */
    private static double parseNumber(String size) {
        String digits = size.replaceAll("[^0-9.]", "");
        if (digits.isEmpty() || digits.equals(".")) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(digits);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
}
