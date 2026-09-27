package com.quickfind.search;

/**
 * Field weights used by {@link RelevanceScorer}. A token found in the product name is
 * the strongest signal, a token found only in the description the weakest.
 */
public final class SearchWeights {

    public static final double NAME = 5.0;
    public static final double CATEGORY = 4.0;      // subcategory or its parent category
    public static final double BRAND = 3.0;
    public static final double COLOR = 2.0;
    public static final double DESCRIPTION = 1.0;

    /** Added when the whole multi-word query appears, in order, inside the product name. */
    public static final double PHRASE_BONUS = 3.0;

    /** Out-of-stock products are demoted rather than hidden (unless inStock=true is requested). */
    public static final double OUT_OF_STOCK_FACTOR = 0.5;

    private SearchWeights() {
    }
}
