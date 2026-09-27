package com.quickfind.config;

import com.quickfind.recommendation.RecommendationScorer;
import com.quickfind.search.QueryNormalizer;
import com.quickfind.search.RelevanceScorer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/**
 * Registers the framework-free algorithm classes as beans, so services receive them by
 * constructor injection while the classes themselves stay plain Java.
 */
@Configuration
@EnableScheduling
public class AppConfig {

    @Bean
    public QueryNormalizer queryNormalizer() {
        return new QueryNormalizer();
    }

    @Bean
    public RelevanceScorer relevanceScorer(QueryNormalizer normalizer) {
        return new RelevanceScorer(normalizer);
    }

    @Bean
    public RecommendationScorer recommendationScorer() {
        return new RecommendationScorer();
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
