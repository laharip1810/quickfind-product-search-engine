package com.quickfind.search;

/**
 * How search candidates were matched.
 * ALL_TERMS is tried first; ANY_TERM is the fallback when no product contains every term.
 */
public enum MatchMode {
    /** No text query: pure filtering/browsing. */
    NONE,
    ALL_TERMS,
    ANY_TERM
}
