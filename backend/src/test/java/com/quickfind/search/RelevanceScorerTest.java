package com.quickfind.search;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.quickfind.search.SearchDocuments.doc;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelevanceScorerTest {

    private final QueryNormalizer normalizer = new QueryNormalizer();
    private final RelevanceScorer scorer = new RelevanceScorer(normalizer);

    private RelevanceScore score(String query, SearchDocument doc) {
        return scorer.score(normalizer.normalizeQuery(query), doc);
    }

    @Test
    void nameMatchOutranksCategoryWhichOutranksBrandWhichOutranksDescription() {
        SearchDocument inName = doc(1, "Trail Runner", "Acme", "Misc", "Other", List.of(), "", true);
        SearchDocument inCategory = doc(2, "Model X", "Acme", "Trail Gear", "Other", List.of(), "", true);
        SearchDocument inBrand = doc(3, "Model Y", "Trail Co", "Misc", "Other", List.of(), "", true);
        SearchDocument inDescription = doc(4, "Model Z", "Acme", "Misc", "Other", List.of(), "great on the trail", true);

        assertEquals(SearchWeights.NAME, score("trail", inName).score());
        assertEquals(SearchWeights.CATEGORY, score("trail", inCategory).score());
        assertEquals(SearchWeights.BRAND, score("trail", inBrand).score());
        assertEquals(SearchWeights.DESCRIPTION, score("trail", inDescription).score());
    }

    @Test
    void blackRunningShoesExample() {
        SearchDocument pegasus = doc(1, "Nike Pegasus 41 Running Shoes", "Nike", "Running Shoes", "Footwear",
                List.of("Black", "White"), "Responsive foam for daily runs", true);
        // running: name 5 + category 4 = 9; shoe: name 5 + category 4 = 9; black: color 2
        // raw 20 x coverage 1.0; no phrase bonus because "black running shoe" is not in the name
        RelevanceScore score = score("black running shoes", pegasus);
        assertEquals(20.0, score.score());
        assertTrue(score.matchesAllTokens());
    }

    @Test
    void wholeQueryInNameEarnsPhraseBonus() {
        SearchDocument pegasus = doc(1, "Nike Pegasus 41 Running Shoes", "Nike", "Running Shoes", "Footwear",
                List.of("Black"), "", true);
        SearchDocument scrambled = doc(2, "Nike Shoes for Running", "Nike", "Running Shoes", "Footwear",
                List.of("Black"), "", true);
        // 9 + 9 = 18, plus 3 for the phrase "running shoe" appearing in order in the name
        assertEquals(18.0 + SearchWeights.PHRASE_BONUS, score("running shoes", pegasus).score());
        assertEquals(18.0, score("running shoes", scrambled).score());
    }

    @Test
    void matchingAllWordsBeatsMatchingOneWordStrongly() {
        SearchDocument allWords = doc(1, "Velocity Running Shoes", "Puma", "Running Shoes", "Footwear",
                List.of("Black"), "", true);
        SearchDocument oneWordManyFields = doc(2, "Shoe Care Kit Shoe", "Shoe Co", "Shoe Accessories", "Shoes",
                List.of("White"), "for every shoe", true);
        double full = score("black running shoes", allWords).score();
        double partial = score("black running shoes", oneWordManyFields).score();
        assertTrue(full > partial, "full=" + full + " partial=" + partial);
    }

    @Test
    void partialWordMatchesAsPrefix() {
        SearchDocument doc = doc(1, "Nike Pegasus Running Shoes", "Nike", "Running Shoes", "Footwear",
                List.of(), "", true);
        assertTrue(score("runn", doc).matchesAnyToken());
        assertFalse(score("unning", doc).matchesAnyToken(), "matches must start at a word boundary");
    }

    @Test
    void outOfStockProductsAreDemotedNotRemoved() {
        SearchDocument inStock = doc(1, "Running Shoes", "Nike", "Running Shoes", "Footwear", List.of(), "", true);
        SearchDocument outOfStock = doc(2, "Running Shoes", "Nike", "Running Shoes", "Footwear", List.of(), "", false);
        double a = score("running shoes", inStock).score();
        double b = score("running shoes", outOfStock).score();
        assertEquals(a * SearchWeights.OUT_OF_STOCK_FACTOR, b, 0.001);
        assertTrue(b > 0);
    }

    @Test
    void noMatchGivesZeroAndReportsCoverage() {
        SearchDocument doc = doc(1, "Leather Wallet", "Fossil", "Bags", "Accessories", List.of("Brown"), "", true);
        RelevanceScore score = score("running shoes", doc);
        assertEquals(0.0, score.score());
        assertFalse(score.matchesAnyToken());
        assertEquals(2, score.totalTokens());
    }

    @Test
    void emptyQueryScoresZero() {
        SearchDocument doc = doc(1, "Anything", "B", "C", "P", List.of(), "", true);
        assertEquals(0.0, scorer.score(List.of(), doc).score());
    }
}
