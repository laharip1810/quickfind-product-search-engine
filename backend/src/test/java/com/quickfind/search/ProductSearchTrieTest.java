package com.quickfind.search;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductSearchTrieTest {

    private ProductSearchTrie trie;

    @BeforeEach
    void setUp() {
        trie = new ProductSearchTrie();
        trie.insert("running shoes", SuggestionType.PHRASE, 50, null);
        trie.insert("running socks", SuggestionType.PHRASE, 30, null);
        trie.insert("running shorts", SuggestionType.PHRASE, 20, null);
        trie.insert("running jacket", SuggestionType.PHRASE, 10, null);
        trie.insert("Nike Pegasus 41 Running Shoes", SuggestionType.PRODUCT, 80, 1L);
        trie.insert("rucksack", SuggestionType.PHRASE, 5, null);
    }

    @Test
    void suggestionsForPrefixAreOrderedByWeight() {
        List<String> texts = trie.getSuggestions("run", 10).stream().map(Suggestion::text).toList();
        assertEquals(List.of("running shoes", "running socks", "running shorts", "running jacket"), texts);
    }

    @Test
    void limitKeepsOnlyTheBestK() {
        List<String> texts = trie.getSuggestions("ru", 2).stream().map(Suggestion::text).toList();
        assertEquals(List.of("running shoes", "running socks"), texts);
    }

    @Test
    void prefixIsCaseAndWhitespaceInsensitive() {
        assertEquals(4, trie.getSuggestions("  RUNNING  ", 10).size());
        assertEquals(1, trie.getSuggestions("nike   pegasus", 10).size());
    }

    @Test
    void trailingSpaceLimitsToWholeWord() {
        trie.insert("runner bag", SuggestionType.PHRASE, 1, null);
        assertEquals(5, trie.getSuggestions("runn", 10).size());
        assertEquals(4, trie.getSuggestions("running ", 10).size());
    }

    @Test
    void missingPrefixReturnsEmptyList() {
        assertTrue(trie.getSuggestions("xyz", 5).isEmpty());
        assertTrue(trie.getSuggestions("", 5).isEmpty());
        assertTrue(trie.getSuggestions(null, 5).isEmpty());
        assertTrue(trie.getSuggestions("run", 0).isEmpty());
        assertFalse(trie.searchPrefix("xyz"));
        assertTrue(trie.searchPrefix("runn"));
    }

    @Test
    void productSuggestionKeepsDisplayCaseAndProductId() {
        Suggestion s = trie.getSuggestions("nike", 1).get(0);
        assertEquals("Nike Pegasus 41 Running Shoes", s.text());
        assertEquals(SuggestionType.PRODUCT, s.type());
        assertEquals(1L, s.productId());
    }

    @Test
    void duplicateInsertMergesWeightAndHigherPriorityTypeWinsTheLabel() {
        int before = trie.size();
        trie.insert("Running Shoes", SuggestionType.CATEGORY, 100, null);
        assertEquals(before, trie.size(), "same text must not create a second term");
        Suggestion top = trie.getSuggestions("running s", 1).get(0);
        assertEquals("Running Shoes", top.text());
        assertEquals(SuggestionType.CATEGORY, top.type());
        assertEquals(150.0, top.score());
        assertNull(top.productId());
    }

    @Test
    void equalWeightsAreOrderedAlphabetically() {
        ProductSearchTrie t = new ProductSearchTrie();
        t.insert("bb", SuggestionType.PHRASE, 1, null);
        t.insert("ba", SuggestionType.PHRASE, 1, null);
        t.insert("bc", SuggestionType.PHRASE, 1, null);
        assertEquals(List.of("ba", "bb", "bc"), t.getSuggestions("b", 3).stream().map(Suggestion::text).toList());
    }

    @Test
    void containsMatchesWholeTermsOnly() {
        assertTrue(trie.contains("running shoes"));
        assertFalse(trie.contains("running"));
    }
}
