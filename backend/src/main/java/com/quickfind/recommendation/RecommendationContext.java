package com.quickfind.recommendation;

import java.util.Map;
import java.util.Set;

/**
 * Everything the scorer needs besides the two products being compared.
 *
 * @param complementaryCategoryIds subcategories that go with the source's subcategory
 *                                 (for example socks with running shoes)
 * @param coInteraction            candidate id to co-interaction strength, normalised to 0..1
 * @param maxPopularity            highest popularity among the candidates (for normalisation)
 * @param preferredCategoryIds     the user's most-engaged subcategories (empty for anonymous users)
 */
public record RecommendationContext(
        Set<Long> complementaryCategoryIds,
        Map<Long, Double> coInteraction,
        double maxPopularity,
        Set<Long> preferredCategoryIds) {

    public RecommendationContext {
        complementaryCategoryIds = complementaryCategoryIds == null ? Set.of() : Set.copyOf(complementaryCategoryIds);
        coInteraction = coInteraction == null ? Map.of() : Map.copyOf(coInteraction);
        preferredCategoryIds = preferredCategoryIds == null ? Set.of() : Set.copyOf(preferredCategoryIds);
    }
}
