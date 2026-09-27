package com.quickfind.dto.response;

import java.math.BigDecimal;

/** Number of in-stock products whose sale price lies in [minPrice, maxPrice]. */
public record PriceRangeCountResponse(String category, BigDecimal minPrice, BigDecimal maxPrice, int count) {
}
