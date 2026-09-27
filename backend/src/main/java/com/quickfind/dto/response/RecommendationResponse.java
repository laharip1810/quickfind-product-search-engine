package com.quickfind.dto.response;

import java.util.List;
import java.util.Map;

public record RecommendationResponse(Long sourceProductId, Long userId, List<Item> recommendations) {

    /**
     * @param breakdown contribution of each signal to the score
     * @param reasons   human-readable explanations such as "Same brand (Nike)"
     */
    public record Item(ProductSummaryResponse product, double score,
                       Map<String, Double> breakdown, List<String> reasons) {
    }
}
