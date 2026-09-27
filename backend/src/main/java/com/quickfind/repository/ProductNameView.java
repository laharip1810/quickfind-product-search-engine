package com.quickfind.repository;

/** Lightweight projection used to build the autocomplete Trie without loading full entities. */
public interface ProductNameView {

    Long getId();

    String getName();

    double getPopularityScore();

    String getBrandName();

    String getCategoryName();
}
