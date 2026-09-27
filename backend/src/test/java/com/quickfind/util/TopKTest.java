package com.quickfind.util;

import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TopKTest {

    private static final Comparator<Integer> DESC = Comparator.reverseOrder();

    @Test
    void returnsBestKInOrder() {
        assertEquals(List.of(9, 8, 7), TopK.select(List.of(5, 9, 1, 7, 3, 8), 3, DESC));
    }

    @Test
    void kLargerThanInputReturnsAllSorted() {
        assertEquals(List.of(3, 2, 1), TopK.select(List.of(1, 3, 2), 10, DESC));
    }

    @Test
    void zeroKOrEmptyInput() {
        assertTrue(TopK.select(List.of(1, 2), 0, DESC).isEmpty());
        assertTrue(TopK.select(List.<Integer>of(), 3, DESC).isEmpty());
    }
}
