package com.quickfind.recommendation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Greedy diversity pass over an already-ranked list: take items in rank order, but at
 * most {@code maxPerGroup} from any one group (subcategory). Without it, the top 8 for a
 * running shoe would be 8 other running shoes. If the cap leaves the list short, the
 * skipped items are used to fill it, so a small catalog still returns results.
 */
public final class DiversityFilter {

    private DiversityFilter() {
    }

    public static <T> List<T> select(List<T> ranked, int limit, int maxPerGroup, Function<T, Long> groupOf) {
        List<T> selected = new ArrayList<>(limit);
        List<T> overflow = new ArrayList<>();
        Map<Long, Integer> perGroup = new HashMap<>();
        for (T item : ranked) {
            if (selected.size() == limit) {
                break;
            }
            int count = perGroup.getOrDefault(groupOf.apply(item), 0);
            if (count < maxPerGroup) {
                selected.add(item);
                perGroup.put(groupOf.apply(item), count + 1);
            } else {
                overflow.add(item);
            }
        }
        for (int i = 0; i < overflow.size() && selected.size() < limit; i++) {
            selected.add(overflow.get(i));
        }
        return selected;
    }
}
