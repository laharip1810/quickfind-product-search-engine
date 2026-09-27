package com.quickfind.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Top-k selection with a bounded min-heap.
 *
 * <p>Sorting everything and taking k costs O(n log n). Keeping a heap of at most k
 * elements costs O(n log k) time and O(k) extra space. For "top 8 of 2,000 candidates"
 * that is log 8 = 3 instead of log 2000 = 11 per element. The heap is ordered
 * worst-first, so its head is always the element to evict.
 */
public final class TopK {

    private TopK() {
    }

    /** Returns up to k items, best first according to {@code bestFirst}. */
    public static <T> List<T> select(Collection<T> items, int k, Comparator<T> bestFirst) {
        if (k <= 0 || items.isEmpty()) {
            return List.of();
        }
        PriorityQueue<T> heap = new PriorityQueue<>(Math.min(k, items.size()) + 1, bestFirst.reversed());
        for (T item : items) {
            heap.offer(item);
            if (heap.size() > k) {
                heap.poll();
            }
        }
        List<T> result = new ArrayList<>(heap);
        result.sort(bestFirst);
        return result;
    }
}
