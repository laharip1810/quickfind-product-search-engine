package com.quickfind.dto.request;

import com.quickfind.entity.InteractionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/interactions. userId is optional: without it the demo user is used.
 * Product events need productId; SEARCH needs query (checked in the service).
 */
public record InteractionRequest(
        @Positive(message = "userId must be positive")
        Long userId,

        @Positive(message = "productId must be positive")
        Long productId,

        @NotNull(message = "eventType is required (PRODUCT_VIEW, SEARCH, ADD_TO_CART, WISHLIST)")
        InteractionType eventType,

        @Size(max = 200, message = "query must be at most 200 characters")
        String query) {
}
