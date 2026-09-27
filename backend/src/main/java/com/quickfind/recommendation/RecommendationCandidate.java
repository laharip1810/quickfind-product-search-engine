package com.quickfind.recommendation;

import java.util.Set;

/** Framework-free view of a product used for recommendation scoring. */
public record RecommendationCandidate(
        long id,
        long categoryId,
        Long parentCategoryId,
        long brandId,
        double salePrice,
        Set<Long> colorIds,
        double popularity,
        double rating) {

    public RecommendationCandidate {
        colorIds = colorIds == null ? Set.of() : Set.copyOf(colorIds);
    }
}
