package com.quickfind.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.oneOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SearchApiIntegrationTest extends ApiIntegrationTestBase {

    private String search(String... params) throws Exception {
        var request = get("/api/products/search");
        for (int i = 0; i < params.length; i += 2) {
            request = request.param(params[i], params[i + 1]);
        }
        return body(mockMvc.perform(request).andExpect(status().isOk()).andReturn());
    }

    private static List<Double> numbers(String json, String path) {
        List<Number> raw = JsonPath.read(json, path);
        return raw.stream().map(Number::doubleValue).toList();
    }

    @Test
    void blackRunningShoesRanksBlackRunningShoesFirst() throws Exception {
        String json = search("query", "black running shoes");
        assertThat((String) JsonPath.read(json, "$.matchMode")).isEqualTo("ALL_TERMS");
        assertThat((List<String>) JsonPath.read(json, "$.normalizedTokens")).containsExactly("black", "running", "shoe");
        List<String> subcategories = JsonPath.read(json, "$.content[*].product.subcategory");
        assertThat(subcategories).isNotEmpty();
        assertThat(subcategories.get(0)).isEqualTo("Running Shoes");
        List<String> colors = JsonPath.read(json, "$.content[0].product.colors[*].name");
        assertThat(colors).contains("Black");

        List<Double> scores = numbers(json, "$.content[*].relevanceScore");
        for (int i = 1; i < scores.size(); i++) {
            assertThat(scores.get(i)).isLessThanOrEqualTo(scores.get(i - 1));
        }
    }

    @Test
    void filtersAreAppliedTogether() throws Exception {
        String json = search("query", "running shoes", "category", "Footwear", "minPrice", "2000",
                "maxPrice", "12000", "minRating", "4");
        assertThat((Integer) JsonPath.read(json, "$.totalElements")).isPositive();
        assertThat((List<String>) JsonPath.read(json, "$.content[*].product.category")).containsOnly("Footwear");
        numbers(json, "$.content[*].product.salePrice").forEach(p -> assertThat(p).isBetween(2000.0, 12000.0));
        numbers(json, "$.content[*].product.rating").forEach(r -> assertThat(r).isGreaterThanOrEqualTo(4.0));
    }

    @Test
    void colorSizeBrandAndAvailabilityFilters() throws Exception {
        mockMvc.perform(get("/api/products/search").param("color", "Black").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].product.colors[*].name", hasItem("Black")))
                .andExpect(jsonPath("$.matchMode", is("NONE")));

        String bySize = search("productSize", "UK 9", "size", "100");
        assertThat((List<String>) JsonPath.read(bySize, "$.content[*].product.subcategory"))
                .isNotEmpty().allMatch(s -> s.equals("Running Shoes") || s.equals("Sneakers"));

        mockMvc.perform(get("/api/products/search").param("brand", "Nike").param("brand", "Puma").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].product.brand", everyItem(oneOf("Nike", "Puma"))));

        mockMvc.perform(get("/api/products/search").param("inStock", "true").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].product.inStock", everyItem(is(true))));
    }

    @Test
    void outOfStockProductsAreDemotedButOnlyHiddenWhenRequested() throws Exception {
        String all = search("query", "gel nimbus");
        assertThat((List<String>) JsonPath.read(all, "$.content[*].product.name"))
                .contains("ASICS Gel-Nimbus 26 Running Shoes");
        String inStockOnly = search("query", "gel nimbus", "inStock", "true");
        assertThat((List<String>) JsonPath.read(inStockOnly, "$.content[*].product.name"))
                .doesNotContain("ASICS Gel-Nimbus 26 Running Shoes");
    }

    @Test
    void sortingWithAndWithoutQuery() throws Exception {
        List<Double> asc = numbers(search("sort", "price_asc", "size", "50"), "$.content[*].product.salePrice");
        assertThat(asc).isSorted();

        List<Double> desc = numbers(search("query", "shoes", "sort", "price_desc", "size", "50"),
                "$.content[*].product.salePrice");
        assertThat(desc).isSortedAccordingTo((a, b) -> Double.compare(b, a));

        List<Double> ratings = numbers(search("category", "Watches", "sort", "rating"), "$.content[*].product.rating");
        assertThat(ratings).isSortedAccordingTo((a, b) -> Double.compare(b, a));
    }

    @Test
    void relevanceWithoutQueryFallsBackToPopularity() throws Exception {
        mockMvc.perform(get("/api/products/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sort", is("popularity")))
                .andExpect(jsonPath("$.content[0].relevanceScore", nullValue()));
    }

    @Test
    void paginationMetadata() throws Exception {
        mockMvc.perform(get("/api/products/search").param("query", "shoes").param("page", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", is(1)))
                .andExpect(jsonPath("$.size", is(5)))
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.totalElements", greaterThan(5)))
                .andExpect(jsonPath("$.totalPages", greaterThan(1)))
                .andExpect(jsonPath("$.tookMs", notNullValue()));
    }

    @Test
    void fallsBackToAnyTermWhenNothingMatchesEveryTerm() throws Exception {
        mockMvc.perform(get("/api/products/search").param("query", "running xylophone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchMode", is("ANY_TERM")))
                .andExpect(jsonPath("$.totalElements", greaterThan(0)));
    }

    @Test
    void noResultsIsAnEmptyPageNotAnError() throws Exception {
        mockMvc.perform(get("/api/products/search").param("query", "qwertyuiop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(0)))
                .andExpect(jsonPath("$.totalPages", is(0)))
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void invalidParametersReturnMeaningfulErrors() throws Exception {
        mockMvc.perform(get("/api/products/search").param("sort", "cheapest"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_SORT")))
                .andExpect(jsonPath("$.message", containsString("Allowed")));
        mockMvc.perform(get("/api/products/search").param("minPrice", "5000").param("maxPrice", "2000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_FILTER")));
        mockMvc.perform(get("/api/products/search").param("category", "Spaceships"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Unknown category")));
        mockMvc.perform(get("/api/products/search").param("minPrice", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.fieldErrors[0].field", is("minPrice")));
        mockMvc.perform(get("/api/products/search").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }
}
