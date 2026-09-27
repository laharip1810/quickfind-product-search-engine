package com.quickfind.search;

/** One autocomplete entry. productId is set only for PRODUCT suggestions. */
public record Suggestion(String text, SuggestionType type, double score, Long productId) {
}
