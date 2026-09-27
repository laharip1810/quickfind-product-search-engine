package com.quickfind.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InteractionApiIntegrationTest extends ApiIntegrationTestBase {

    private org.springframework.test.web.servlet.ResultActions postInteraction(String json) throws Exception {
        return mockMvc.perform(post("/api/interactions").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void recordsViewsAndSearchesForTheDemoUser() throws Exception {
        long demoUser = ((Number) JsonPath.read(getJson("/api/users/demo"), "$.id")).longValue();
        long product = productIdByName("Puma Phase Backpack");

        postInteraction("{\"productId\": " + product + ", \"eventType\": \"PRODUCT_VIEW\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId", is((int) demoUser)))
                .andExpect(jsonPath("$.productName", is("Puma Phase Backpack")))
                .andExpect(jsonPath("$.eventType", is("PRODUCT_VIEW")));

        postInteraction("{\"eventType\": \"SEARCH\", \"query\": \"  travel backpack  \"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.query", is("travel backpack")));

        List<String> recent = JsonPath.read(getJson("/api/interactions/recent?limit=5"), "$[*].eventType");
        assertThat(recent).contains("PRODUCT_VIEW", "SEARCH");
    }

    @Test
    void wishlistIsIdempotentAndRemovable() throws Exception {
        long product = productIdByName("Wildcraft Trailblazer 45L Rucksack");
        String body = "{\"productId\": " + product + ", \"eventType\": \"WISHLIST\"}";

        postInteraction(body).andExpect(status().isCreated());
        postInteraction(body).andExpect(status().isOk());

        List<Number> wishlist = JsonPath.read(getJson("/api/interactions/wishlist"), "$[*].id");
        assertThat(wishlist.stream().map(Number::longValue).filter(id -> id == product)).hasSize(1);

        mockMvc.perform(delete("/api/interactions/wishlist/{productId}", product)).andExpect(status().isNoContent());
        List<Number> after = JsonPath.read(getJson("/api/interactions/wishlist"), "$[*].id");
        assertThat(after.stream().map(Number::longValue)).doesNotContain(product);
    }

    @Test
    void validationErrors() throws Exception {
        postInteraction("{\"eventType\": \"PRODUCT_VIEW\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("productId is required for PRODUCT_VIEW events")));
        postInteraction("{\"eventType\": \"SEARCH\"}")
                .andExpect(status().isBadRequest());
        postInteraction("{\"eventType\": \"CLICK\", \"productId\": 1}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("INVALID_REQUEST")));
        postInteraction("{\"productId\": 1}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VALIDATION_FAILED")));
        postInteraction("{\"eventType\": \"PRODUCT_VIEW\", \"productId\": 999999}")
                .andExpect(status().isNotFound());
        postInteraction("{\"eventType\": \"PRODUCT_VIEW\", \"productId\": 1, \"userId\": 999999}")
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/interactions/recent").param("limit", "1000"))
                .andExpect(status().isBadRequest());
    }
}
