package com.quickfind.support;

import com.quickfind.config.QuickFindProperties;

import java.time.Duration;
import java.util.List;

/** QuickFindProperties with the same values as application.yml, for plain unit tests. */
public final class TestProperties {

    private TestProperties() {
    }

    public static QuickFindProperties defaults() {
        return new QuickFindProperties(
                new QuickFindProperties.Cors(List.of("http://localhost:5173")),
                "demo@quickfind.dev",
                new QuickFindProperties.Seed(true),
                new QuickFindProperties.Cache(true, Duration.ofMinutes(30), Duration.ofMinutes(10),
                        Duration.ofMinutes(10), Duration.ofMinutes(5), Duration.ofSeconds(30)),
                new QuickFindProperties.Search(2000, 100, 20),
                new QuickFindProperties.Suggestions(8, 20, 10000),
                new QuickFindProperties.Recommendations(8, 20, 500, 3),
                new QuickFindProperties.Trending(7, 20000),
                null);
    }
}
