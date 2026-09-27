package com.quickfind.search;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.PriorityQueue;

/**
 * Prefix tree for autocomplete.
 *
 * <p>Complexity, with L = term length, p = prefix length, m = number of nodes below the
 * prefix node and k = the suggestion limit:
 * <ul>
 *   <li>{@link #insert}: O(L)</li>
 *   <li>{@link #searchPrefix}: O(p), independent of how many products exist</li>
 *   <li>{@link #getSuggestions}: O(p + m log k). The subtree is walked once, and a
 *       min-heap of size k keeps only the best k terms, so there is no full sort.</li>
 *   <li>Space: O(total characters of all inserted terms) in the worst case, less where
 *       terms share prefixes.</li>
 * </ul>
 *
 * <p>Not thread-safe for writes. The service builds a new Trie off to the side and swaps
 * the reference, so readers only ever see a fully built, read-only instance.
 */
public class ProductSearchTrie {

    /** Best suggestion first: higher weight, then alphabetical for determinism. */
    private static final Comparator<Suggestion> BEST_FIRST =
            Comparator.comparingDouble(Suggestion::score).reversed()
                    .thenComparing(Suggestion::text, String.CASE_INSENSITIVE_ORDER);

    private final TrieNode root = new TrieNode();
    private int termCount;

    /**
     * Inserts a term, or merges it with an existing identical term (case-insensitive).
     * Weights are summed on merge, so text that appears in many products ranks higher.
     */
    public void insert(String term, SuggestionType type, double weight, Long productId) {
        String key = normalizeKey(term);
        if (key.isEmpty()) {
            return;
        }
        TrieNode node = root;
        for (int i = 0; i < key.length(); i++) {
            node = node.getOrCreateChild(key.charAt(i));
        }
        if (!node.terminal) {
            node.terminal = true;
            termCount++;
            node.displayText = displayFor(term, type);
            node.type = type;
            node.productId = productId;
            node.weight = weight;
            return;
        }
        node.weight += weight;
        if (type.outranks(node.type)) {
            node.type = type;
            node.displayText = displayFor(term, type);
            node.productId = productId;
        }
    }

    /** True if at least one inserted term starts with the prefix. */
    public boolean searchPrefix(String prefix) {
        String key = normalizeKey(prefix);
        return !key.isEmpty() && findNode(key) != null;
    }

    /** True if exactly this term was inserted. */
    public boolean contains(String term) {
        TrieNode node = findNode(normalizeKey(term));
        return node != null && node.terminal;
    }

    /**
     * Up to {@code limit} terms starting with the prefix, best first.
     * A missing or blank prefix returns an empty list (never null, never an exception).
     */
    public List<Suggestion> getSuggestions(String prefix, int limit) {
        String key = normalizeKey(prefix);
        if (key.isEmpty() || limit <= 0) {
            return List.of();
        }
        TrieNode start = findNode(key);
        if (start == null) {
            return List.of();
        }

        // Min-heap ordered worst-first: the head is the weakest of the current top k.
        PriorityQueue<Suggestion> topK = new PriorityQueue<>(limit + 1, BEST_FIRST.reversed());
        Deque<TrieNode> stack = new ArrayDeque<>();
        stack.push(start);
        while (!stack.isEmpty()) {
            TrieNode node = stack.pop();
            if (node.terminal) {
                topK.offer(new Suggestion(node.displayText, node.type, round2(node.weight), node.productId));
                if (topK.size() > limit) {
                    topK.poll();
                }
            }
            if (node.children != null) {
                for (TrieNode child : node.children.values()) {
                    stack.push(child);
                }
            }
        }

        List<Suggestion> result = new ArrayList<>(topK);
        result.sort(BEST_FIRST);
        return Collections.unmodifiableList(result);
    }

    public int size() {
        return termCount;
    }

    private TrieNode findNode(String key) {
        TrieNode node = root;
        for (int i = 0; i < key.length(); i++) {
            node = node.child(key.charAt(i));
            if (node == null) {
                return null;
            }
        }
        return node;
    }

    /** Lowercase and collapse runs of whitespace; a single trailing space is kept ("running "). */
    public static String normalizeKey(String text) {
        if (text == null) {
            return "";
        }
        boolean trailingSpace = !text.isEmpty() && Character.isWhitespace(text.charAt(text.length() - 1));
        String collapsed = text.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
        if (collapsed.isEmpty()) {
            return "";
        }
        return trailingSpace ? collapsed + " " : collapsed;
    }

    private static String displayFor(String term, SuggestionType type) {
        String trimmed = term.trim().replaceAll("\\s+", " ");
        return (type == SuggestionType.PHRASE || type == SuggestionType.QUERY)
                ? trimmed.toLowerCase(Locale.ROOT)
                : trimmed;
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
