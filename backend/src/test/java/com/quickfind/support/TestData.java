package com.quickfind.support;

import com.quickfind.dto.request.ProductRequest;
import com.quickfind.entity.Brand;
import com.quickfind.entity.Category;
import com.quickfind.entity.Color;
import com.quickfind.entity.Product;

import java.math.BigDecimal;
import java.util.Set;

/** Entity and request builders for unit tests. */
public final class TestData {

    private TestData() {
    }

    public static Brand brand(long id, String name) {
        Brand brand = new Brand(name);
        brand.setId(id);
        return brand;
    }

    public static Category category(long id, String name, Category parent) {
        Category category = new Category(name, parent);
        category.setId(id);
        return category;
    }

    public static Color color(long id, String name) {
        Color color = new Color(name, "#000000");
        color.setId(id);
        return color;
    }

    public static Product product(long id, String name, Brand brand, Category category, int price, int stock) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setDescription("Test description for " + name);
        product.setBrand(brand);
        product.setCategory(category);
        product.setPrice(BigDecimal.valueOf(price));
        product.setDiscountPercent(BigDecimal.TEN);
        product.setRating(new BigDecimal("4.5"));
        product.setReviewCount(100);
        product.setStockQuantity(stock);
        product.setPopularityScore(50);
        product.replaceColors(Set.of(color(1, "Black")));
        product.replaceSizes(Set.of("UK 8", "UK 9"));
        return product;
    }

    public static ProductRequest productRequest(String name, long brandId, long categoryId, Long version) {
        return new ProductRequest(name, "A product used in tests", brandId, categoryId,
                new BigDecimal("4999.00"), new BigDecimal("10"), new BigDecimal("4.5"), 10, 25, 12.0,
                Set.of(1L), Set.of("UK 8"), null, version);
    }
}
