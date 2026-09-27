package com.quickfind.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductDetailResponse(
        Long id,
        String name,
        String description,
        Long brandId,
        String brand,
        Long categoryId,
        String category,
        Long subcategoryId,
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
        List<String> sizes,
        String imageUrl,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
