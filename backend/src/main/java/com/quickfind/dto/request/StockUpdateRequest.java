package com.quickfind.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockUpdateRequest(
        @NotNull(message = "stockQuantity is required")
        @Min(value = 0, message = "stockQuantity must be zero or positive")
        @Max(value = 1_000_000, message = "stockQuantity must be at most 1000000")
        Integer stockQuantity) {
}
