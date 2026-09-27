package com.quickfind.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full application (all layers, real Flyway schema, seed data) on in-memory H2 in MySQL
 * mode, with Redis replaced by the no-op cache. All API test classes share one Spring
 * context and one seeded database, so tests create their own products instead of
 * modifying seed products.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class ApiIntegrationTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String body(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString();
    }

    protected String getJson(String url) throws Exception {
        return body(mockMvc.perform(get(url).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn());
    }

    /** Id of a seed product, found through the search API by its exact name. */
    protected long productIdByName(String name) throws Exception {
        String json = getJson("/api/products/search?size=100&query=" + name.replace(" ", "+"));
        List<Map<String, Object>> hits = JsonPath.read(json, "$.content[?(@.product.name == '" + name.replace("'", "\\'") + "')]");
        if (hits.isEmpty()) {
            throw new AssertionError("Seed product not found: " + name);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> product = (Map<String, Object>) hits.get(0).get("product");
        return ((Number) product.get("id")).longValue();
    }

    protected long brandId(String name) throws Exception {
        List<Number> ids = JsonPath.read(getJson("/api/brands"), "$[?(@.name == '" + name + "')].id");
        return ids.get(0).longValue();
    }

    protected long subcategoryId(String name) throws Exception {
        List<Number> ids = JsonPath.read(getJson("/api/categories"), "$[*].subcategories[?(@.name == '" + name + "')].id");
        return ids.get(0).longValue();
    }

    protected long topLevelCategoryId(String name) throws Exception {
        List<Number> ids = JsonPath.read(getJson("/api/categories"), "$[?(@.name == '" + name + "')].id");
        return ids.get(0).longValue();
    }

    protected long colorId(String name) throws Exception {
        List<Number> ids = JsonPath.read(getJson("/api/colors"), "$[?(@.name == '" + name + "')].id");
        return ids.get(0).longValue();
    }

    /** Minimal valid product JSON body. */
    protected String productJson(String name, long brandId, long categoryId, long colorId, int stock, Long version)
            throws Exception {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("name", name);
        body.put("description", "Created by an integration test");
        body.put("brandId", brandId);
        body.put("categoryId", categoryId);
        body.put("price", 5999.00);
        body.put("discountPercent", 20);
        body.put("rating", 4.2);
        body.put("reviewCount", 3);
        body.put("stockQuantity", stock);
        body.put("colorIds", List.of(colorId));
        body.put("sizes", List.of("UK 8", "UK 9"));
        if (version != null) {
            body.put("version", version);
        }
        return objectMapper.writeValueAsString(body);
    }
}
