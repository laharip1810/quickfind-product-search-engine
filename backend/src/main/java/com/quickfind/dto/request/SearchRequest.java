package com.quickfind.dto.request;

import java.math.BigDecimal;
import java.util.List;

/**
 * Raw query parameters of GET /api/products/search, bound by Spring MVC.
 * All fields are optional; semantic validation lives in SearchRequestValidator.
 *
 * <p>{@code size} is the page size (as in /api/products?page=0&size=20), so the
 * clothing/shoe size filter is called {@code productSize}. {@code brand} may be
 * repeated (?brand=Nike&brand=Puma).
 */
public record SearchRequest(
        String query,
        String category,
        List<String> brand,
        String color,
        String productSize,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        BigDecimal minRating,
        Boolean inStock,
        String sort,
        Integer page,
        Integer size) {
}
