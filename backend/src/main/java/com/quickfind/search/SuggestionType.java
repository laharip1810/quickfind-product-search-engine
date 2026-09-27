package com.quickfind.search;

/**
 * What a suggestion refers to. When the same text is inserted with several types
 * (for example "running shoes" as both a category and a name phrase), the type with the
 * higher priority provides the label shown to the user.
 */
public enum SuggestionType {
    PHRASE(0),
    QUERY(1),
    BRAND(2),
    CATEGORY(3),
    PRODUCT(4);

    private final int priority;

    SuggestionType(int priority) {
        this.priority = priority;
    }

    public boolean outranks(SuggestionType other) {
        return other == null || priority > other.priority;
    }
}
