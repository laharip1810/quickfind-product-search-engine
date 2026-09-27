package com.quickfind.dto.response;

import java.math.BigDecimal;
import java.util.List;

/** Product card data used in lists, search results and recommendations. */
public record ProductSummaryResponse(
        Long id,
        String name,
        String brand,
        String category,
        String subcategory,
        BigDecimal price,
        BigDecimal discountPercent,
        BigDecimal salePrice,
        BigDecimal rating,
        int reviewCount,
        int stockQuantity,
        boolean inStock,
        double popularityScore,
        List<ColorResponse> colors,
        String imageUrl) {
}
