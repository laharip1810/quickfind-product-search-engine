package com.quickfind.entity;

/**
 * Kinds of user interaction the system records.
 * The weight expresses how strong a signal of interest each event is; it is used by
 * trending, co-interaction recommendations and popularity updates.
 */
public enum InteractionType {
    PRODUCT_VIEW(1.0, true),
    SEARCH(0.0, false),
    ADD_TO_CART(5.0, true),
    WISHLIST(3.0, true);

    private final double weight;
    private final boolean requiresProduct;

    InteractionType(double weight, boolean requiresProduct) {
        this.weight = weight;
        this.requiresProduct = requiresProduct;
    }

    public double weight() {
        return weight;
    }

    public boolean requiresProduct() {
        return requiresProduct;
    }
}
