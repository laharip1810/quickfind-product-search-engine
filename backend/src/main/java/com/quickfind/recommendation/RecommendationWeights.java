package com.quickfind.recommendation;

/**
 * Weights of the recommendation formula. The seven signal weights sum to 1.0, so
 * a score is roughly "fraction of a perfect match", plus the optional personal bonus.
 */
public final class RecommendationWeights {

    public static final double CATEGORY = 0.30;
    public static final double BRAND = 0.15;
    public static final double PRICE = 0.15;
    public static final double CO_INTERACTION = 0.15;
    public static final double POPULARITY = 0.10;
    public static final double RATING = 0.10;
    public static final double COLOR = 0.05;

    /** Added when the candidate's subcategory is one the user engages with most. */
    public static final double USER_AFFINITY_BONUS = 0.10;

    /** Category similarity levels. */
    public static final double SAME_SUBCATEGORY = 1.0;
    public static final double COMPLEMENTARY_SUBCATEGORY = 0.7;
    public static final double SAME_PARENT_CATEGORY = 0.5;

    private RecommendationWeights() {
    }
}
