package com.quickfind.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Body of POST /api/products and PUT /api/products/{id}.
 * Existence checks (brand, category, colors) happen in the service, since they need the database.
 */
public record ProductRequest(
        @NotBlank(message = "name is required")
        @Size(min = 3, max = 200, message = "name must be between 3 and 200 characters")
        String name,

        @NotBlank(message = "description is required")
        @Size(max = 2000, message = "description must be at most 2000 characters")
        String description,

        @NotNull(message = "brandId is required")
        @Positive(message = "brandId must be positive")
        Long brandId,

        @NotNull(message = "categoryId is required")
        @Positive(message = "categoryId must be positive")
        Long categoryId,

        @NotNull(message = "price is required")
        @DecimalMin(value = "1.00", message = "price must be at least 1.00")
        @DecimalMax(value = "10000000.00", message = "price must be at most 10000000.00")
        @Digits(integer = 8, fraction = 2, message = "price may have at most 2 decimal places")
        BigDecimal price,

        @DecimalMin(value = "0.0", message = "discountPercent must be between 0 and 90")
        @DecimalMax(value = "90.0", message = "discountPercent must be between 0 and 90")
        @Digits(integer = 2, fraction = 2, message = "discountPercent may have at most 2 decimal places")
        BigDecimal discountPercent,

        @DecimalMin(value = "0.0", message = "rating must be between 0 and 5")
        @DecimalMax(value = "5.0", message = "rating must be between 0 and 5")
        @Digits(integer = 1, fraction = 1, message = "rating may have at most 1 decimal place")
        BigDecimal rating,

        @PositiveOrZero(message = "reviewCount must be zero or positive")
        Integer reviewCount,

        @NotNull(message = "stockQuantity is required")
        @Min(value = 0, message = "stockQuantity must be zero or positive")
        @Max(value = 1_000_000, message = "stockQuantity must be at most 1000000")
        Integer stockQuantity,

        @PositiveOrZero(message = "popularityScore must be zero or positive")
        Double popularityScore,

        @NotEmpty(message = "at least one colorId is required")
        Set<@NotNull @Positive Long> colorIds,

        Set<@NotBlank @Size(max = 20, message = "size labels must be at most 20 characters") String> sizes,

        @Size(max = 500, message = "imageUrl must be at most 500 characters")
        @Pattern(regexp = "^https?://\\S+$", message = "imageUrl must be an http(s) URL")
        String imageUrl,

        /** Optional optimistic-locking check on update: the version the client last saw. */
        Long version) {
}
