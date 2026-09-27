package com.quickfind.recommendation;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecommendationScorerTest {

    private static final long RUNNING_SHOES = 10;
    private static final long SNEAKERS = 11;
    private static final long SOCKS = 20;
    private static final long WATCHES = 30;
    private static final long FOOTWEAR = 1;
    private static final long ACCESSORIES = 3;
    private static final long NIKE = 100;
    private static final long CASIO = 200;

    private final RecommendationScorer scorer = new RecommendationScorer();

    private static RecommendationCandidate product(long id, long category, long parent, long brand, double price,
                                                   Set<Long> colors, double popularity, double rating) {
        return new RecommendationCandidate(id, category, parent, brand, price, colors, popularity, rating);
    }

    private final RecommendationCandidate pegasus = product(1, RUNNING_SHOES, FOOTWEAR, NIKE, 10_000, Set.of(1L, 2L), 80, 4.6);

    private RecommendationContext context(Map<Long, Double> co, Set<Long> preferred) {
        return new RecommendationContext(Set.of(SOCKS), co, 100, preferred);
    }

    @Test
    void identicalProfileScoresTheMaximumContentSimilarity() {
        RecommendationCandidate twin = product(2, RUNNING_SHOES, FOOTWEAR, NIKE, 10_000, Set.of(1L, 2L), 100, 5.0);
        ScoreBreakdown b = scorer.score(pegasus, twin, context(Map.of(2L, 1.0), Set.of()));
        assertEquals(1.0, b.total(), 1e-9);
        assertEquals(0.30, b.components().get("category"));
        assertEquals(0.15, b.components().get("brand"));
        assertEquals(0.15, b.components().get("price"));
    }

    @Test
    void sameBrandComplementBeatsUnrelatedProduct() {
        RecommendationCandidate nikeSocks = product(2, SOCKS, ACCESSORIES, NIKE, 1_200, Set.of(2L), 50, 4.5);
        RecommendationCandidate casioWatch = product(3, WATCHES, ACCESSORIES, CASIO, 10_000, Set.of(), 50, 4.5);
        double socks = scorer.score(pegasus, nikeSocks, context(Map.of(), Set.of())).total();
        double watch = scorer.score(pegasus, casioWatch, context(Map.of(), Set.of())).total();
        assertTrue(socks > watch, "socks=" + socks + " watch=" + watch);
    }

    @Test
    void categorySimilarityLevels() {
        RecommendationContext ctx = context(Map.of(), Set.of());
        assertEquals(1.0, RecommendationScorer.categorySimilarity(pegasus,
                product(2, RUNNING_SHOES, FOOTWEAR, CASIO, 1, Set.of(), 0, 0), ctx));
        assertEquals(0.7, RecommendationScorer.categorySimilarity(pegasus,
                product(3, SOCKS, ACCESSORIES, CASIO, 1, Set.of(), 0, 0), ctx));
        assertEquals(0.5, RecommendationScorer.categorySimilarity(pegasus,
                product(4, SNEAKERS, FOOTWEAR, CASIO, 1, Set.of(), 0, 0), ctx));
        assertEquals(0.0, RecommendationScorer.categorySimilarity(pegasus,
                product(5, WATCHES, ACCESSORIES, CASIO, 1, Set.of(), 0, 0), ctx));
    }

    @Test
    void priceSimilarityIsRelative() {
        assertEquals(1.0, RecommendationScorer.priceSimilarity(1000, 1000));
        assertEquals(0.5, RecommendationScorer.priceSimilarity(1000, 2000));
        assertEquals(0.0, RecommendationScorer.priceSimilarity(0, 5000));
        assertEquals(1.0, RecommendationScorer.priceSimilarity(0, 0));
    }

    @Test
    void jaccardColorSimilarity() {
        assertEquals(1.0 / 3.0, RecommendationScorer.jaccard(Set.of(1L, 2L), Set.of(2L, 3L)), 1e-9);
        assertEquals(0.0, RecommendationScorer.jaccard(Set.of(), Set.of()));
    }

    @Test
    void popularityIsLogNormalised() {
        assertEquals(1.0, RecommendationScorer.normalizedPopularity(100, 100), 1e-9);
        assertEquals(0.0, RecommendationScorer.normalizedPopularity(0, 100), 1e-9);
        assertEquals(0.0, RecommendationScorer.normalizedPopularity(10, 0), 1e-9);
        assertTrue(RecommendationScorer.normalizedPopularity(10, 100) > 0.5, "log scale rewards the long tail");
    }

    @Test
    void userAffinityAddsBonusOnlyForPreferredCategories() {
        RecommendationCandidate socks = product(2, SOCKS, ACCESSORIES, NIKE, 1_200, Set.of(), 50, 4.5);
        ScoreBreakdown without = scorer.score(pegasus, socks, context(Map.of(), Set.of()));
        ScoreBreakdown with = scorer.score(pegasus, socks, context(Map.of(), Set.of(SOCKS)));
        assertFalse(without.components().containsKey("userAffinity"));
        assertEquals(RecommendationWeights.USER_AFFINITY_BONUS, with.total() - without.total(), 1e-9);
    }

    @Test
    void coInteractionContributesProportionally() {
        RecommendationCandidate socks = product(2, SOCKS, ACCESSORIES, NIKE, 1_200, Set.of(), 50, 4.5);
        double none = scorer.score(pegasus, socks, context(Map.of(), Set.of())).total();
        double half = scorer.score(pegasus, socks, context(Map.of(2L, 0.5), Set.of())).total();
        assertEquals(RecommendationWeights.CO_INTERACTION * 0.5, half - none, 1e-3);
    }

    @Test
    void signalWeightsSumToOne() {
        double sum = RecommendationWeights.CATEGORY + RecommendationWeights.BRAND + RecommendationWeights.PRICE
                + RecommendationWeights.CO_INTERACTION + RecommendationWeights.POPULARITY
                + RecommendationWeights.RATING + RecommendationWeights.COLOR;
        assertEquals(1.0, sum, 1e-9);
    }
}
