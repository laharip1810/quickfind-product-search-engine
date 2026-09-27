package com.quickfind.search;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Supported sort orders. Every comparator ends with the product id, so equal keys have a
 * stable order and pagination never shows the same product on two pages.
 * Java's List.sort is a stable TimSort: O(n log n).
 */
public enum SortOption {

    RELEVANCE("relevance",
            Comparator.comparingDouble(RankedDocument::score).reversed()
                    .thenComparing(Comparator.comparingDouble((RankedDocument r) -> r.document().popularity()).reversed())),
    PRICE_ASC("price_asc",
            Comparator.comparing((RankedDocument r) -> r.document().salePrice())),
    PRICE_DESC("price_desc",
            Comparator.comparing((RankedDocument r) -> r.document().salePrice()).reversed()),
    RATING("rating",
            Comparator.comparingDouble((RankedDocument r) -> r.document().rating()).reversed()
                    .thenComparing(Comparator.comparingInt((RankedDocument r) -> r.document().reviewCount()).reversed())),
    POPULARITY("popularity",
            Comparator.comparingDouble((RankedDocument r) -> r.document().popularity()).reversed());

    private final String value;
    private final Comparator<RankedDocument> comparator;

    SortOption(String value, Comparator<RankedDocument> primary) {
        this.value = value;
        this.comparator = primary.thenComparingLong(r -> r.document().id());
    }

    public String value() {
        return value;
    }

    public Comparator<RankedDocument> comparator() {
        return comparator;
    }

    /** Blank means "use the default"; an unknown value yields empty so the caller can report it. */
    public static Optional<SortOption> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.of(RELEVANCE);
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(o -> o.value.equals(normalized)).findFirst();
    }

    public static String allowedValues() {
        return Arrays.stream(values()).map(SortOption::value).collect(Collectors.joining(", "));
    }
}
