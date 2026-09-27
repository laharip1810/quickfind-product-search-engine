package com.quickfind.dto.response;

import java.util.List;

/** A top-level category with its subcategories. */
public record CategoryResponse(Long id, String name, List<SubcategoryResponse> subcategories) {

    public record SubcategoryResponse(Long id, String name) {
    }
}
