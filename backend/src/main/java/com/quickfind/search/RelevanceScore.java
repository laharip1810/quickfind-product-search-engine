package com.quickfind.search;

public record RelevanceScore(double score, int matchedTokens, int totalTokens) {

    public static RelevanceScore none(int totalTokens) {
        return new RelevanceScore(0.0, 0, totalTokens);
    }

    public boolean matchesAllTokens() {
        return totalTokens > 0 && matchedTokens == totalTokens;
    }

    public boolean matchesAnyToken() {
        return matchedTokens > 0;
    }
}
