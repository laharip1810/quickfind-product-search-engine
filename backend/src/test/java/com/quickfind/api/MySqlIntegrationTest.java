package com.quickfind.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs the real Flyway migrations, Hibernate schema validation, search and the
 * co-interaction native query against MySQL 8.4 in a container, to catch differences
 * that the H2-based tests cannot. Skipped automatically when Docker is not available.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MySqlIntegrationTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void migrationsSeedSearchAndRecommendationsWorkOnMySql() throws Exception {
        String search = mockMvc.perform(get("/api/products/search").param("query", "black running shoes"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<String> subcategories = JsonPath.read(search, "$.content[*].product.subcategory");
        assertThat(subcategories).isNotEmpty();
        assertThat(subcategories.get(0)).isEqualTo("Running Shoes");

        Number id = JsonPath.read(search, "$.content[0].product.id");
        String recommendations = mockMvc.perform(get("/api/products/{id}/recommendations", id.longValue()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat((List<Object>) JsonPath.read(recommendations, "$.recommendations")).isNotEmpty();

        mockMvc.perform(get("/api/products/trending")).andExpect(status().isOk());
        mockMvc.perform(get("/api/search/suggestions").param("prefix", "run")).andExpect(status().isOk());
    }
}
