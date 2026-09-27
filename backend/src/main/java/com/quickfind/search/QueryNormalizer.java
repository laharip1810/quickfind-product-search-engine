package com.quickfind.search;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Turns free text into comparable tokens. The same pipeline is applied to the user's
 * query and to product fields, so "Black Running Shoes!" and "black running shoe"
 * normalise to the same tokens.
 *
 * <ol>
 *   <li>lowercase</li>
 *   <li>drop apostrophes ("levi's" becomes "levis")</li>
 *   <li>replace every other non-alphanumeric character with a space</li>
 *   <li>split on whitespace (this also collapses repeated spaces)</li>
 *   <li>remove stopwords and one-letter tokens</li>
 *   <li>fold simple English plurals ("shoes" to "shoe", "watches" to "watch")</li>
 * </ol>
 *
 * Complexity: O(n) in the length of the input.
 */
public final class QueryNormalizer {

    private static final Pattern APOSTROPHES = Pattern.compile("['’]");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");

    private static final Set<String> STOPWORDS = Set.of(
            "a", "an", "and", "are", "at", "by", "for", "from", "in", "is",
            "of", "on", "or", "the", "to", "with");

    /** Query tokens: normalised and de-duplicated, in original order. */
    public List<String> normalizeQuery(String query) {
        return new ArrayList<>(new LinkedHashSet<>(tokenize(query)));
    }

    /** Field tokens: normalised, order and duplicates preserved (needed for phrase matching). */
    public List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String cleaned = APOSTROPHES.matcher(text.toLowerCase(Locale.ROOT)).replaceAll("");
        cleaned = NON_ALPHANUMERIC.matcher(cleaned).replaceAll(" ").trim();
        if (cleaned.isEmpty()) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        for (String word : cleaned.split(" ")) {
            if (word.length() < 2 || STOPWORDS.contains(word)) {
                continue;
            }
            tokens.add(singularize(word));
        }
        return tokens;
    }

    /** Light plural folding. Deliberately simple and predictable, not a full stemmer. */
    static String singularize(String word) {
        if (word.length() <= 3 || !Character.isLetter(word.charAt(word.length() - 1))) {
            return word;
        }
        if (word.endsWith("ies") && word.length() > 4) {
            return word.substring(0, word.length() - 3) + "y";
        }
        if (word.endsWith("sses") || word.endsWith("ches") || word.endsWith("shes") || word.endsWith("xes")) {
            return word.substring(0, word.length() - 2);
        }
        if (word.endsWith("ss") || word.endsWith("us") || word.endsWith("is")) {
            return word;
        }
        if (word.endsWith("s")) {
            return word.substring(0, word.length() - 1);
        }
        return word;
    }
}
