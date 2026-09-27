package com.quickfind.search;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriceRangeIndexTest {

    private final PriceRangeIndex index = new PriceRangeIndex(new double[]{5000, 1000, 2000, 2000, 3000, 4999.99});

    @Test
    void countsInclusiveRange() {
        assertEquals(4, index.countBetween(2000, 4999.99));
        assertEquals(6, index.countBetween(0, 10_000));
        assertEquals(2, index.countBetween(2000, 2000));
    }

    @Test
    void emptyAndInvertedRanges() {
        assertEquals(0, index.countBetween(6000, 9000));
        assertEquals(0, index.countBetween(3000, 2000));
        assertEquals(0, PriceRangeIndex.empty().countBetween(0, 100));
    }

    @Test
    void boundsAreBinarySearchPositions() {
        // sorted: 1000, 2000, 2000, 3000, 4999.99, 5000
        assertEquals(1, index.lowerBound(2000));
        assertEquals(3, index.upperBound(2000));
        assertEquals(0, index.lowerBound(0));
        assertEquals(6, index.upperBound(99_999));
    }

    @Test
    void minAndMax() {
        assertEquals(1000, index.min().getAsDouble());
        assertEquals(5000, index.max().getAsDouble());
        assertTrue(PriceRangeIndex.empty().min().isEmpty());
    }

    @Test
    void doesNotMutateCallerArray() {
        double[] prices = {3, 1, 2};
        new PriceRangeIndex(prices);
        assertEquals(3, prices[0]);
    }
}
