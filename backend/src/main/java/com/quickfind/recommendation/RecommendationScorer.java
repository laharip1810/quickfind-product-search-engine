package com.quickfind.recommendation;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Deterministic, explainable recommendation score for a candidate C given a source S.
 *
 * <pre>
 * score = 0.30 · categorySim     1.0 same subcategory, 0.7 complementary, 0.5 same parent
 *       + 0.15 · brandSim        1 if same brand
 *       + 0.15 · priceSim        max(0, 1 − |pS − pC| / max(pS, pC))
 *       + 0.15 · coInteraction   normalised "users who engaged with S also engaged with C"
 *       + 0.10 · popularity      log(1 + pop) / log(1 + maxPop)
 *       + 0.10 · rating          rating / 5
 *       + 0.05 · colorSim        Jaccard(colors S, colors C)
 *       + 0.10 bonus             if C's subcategory is among the user's top interests
 * </pre>
 *
 * Every component is returned in the breakdown, so the API can show why each product was
 * recommended.
 */
public class RecommendationScorer {

    public ScoreBreakdown score(RecommendationCandidate source,
                                RecommendationCandidate candidate,
                                RecommendationContext context) {
        Map<String, Double> components = new LinkedHashMap<>();
        components.put("category", RecommendationWeights.CATEGORY * categorySimilarity(source, candidate, context));
        components.put("brand", RecommendationWeights.BRAND * (source.brandId() == candidate.brandId() ? 1.0 : 0.0));
        components.put("price", RecommendationWeights.PRICE * priceSimilarity(source.salePrice(), candidate.salePrice()));
        components.put("coInteraction", RecommendationWeights.CO_INTERACTION
                * context.coInteraction().getOrDefault(candidate.id(), 0.0));
        components.put("popularity", RecommendationWeights.POPULARITY
                * normalizedPopularity(candidate.popularity(), context.maxPopularity()));
        components.put("rating", RecommendationWeights.RATING * clamp(candidate.rating() / 5.0));
        components.put("color", RecommendationWeights.COLOR * jaccard(source.colorIds(), candidate.colorIds()));
        if (context.preferredCategoryIds().contains(candidate.categoryId())) {
            components.put("userAffinity", RecommendationWeights.USER_AFFINITY_BONUS);
        }

        double total = 0.0;
        for (Map.Entry<String, Double> entry : components.entrySet()) {
            double rounded = round3(entry.getValue());
            entry.setValue(rounded);
            total += rounded;
        }
        return new ScoreBreakdown(round3(total), components);
    }

    static double categorySimilarity(RecommendationCandidate source,
                                     RecommendationCandidate candidate,
                                     RecommendationContext context) {
        if (source.categoryId() == candidate.categoryId()) {
            return RecommendationWeights.SAME_SUBCATEGORY;
        }
        if (context.complementaryCategoryIds().contains(candidate.categoryId())) {
            return RecommendationWeights.COMPLEMENTARY_SUBCATEGORY;
        }
        if (source.parentCategoryId() != null && source.parentCategoryId().equals(candidate.parentCategoryId())) {
            return RecommendationWeights.SAME_PARENT_CATEGORY;
        }
        return 0.0;
    }

    static double priceSimilarity(double sourcePrice, double candidatePrice) {
        double max = Math.max(sourcePrice, candidatePrice);
        if (max <= 0) {
            return 1.0;
        }
        return Math.max(0.0, 1.0 - Math.abs(sourcePrice - candidatePrice) / max);
    }

    static double normalizedPopularity(double popularity, double maxPopularity) {
        if (maxPopularity <= 0) {
            return 0.0;
        }
        return clamp(Math.log1p(Math.max(0, popularity)) / Math.log1p(maxPopularity));
    }

    static double jaccard(Set<Long> a, Set<Long> b) {
        if (a.isEmpty() && b.isEmpty()) {
            return 0.0;
        }
        Set<Long> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<Long> union = new HashSet<>(a);
        union.addAll(b);
        return (double) intersection.size() / union.size();
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static double round3(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
