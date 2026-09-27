package com.quickfind.recommendation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DiversityFilterTest {

    private record Item(String name, long group) {
    }

    @Test
    void capsItemsPerGroupInRankOrder() {
        List<Item> ranked = List.of(new Item("a1", 1), new Item("a2", 1), new Item("a3", 1),
                new Item("b1", 2), new Item("c1", 3));
        List<Item> picked = DiversityFilter.select(ranked, 4, 2, Item::group);
        assertEquals(List.of("a1", "a2", "b1", "c1"), picked.stream().map(Item::name).toList());
    }

    @Test
    void fillsFromOverflowWhenNotEnoughDiversity() {
        List<Item> ranked = List.of(new Item("a1", 1), new Item("a2", 1), new Item("a3", 1), new Item("a4", 1));
        List<Item> picked = DiversityFilter.select(ranked, 3, 1, Item::group);
        assertEquals(List.of("a1", "a2", "a3"), picked.stream().map(Item::name).toList());
    }
}
