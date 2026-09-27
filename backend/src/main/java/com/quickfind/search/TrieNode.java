package com.quickfind.search;

import java.util.HashMap;
import java.util.Map;

/**
 * A Trie node. Children are kept in a HashMap rather than a fixed 26-slot array because
 * product text contains digits, spaces and hyphens, and most nodes have only one or two
 * children. The map is created on the first child, since leaf nodes never need one.
 */
final class TrieNode {

    Map<Character, TrieNode> children;

    boolean terminal;
    String displayText;
    SuggestionType type;
    double weight;
    Long productId;

    TrieNode child(char c) {
        return children == null ? null : children.get(c);
    }

    TrieNode getOrCreateChild(char c) {
        if (children == null) {
            children = new HashMap<>(4);
        }
        return children.computeIfAbsent(c, k -> new TrieNode());
    }
}
