package com.quickfind.search;

import java.util.ArrayList;
import java.util.List;

/**
 * Explainable, deterministic relevance scoring.
 *
 * <pre>
 * tokenScore(t) = 5·[t in name] + 4·[t in subcategory or category] + 3·[t in brand]
 *               + 2·[t in color]  + 1·[t in description]
 * coverage      = matchedTokens / queryTokens
 * score         = (Σ tokenScore) · coverage
 *               + 3   if the whole multi-word query appears in the name, in order
 * score         = score · 0.5 if the product is out of stock
 * </pre>
 *
 * "t in field" means some word of the normalised field starts with t, so a partially
 * typed "runn" still matches "running". Multiplying by coverage makes a product that
 * matches all three words of "black running shoes" beat one that matches a single
 * word many times.
 *
 * Complexity per document: O(q · w), where q is the number of query tokens and w the
 * number of words across the document's fields.
 */
public class RelevanceScorer {

    private final QueryNormalizer normalizer;

    public RelevanceScorer(QueryNormalizer normalizer) {
        this.normalizer = normalizer;
    }

    public RelevanceScore score(List<String> queryTokens, SearchDocument doc) {
        if (queryTokens == null || queryTokens.isEmpty()) {
            return RelevanceScore.none(0);
        }
        List<String> nameWords = normalizer.tokenize(doc.name());
        List<String> categoryWords = new ArrayList<>(normalizer.tokenize(doc.category()));
        categoryWords.addAll(normalizer.tokenize(doc.parentCategory()));
        List<String> brandWords = normalizer.tokenize(doc.brand());
        List<String> colorWords = normalizer.tokenize(String.join(" ", doc.colors()));
        List<String> descriptionWords = normalizer.tokenize(doc.description());

        double raw = 0.0;
        int matched = 0;
        for (String token : queryTokens) {
            double tokenScore = 0.0;
            if (containsWordStartingWith(nameWords, token)) {
                tokenScore += SearchWeights.NAME;
            }
            if (containsWordStartingWith(categoryWords, token)) {
                tokenScore += SearchWeights.CATEGORY;
            }
            if (containsWordStartingWith(brandWords, token)) {
                tokenScore += SearchWeights.BRAND;
            }
            if (containsWordStartingWith(colorWords, token)) {
                tokenScore += SearchWeights.COLOR;
            }
            if (containsWordStartingWith(descriptionWords, token)) {
                tokenScore += SearchWeights.DESCRIPTION;
            }
            if (tokenScore > 0) {
                matched++;
                raw += tokenScore;
            }
        }
        if (matched == 0) {
            return RelevanceScore.none(queryTokens.size());
        }

        double coverage = (double) matched / queryTokens.size();
        double score = raw * coverage;
        if (queryTokens.size() > 1 && containsPhrase(nameWords, queryTokens)) {
            score += SearchWeights.PHRASE_BONUS;
        }
        if (!doc.inStock()) {
            score *= SearchWeights.OUT_OF_STOCK_FACTOR;
        }
        return new RelevanceScore(round2(score), matched, queryTokens.size());
    }

    private static boolean containsWordStartingWith(List<String> words, String token) {
        for (String word : words) {
            if (word.startsWith(token)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsPhrase(List<String> words, List<String> phrase) {
        String haystack = " " + String.join(" ", words) + " ";
        return haystack.contains(" " + String.join(" ", phrase) + " ");
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
