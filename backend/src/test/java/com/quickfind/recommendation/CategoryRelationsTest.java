package com.quickfind.recommendation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryRelationsTest {

    @Test
    void relationsAreSymmetricAndCaseInsensitive() {
        assertTrue(CategoryRelations.complementsOf("Running Shoes").contains("Sports Accessories"));
        assertTrue(CategoryRelations.complementsOf("sports accessories").contains("Running Shoes"));
        assertTrue(CategoryRelations.complementsOf("Unknown").isEmpty());
        assertTrue(CategoryRelations.complementsOf(null).isEmpty());
    }
}
