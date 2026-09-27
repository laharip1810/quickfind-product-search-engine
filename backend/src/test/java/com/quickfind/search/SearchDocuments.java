package com.quickfind.search;

import java.math.BigDecimal;
import java.util.List;

/** Test factory for SearchDocument. */
final class SearchDocuments {

    private SearchDocuments() {
    }

    static SearchDocument doc(long id, String name, String brand, String category, String parent,
                              List<String> colors, String description, boolean inStock) {
        return new SearchDocument(id, name, description, brand, category, parent, colors,
                BigDecimal.valueOf(1000 + id), 4.0, 100, 10.0, inStock);
    }

    static SearchDocument sortable(long id, double price, double rating, int reviews, double popularity) {
        return new SearchDocument(id, "Product " + id, "", "Brand", "Cat", "Parent", List.of(),
                BigDecimal.valueOf(price), rating, reviews, popularity, true);
    }
}
