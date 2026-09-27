package com.quickfind.dto.response;

import java.util.List;

/**
 * @param normalizedTokens    the query after normalisation, so clients can see what was searched
 * @param matchMode           NONE (no query), ALL_TERMS, or ANY_TERM (fallback)
 * @param candidatesTruncated true if more products matched than the candidate limit, so only
 *                            the most popular candidates were scored
 * @param tookMs              server-side time spent on the search
 */
public record SearchResponse(
        List<SearchHitResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        String sort,
        String query,
        List<String> normalizedTokens,
        String matchMode,
        boolean candidatesTruncated,
        long tookMs) {
}
