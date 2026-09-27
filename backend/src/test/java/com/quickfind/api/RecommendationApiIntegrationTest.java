package com.quickfind.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecommendationApiIntegrationTest extends ApiIntegrationTestBase {

    @Test
    void recommendationsAreRealInStockProductsWithExplanations() throws Exception {
        long pegasus = productIdByName("Nike Pegasus 41 Running Shoes");
        String json = getJson("/api/products/" + pegasus + "/recommendations");

        List<Number> ids = JsonPath.read(json, "$.recommendations[*].product.id");
        assertThat(ids).isNotEmpty().hasSizeLessThanOrEqualTo(8);
        assertThat(ids.stream().map(Number::longValue)).doesNotContain(pegasus);
        assertThat((List<Boolean>) JsonPath.read(json, "$.recommendations[*].product.inStock")).containsOnly(true);
        assertThat((List<Object>) JsonPath.read(json, "$.recommendations[*].breakdown.category")).hasSameSizeAs(ids);
        assertThat((List<Object>) JsonPath.read(json, "$.recommendations[*].reasons")).hasSameSizeAs(ids);

        // Diversity: at most 3 per subcategory, and complementary items (not only more shoes) appear.
        List<String> subcategories = JsonPath.read(json, "$.recommendations[*].product.subcategory");
        Map<String, Integer> counts = new HashMap<>();
        subcategories.forEach(s -> counts.merge(s, 1, Integer::sum));
        assertThat(counts.values()).allMatch(c -> c <= 3);
        assertThat(subcategories).anyMatch(s -> !s.equals("Running Shoes"));

        // Seeded shoppers who viewed the Pegasus also engaged with Nike running socks.
        List<String> names = JsonPath.read(json, "$.recommendations[*].product.name");
        assertThat(names).contains("Nike Dri-FIT Everyday Running Socks");
    }

    @Test
    void personalisedRecommendationsForTheDemoUser() throws Exception {
        long userId = ((Number) JsonPath.read(getJson("/api/users/demo"), "$.id")).longValue();
        long pegasus = productIdByName("Nike Pegasus 41 Running Shoes");
        mockMvc.perform(get("/api/products/{id}/recommendations", pegasus).param("userId", String.valueOf(userId))
                        .param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is((int) userId)))
                .andExpect(jsonPath("$.recommendations.length()", is(3)));
    }

    @Test
    void errors() throws Exception {
        long pegasus = productIdByName("Nike Pegasus 41 Running Shoes");
        mockMvc.perform(get("/api/products/{id}/recommendations", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", is("PRODUCT_NOT_FOUND")));
        mockMvc.perform(get("/api/products/{id}/recommendations", pegasus).param("userId", "999999"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/products/{id}/recommendations", pegasus).param("limit", "0"))
                .andExpect(status().isBadRequest());
    }
}
