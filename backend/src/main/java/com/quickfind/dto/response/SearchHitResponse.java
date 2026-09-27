package com.quickfind.dto.response;

/** A search result: the product plus the relevance score that ranked it (null when browsing without a query). */
public record SearchHitResponse(ProductSummaryResponse product, Double relevanceScore) {
}
