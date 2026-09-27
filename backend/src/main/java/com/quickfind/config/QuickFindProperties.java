package com.quickfind.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.List;

/** Typed view of the "quickfind.*" settings in application.yml. */
@ConfigurationProperties(prefix = "quickfind")
public record QuickFindProperties(
        Cors cors,
        @DefaultValue("demo@quickfind.dev") String demoUserEmail,
        Seed seed,
        Cache cache,
        Search search,
        Suggestions suggestions,
        Recommendations recommendations,
        Trending trending,
        Generate generate) {

    public record Cors(List<String> allowedOrigins) {
    }

    public record Seed(@DefaultValue("true") boolean enabled) {
    }

    public record Cache(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("30m") Duration productTtl,
            @DefaultValue("10m") Duration suggestionTtl,
            @DefaultValue("10m") Duration recommendationTtl,
            @DefaultValue("5m") Duration trendingTtl,
            @DefaultValue("30s") Duration failureBackoff) {
    }

    public record Search(
            @DefaultValue("2000") int candidateLimit,
            @DefaultValue("100") int maxPageSize,
            @DefaultValue("20") int defaultPageSize) {
    }

    public record Suggestions(
            @DefaultValue("8") int defaultLimit,
            @DefaultValue("20") int maxLimit,
            @DefaultValue("10000") int maxProductTerms) {
    }

    public record Recommendations(
            @DefaultValue("8") int defaultLimit,
            @DefaultValue("20") int maxLimit,
            @DefaultValue("500") int candidateLimit,
            @DefaultValue("3") int maxPerSubcategory) {
    }

    public record Trending(
            @DefaultValue("7") int windowDays,
            @DefaultValue("20000") int maxEvents) {
    }

    public record Generate(
            @DefaultValue("10000") int count,
            @DefaultValue("1000") int batchSize) {
    }
}
