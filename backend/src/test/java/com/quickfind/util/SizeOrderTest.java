package com.quickfind.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SizeOrderTest {

    @Test
    void ordersLetterThenNumericThenOtherThenOneSize() {
        List<String> sizes = new ArrayList<>(List.of("One Size", "XL", "UK 10", "S", "32", "UK 7", "M", "Free"));
        sizes.sort(SizeOrder.COMPARATOR);
        assertEquals(List.of("S", "M", "XL", "UK 7", "UK 10", "32", "Free", "One Size"), sizes);
    }
}
