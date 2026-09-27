package com.quickfind.api;

import com.jayway.jsonpath.JsonPath;
import com.quickfind.service.AutocompleteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SuggestionApiIntegrationTest extends ApiIntegrationTestBase {

    @Autowired
    private AutocompleteService autocompleteService;

    @Test
    void runSuggestsRunningProductTypes() throws Exception {
        String json = getJson("/api/search/suggestions?prefix=run");
        List<String> texts = JsonPath.read(json, "$.suggestions[*].text");
        assertThat(texts).hasSizeLessThanOrEqualTo(8);
        assertThat(texts.get(0)).isEqualTo("Running Shoes");
        assertThat(texts).contains("running socks", "running shorts", "running jacket");
    }

    @Test
    void brandPrefixPutsTheBrandFirst() throws Exception {
        mockMvc.perform(get("/api/search/suggestions").param("prefix", "nik").param("limit", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestions", hasSize(3)))
                .andExpect(jsonPath("$.suggestions[0].text", is("Nike")))
                .andExpect(jsonPath("$.suggestions[0].type", is("BRAND")));
    }

    @Test
    void unknownPrefixReturnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/search/suggestions").param("prefix", "zzqx"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestions", hasSize(0)));
    }

    @Test
    void invalidParameters() throws Exception {
        mockMvc.perform(get("/api/search/suggestions")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/search/suggestions").param("prefix", "  ")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/search/suggestions").param("prefix", "run").param("limit", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/search/suggestions").param("prefix", "run").param("limit", "21"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void newProductsBecomeSuggestionsAfterTheTrieRebuild() throws Exception {
        String body = productJson("Nike Zephyrine Trail Runner", brandId("Nike"), subcategoryId("Running Shoes"),
                colorId("Black"), 5, null);
        String created = body(mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn());
        long id = ((Number) JsonPath.read(created, "$.id")).longValue();

        // The change listener marked the Trie dirty; the scheduled check would rebuild it within seconds.
        autocompleteService.rebuild();
        List<String> texts = JsonPath.read(getJson("/api/search/suggestions?prefix=nike zeph"), "$.suggestions[*].text");
        assertThat(texts).containsExactly("Nike Zephyrine Trail Runner");

        mockMvc.perform(delete("/api/products/{id}", id)).andExpect(status().isNoContent());
        autocompleteService.rebuild();
        assertThat((List<String>) JsonPath.read(getJson("/api/search/suggestions?prefix=nike zeph"), "$.suggestions[*].text"))
                .isEmpty();
    }
}
