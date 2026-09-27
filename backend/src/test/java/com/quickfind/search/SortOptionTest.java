package com.quickfind.search;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.quickfind.search.SearchDocuments.sortable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SortOptionTest {

    private static RankedDocument ranked(SearchDocument doc, double score) {
        return new RankedDocument(doc, new RelevanceScore(score, 1, 1));
    }

    private static List<Long> sortedIds(SortOption option, List<RankedDocument> docs) {
        List<RankedDocument> copy = new ArrayList<>(docs);
        copy.sort(option.comparator());
        return copy.stream().map(r -> r.document().id()).toList();
    }

    private final List<RankedDocument> docs = List.of(
            ranked(sortable(1, 3000, 4.2, 100, 50), 10),
            ranked(sortable(2, 1000, 4.8, 20, 90), 5),
            ranked(sortable(3, 2000, 4.8, 500, 10), 10),
            ranked(sortable(4, 1000, 3.9, 1000, 90), 1));

    @Test
    void relevanceSortsByScoreThenPopularityThenId() {
        assertEquals(List.of(1L, 3L, 2L, 4L), sortedIds(SortOption.RELEVANCE, docs));
    }

    @Test
    void priceAscendingBreaksTiesById() {
        assertEquals(List.of(2L, 4L, 3L, 1L), sortedIds(SortOption.PRICE_ASC, docs));
    }

    @Test
    void priceDescending() {
        assertEquals(List.of(1L, 3L, 2L, 4L), sortedIds(SortOption.PRICE_DESC, docs));
    }

    @Test
    void ratingThenReviewCount() {
        assertEquals(List.of(3L, 2L, 1L, 4L), sortedIds(SortOption.RATING, docs));
    }

    @Test
    void popularity() {
        assertEquals(List.of(2L, 4L, 1L, 3L), sortedIds(SortOption.POPULARITY, docs));
    }

    @Test
    void parsesKnownValuesCaseInsensitively() {
        assertEquals(SortOption.PRICE_ASC, SortOption.parse(" PRICE_ASC ").orElseThrow());
        assertEquals(SortOption.RELEVANCE, SortOption.parse(null).orElseThrow());
        assertEquals(SortOption.RELEVANCE, SortOption.parse("").orElseThrow());
    }

    @Test
    void rejectsUnknownValues() {
        assertTrue(SortOption.parse("cheapest").isEmpty());
        assertTrue(SortOption.allowedValues().contains("price_desc"));
    }
}
