package com.quickfind.recommendation;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Hand-curated "goes well with" relations between subcategories, keyed by name.
 *
 * <p>Content similarity alone would only ever recommend more of the same (running shoes
 * for running shoes). These relations let the engine surface complementary products
 * such as socks and shorts even before there is interaction data. As interactions
 * accumulate, the co-interaction signal learns such relations from real behaviour.
 * Relations are symmetric.
 */
public final class CategoryRelations {

    private static final Map<String, Set<String>> COMPLEMENTS = new HashMap<>();

    static {
        relate("Running Shoes", "Sports Accessories", "Shorts", "Track Pants", "T-Shirts");
        relate("Sneakers", "Jeans", "T-Shirts", "Jackets");
        relate("T-Shirts", "Jeans", "Shorts", "Track Pants");
        relate("Jackets", "Jeans", "T-Shirts");
        relate("Watches", "Bags");
        relate("Bags", "Sports Accessories");
        relate("Track Pants", "Sports Accessories");
    }

    private CategoryRelations() {
    }

    private static void relate(String category, String... complements) {
        for (String complement : complements) {
            link(category, complement);
            link(complement, category);
        }
    }

    private static void link(String from, String to) {
        COMPLEMENTS.computeIfAbsent(key(from), k -> new java.util.HashSet<>()).add(to);
    }

    /** Names of subcategories that complement the given subcategory (empty if none). */
    public static Set<String> complementsOf(String subcategoryName) {
        return Set.copyOf(COMPLEMENTS.getOrDefault(key(subcategoryName), Set.of()));
    }

    private static String key(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }
}
