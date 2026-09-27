package com.quickfind.seed;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Guards the demo catalog against typos that would only show up at startup. */
class CatalogSeedDataTest {

    private final List<CatalogSeedData.SeedProduct> products = CatalogSeedData.products();

    @Test
    void hasAtLeastOneHundredProductsWithUniqueNames() {
        assertTrue(products.size() >= 100, "products: " + products.size());
        Set<String> names = products.stream().map(CatalogSeedData.SeedProduct::name).collect(Collectors.toSet());
        assertEquals(products.size(), names.size(), "product names must be unique");
    }

    @Test
    void everyReferenceResolves() {
        Set<String> subcategories = CatalogSeedData.CATEGORIES.values().stream()
                .flatMap(List::stream).collect(Collectors.toSet());
        for (CatalogSeedData.SeedProduct p : products) {
            assertTrue(CatalogSeedData.BRANDS.contains(p.brand()), "brand of " + p.name());
            assertTrue(subcategories.contains(p.subcategory()), "subcategory of " + p.name());
            assertFalse(p.colors().isEmpty(), "colors of " + p.name());
            p.colors().forEach(c -> assertTrue(CatalogSeedData.COLORS.containsKey(c), c + " in " + p.name()));
            assertTrue(p.name().startsWith(p.brand() + " "), "name starts with brand: " + p.name());
        }
    }

    @Test
    void valuesRespectDatabaseConstraints() {
        for (CatalogSeedData.SeedProduct p : products) {
            assertTrue(p.price() > 0, p.name());
            assertTrue(p.discountPercent() >= 0 && p.discountPercent() <= 90, p.name());
            assertTrue(p.rating() >= 0 && p.rating() <= 5, p.name());
            assertTrue(p.stock() >= 0, p.name());
            assertTrue(p.name().length() <= 200 && p.description().length() <= 2000, p.name());
            p.sizes().forEach(s -> assertTrue(s.length() <= 20, s));
        }
    }

    @Test
    void coversEveryRequiredSubcategoryAndHasOutOfStockExamples() {
        Set<String> used = products.stream().map(CatalogSeedData.SeedProduct::subcategory).collect(Collectors.toSet());
        assertTrue(used.containsAll(List.of(CatalogSeedData.RUNNING_SHOES, CatalogSeedData.SNEAKERS,
                CatalogSeedData.T_SHIRTS, CatalogSeedData.JEANS, CatalogSeedData.JACKETS, CatalogSeedData.WATCHES,
                CatalogSeedData.BAGS, CatalogSeedData.SPORTS_ACCESSORIES)));
        assertTrue(products.stream().anyMatch(p -> p.stock() == 0));
    }

    @Test
    void interactionsReferenceExistingUsersAndProducts() {
        Set<String> names = products.stream().map(CatalogSeedData.SeedProduct::name).collect(Collectors.toSet());
        Set<String> emails = new HashSet<>();
        CatalogSeedData.USERS.forEach(u -> emails.add(u.email()));
        Set<String> types = Set.of("PRODUCT_VIEW", "SEARCH", "ADD_TO_CART", "WISHLIST");
        for (CatalogSeedData.SeedInteraction i : CatalogSeedData.interactions()) {
            assertTrue(emails.contains(i.userEmail()), i.userEmail());
            assertTrue(types.contains(i.type()), i.type());
            if (i.type().equals("SEARCH")) {
                assertTrue(i.query() != null && !i.query().isBlank());
            } else {
                assertTrue(names.contains(i.productName()), "unknown product " + i.productName());
            }
        }
        assertTrue(emails.contains(CatalogSeedData.DEMO_USER_EMAIL));
    }

    @Test
    void autocompleteExampleFromTheSpecIsCovered() {
        // "run" should be able to suggest running shoes / socks / shorts / jacket
        for (String phrase : List.of("Running Shoes", "Running Socks", "Running Shorts", "Running Jacket")) {
            assertTrue(products.stream().anyMatch(p -> p.name().endsWith(phrase)), phrase);
        }
    }
}
