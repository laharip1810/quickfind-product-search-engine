package com.quickfind.search;

import java.math.BigDecimal;
import java.util.List;

/**
 * The searchable view of a product. Keeps the scoring and sorting code independent of
 * JPA, so it can be unit-tested without a database.
 */
public record SearchDocument(
        long id,
        String name,
        String description,
        String brand,
        String category,
        String parentCategory,
        List<String> colors,
        BigDecimal salePrice,
        double rating,
        int reviewCount,
        double popularity,
        boolean inStock) {

    public SearchDocument {
        colors = colors == null ? List.of() : List.copyOf(colors);
    }
}
