package com.quickfind.search;

public record RankedDocument(SearchDocument document, RelevanceScore relevance) {

    public double score() {
        return relevance == null ? 0.0 : relevance.score();
    }
}
