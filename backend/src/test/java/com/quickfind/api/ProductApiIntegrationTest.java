package com.quickfind.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductApiIntegrationTest extends ApiIntegrationTestBase {

    @Test
    void listIsPaginatedWithMetadata() throws Exception {
        mockMvc.perform(get("/api/products").param("page", "1").param("size", "5").param("sort", "price_asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.page", is(1)))
                .andExpect(jsonPath("$.size", is(5)))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(100)))
                .andExpect(jsonPath("$.totalPages", greaterThanOrEqualTo(20)))
                .andExpect(jsonPath("$.sort", is("price_asc")));
    }

    @Test
    void listRejectsInvalidSortAndOversizedPages() throws Exception {
        mockMvc.perform(get("/api/products").param("sort", "cheapest"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_SORT")))
                .andExpect(jsonPath("$.message", containsString("price_asc")));
        mockMvc.perform(get("/api/products").param("size", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_SEARCH_REQUEST")));
    }

    @Test
    void getReturnsDetailsAndMissingProductIs404WithoutStackTrace() throws Exception {
        long id = productIdByName("Nike Pegasus 41 Running Shoes");
        mockMvc.perform(get("/api/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brand", is("Nike")))
                .andExpect(jsonPath("$.category", is("Footwear")))
                .andExpect(jsonPath("$.subcategory", is("Running Shoes")))
                .andExpect(jsonPath("$.sizes", hasItem("UK 9")))
                .andExpect(jsonPath("$.colors[*].name", hasItem("Black")))
                .andExpect(jsonPath("$.salePrice", is(10705.5)));

        mockMvc.perform(get("/api/products/{id}", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("PRODUCT_NOT_FOUND")))
                .andExpect(jsonPath("$.path", is("/api/products/999999")))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void nonNumericIdIsABadRequest() throws Exception {
        mockMvc.perform(get("/api/products/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_REQUEST")));
    }

    @Test
    void fullLifecycleCreateUpdateRestockDelete() throws Exception {
        long brand = brandId("Puma");
        long category = subcategoryId("Running Shoes");
        long color = colorId("Black");

        MvcResult created = mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("Puma Lifecycle Test Runner", brand, category, color, 10, null)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/products/")))
                .andExpect(jsonPath("$.salePrice", is(4799.2)))
                .andExpect(jsonPath("$.inStock", is(true)))
                .andReturn();
        String json = body(created);
        long id = ((Number) JsonPath.read(json, "$.id")).longValue();
        long version = ((Number) JsonPath.read(json, "$.version")).longValue();

        // Duplicate name for the same brand
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("Puma Lifecycle Test Runner", brand, category, color, 10, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("DUPLICATE_PRODUCT")));

        // Update with the current version succeeds and bumps the version
        MvcResult updated = mockMvc.perform(put("/api/products/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("Puma Lifecycle Test Runner v2", brand, category, color, 10, version)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Puma Lifecycle Test Runner v2")))
                .andReturn();
        long newVersion = ((Number) JsonPath.read(body(updated), "$.version")).longValue();
        assertThat(newVersion).isGreaterThan(version);

        // Update with the old version is a conflict
        mockMvc.perform(put("/api/products/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("Puma Lifecycle Test Runner v3", brand, category, color, 10, version)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error", is("VERSION_CONFLICT")));

        // Stock to zero
        mockMvc.perform(patch("/api/products/{id}/stock", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stockQuantity\": 0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity", is(0)))
                .andExpect(jsonPath("$.inStock", is(false)));

        mockMvc.perform(delete("/api/products/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/products/{id}", id)).andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/products/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void createValidatesFieldsAndReportsEachError() throws Exception {
        String invalid = """
                {"name": "", "description": "x", "brandId": 1, "categoryId": 1,
                 "price": -10, "rating": 7, "stockQuantity": -1, "colorIds": []}
                """;
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("name")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("price")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("rating")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("stockQuantity")))
                .andExpect(jsonPath("$.fieldErrors[*].field", hasItem("colorIds")));
    }

    @Test
    void createRejectsUnknownBrandAndTopLevelCategory() throws Exception {
        long category = subcategoryId("Sneakers");
        long color = colorId("White");
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("Ghost Brand Sneaker", 987_654, category, color, 5, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_PRODUCT")));

        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("Top Level Sneaker", brandId("Nike"), topLevelCategoryId("Footwear"), color, 5, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("subcategory")));
    }

    @Test
    void malformedJsonIsABadRequest() throws Exception {
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_REQUEST")));
    }

    @Test
    void unsupportedMethodAndUnknownEndpoint() throws Exception {
        mockMvc.perform(patch("/api/products"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error", is("METHOD_NOT_ALLOWED")));
        mockMvc.perform(get("/api/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("RESOURCE_NOT_FOUND")));
    }

    @Test
    void trendingReturnsInStockProducts() throws Exception {
        mockMvc.perform(get("/api/products/trending").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[*].inStock", org.hamcrest.Matchers.everyItem(is(true))));
        mockMvc.perform(get("/api/products/trending").param("limit", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void priceRangeCountUsesTheBinarySearchIndex() throws Exception {
        String json = getJson("/api/products/price-range-count?category=Footwear&minPrice=2000&maxPrice=5000");
        int count = JsonPath.read(json, "$.count");
        assertThat(count).isPositive();

        int all = JsonPath.read(getJson("/api/products/price-range-count"), "$.count");
        assertThat(all).isGreaterThanOrEqualTo(count);

        mockMvc.perform(get("/api/products/price-range-count").param("minPrice", "5000").param("maxPrice", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_FILTER")));
        mockMvc.perform(get("/api/products/price-range-count").param("category", "Spaceships"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void responsesCarryARequestId() throws Exception {
        mockMvc.perform(get("/api/brands").header("X-Request-Id", "test-123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "test-123"));
    }
}
