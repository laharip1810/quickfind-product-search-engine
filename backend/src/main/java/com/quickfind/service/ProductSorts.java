package com.quickfind.service;

import com.quickfind.search.SortOption;
import org.springframework.data.domain.Sort;

/**
 * SQL ORDER BY equivalents of {@link SortOption} for queries without a text query, where
 * sorting and pagination can be done entirely by the database (using the V2 indexes).
 */
final class ProductSorts {

    private ProductSorts() {
    }

    static Sort forOption(SortOption option) {
        return switch (option) {
            case PRICE_ASC -> Sort.by(Sort.Order.asc("salePrice"), Sort.Order.asc("id"));
            case PRICE_DESC -> Sort.by(Sort.Order.desc("salePrice"), Sort.Order.asc("id"));
            case RATING -> Sort.by(Sort.Order.desc("rating"), Sort.Order.desc("reviewCount"), Sort.Order.asc("id"));
            case RELEVANCE, POPULARITY -> byPopularity();
        };
    }

    static Sort byPopularity() {
        return Sort.by(Sort.Order.desc("popularityScore"), Sort.Order.asc("id"));
    }

    /** Relevance needs a query; without one the result is ordered by popularity, and we say so. */
    static String effectiveName(SortOption option) {
        return option == SortOption.RELEVANCE ? SortOption.POPULARITY.value() : option.value();
    }
}
