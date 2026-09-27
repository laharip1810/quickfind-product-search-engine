package com.quickfind.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CatalogApiIntegrationTest extends ApiIntegrationTestBase {

    @Test
    void categoriesAreATwoLevelTree() throws Exception {
        String json = getJson("/api/categories");
        assertThat((List<String>) JsonPath.read(json, "$[*].name")).containsExactly("Accessories", "Clothing", "Footwear");
        assertThat((List<String>) JsonPath.read(json, "$[?(@.name == 'Footwear')].subcategories[*].name"))
                .containsExactlyInAnyOrder("Running Shoes", "Sneakers");
    }

    @Test
    void brandsColorsSizesAndDemoUser() throws Exception {
        mockMvc.perform(get("/api/brands")).andExpect(status().isOk()).andExpect(jsonPath("$[0].name", is("Adidas")));
        mockMvc.perform(get("/api/colors")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(16)));
        List<String> sizes = JsonPath.read(getJson("/api/sizes"), "$");
        assertThat(sizes).containsSubsequence("S", "M", "L", "UK 6", "UK 11", "One Size");
        mockMvc.perform(get("/api/users/demo")).andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("demo@quickfind.dev")));
    }
}
