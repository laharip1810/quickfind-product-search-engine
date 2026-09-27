package com.quickfind.service;

import com.quickfind.search.SortOption;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/** A validated search request with filter names already resolved to database ids. */
public record SearchCriteria(
        String query,
        List<String> tokens,
        Set<Long> categoryIds,
        Set<Long> brandIds,
        Set<Long> colorIds,
        String productSize,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        BigDecimal minRating,
        boolean inStockOnly,
        SortOption sort,
        int page,
        int size) {

    public SearchCriteria {
        tokens = tokens == null ? List.of() : List.copyOf(tokens);
        categoryIds = categoryIds == null ? Set.of() : Set.copyOf(categoryIds);
        brandIds = brandIds == null ? Set.of() : Set.copyOf(brandIds);
        colorIds = colorIds == null ? Set.of() : Set.copyOf(colorIds);
    }

    public boolean hasQuery() {
        return !tokens.isEmpty();
    }
}
