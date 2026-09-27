package com.quickfind.dto;

import com.quickfind.dto.request.ProductRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ProductRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    static ProductRequest valid() {
        return new ProductRequest("Nike Test Runner", "A test running shoe", 1L, 2L,
                new BigDecimal("4999.00"), new BigDecimal("10"), new BigDecimal("4.5"), 10, 25, 12.0,
                Set.of(1L), Set.of("UK 8"), null, null);
    }

    private Set<String> invalidFields(ProductRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    @Test
    void validRequestHasNoViolations() {
        assertThat(validator.validate(valid())).isEmpty();
    }

    @Test
    void rejectsBlankAndShortNames() {
        ProductRequest v = valid();
        assertThat(invalidFields(new ProductRequest(" ", v.description(), v.brandId(), v.categoryId(), v.price(),
                v.discountPercent(), v.rating(), v.reviewCount(), v.stockQuantity(), v.popularityScore(),
                v.colorIds(), v.sizes(), null, null))).contains("name");
        assertThat(invalidFields(new ProductRequest("ab", v.description(), v.brandId(), v.categoryId(), v.price(),
                v.discountPercent(), v.rating(), v.reviewCount(), v.stockQuantity(), v.popularityScore(),
                v.colorIds(), v.sizes(), null, null))).contains("name");
    }

    @Test
    void rejectsInvalidPriceRatingStockAndDiscount() {
        ProductRequest v = valid();
        ProductRequest bad = new ProductRequest(v.name(), v.description(), v.brandId(), v.categoryId(),
                new BigDecimal("-5"), new BigDecimal("95"), new BigDecimal("5.5"), -1, -3, v.popularityScore(),
                v.colorIds(), v.sizes(), null, null);
        assertThat(invalidFields(bad)).contains("price", "discountPercent", "rating", "reviewCount", "stockQuantity");
    }

    @Test
    void requiresBrandCategoryAndColors() {
        ProductRequest v = valid();
        ProductRequest bad = new ProductRequest(v.name(), v.description(), null, null, v.price(),
                v.discountPercent(), v.rating(), v.reviewCount(), v.stockQuantity(), v.popularityScore(),
                Set.of(), v.sizes(), null, null);
        assertThat(invalidFields(bad)).contains("brandId", "categoryId", "colorIds");
    }

    @Test
    void imageUrlMustBeHttp() {
        ProductRequest v = valid();
        ProductRequest bad = new ProductRequest(v.name(), v.description(), v.brandId(), v.categoryId(), v.price(),
                v.discountPercent(), v.rating(), v.reviewCount(), v.stockQuantity(), v.popularityScore(),
                v.colorIds(), v.sizes(), "javascript:alert(1)", null);
        assertThat(invalidFields(bad)).contains("imageUrl");
    }
}
