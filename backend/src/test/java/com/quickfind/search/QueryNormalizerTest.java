package com.quickfind.search;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryNormalizerTest {

    private final QueryNormalizer normalizer = new QueryNormalizer();

    @Test
    void lowercasesTrimsAndCollapsesWhitespace() {
        assertEquals(List.of("black", "running", "shoe"), normalizer.normalizeQuery("   Black    RUNNING\tShoes  "));
    }

    @Test
    void stripsPunctuationAndApostrophes() {
        assertEquals(List.of("levis", "511"), normalizer.normalizeQuery("Levi's 511!!"));
        assertEquals(List.of("dri", "fit"), normalizer.normalizeQuery("dri-fit"));
    }

    @Test
    void removesStopwordsAndSingleLetters() {
        assertEquals(List.of("shoe", "running"), normalizer.normalizeQuery("shoes for the running a"));
    }

    @Test
    void foldsCommonPlurals() {
        assertEquals("shoe", QueryNormalizer.singularize("shoes"));
        assertEquals("watch", QueryNormalizer.singularize("watches"));
        assertEquals("accessory", QueryNormalizer.singularize("accessories"));
        assertEquals("glass", QueryNormalizer.singularize("glasses"));
        assertEquals("dress", QueryNormalizer.singularize("dress"));
        assertEquals("bus", QueryNormalizer.singularize("bus"));
        assertEquals("jean", QueryNormalizer.singularize("jeans"));
    }

    @Test
    void deduplicatesQueryTokensButKeepsOrder() {
        assertEquals(List.of("running", "shoe"), normalizer.normalizeQuery("running shoes running shoe"));
    }

    @Test
    void fieldTokenizationKeepsDuplicatesForPhraseMatching() {
        assertEquals(List.of("run", "run"), normalizer.tokenize("run run"));
    }

    @Test
    void blankOrSymbolOnlyInputGivesNoTokens() {
        assertTrue(normalizer.normalizeQuery(null).isEmpty());
        assertTrue(normalizer.normalizeQuery("   ").isEmpty());
        assertTrue(normalizer.normalizeQuery("!!! ??").isEmpty());
    }
}
