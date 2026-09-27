package com.quickfind.search;

import java.util.Arrays;
import java.util.OptionalDouble;

/**
 * Sorted array of prices that answers "how many products cost between min and max?" with
 * two binary searches.
 *
 * <p>Why not just ask the database? The price filter itself does use the
 * (category_id, sale_price) B-tree index. This structure is for the price facet in the
 * filter sidebar: the count is recomputed on every keystroke while a user edits the
 * price range. Serving that from memory in O(log n) avoids a COUNT(*) round trip per
 * keystroke. The index is rebuilt after catalog changes.
 *
 * <ul>
 *   <li>Build: O(n log n) (sort)</li>
 *   <li>{@link #countBetween}: O(log n)</li>
 *   <li>Space: O(n)</li>
 * </ul>
 */
public final class PriceRangeIndex {

    private final double[] sortedPrices;

    public PriceRangeIndex(double[] prices) {
        this.sortedPrices = prices.clone();
        Arrays.sort(this.sortedPrices);
    }

    public static PriceRangeIndex empty() {
        return new PriceRangeIndex(new double[0]);
    }

    /** Number of prices p with min <= p <= max (both bounds inclusive). */
    public int countBetween(double min, double max) {
        if (min > max || sortedPrices.length == 0) {
            return 0;
        }
        return upperBound(max) - lowerBound(min);
    }

    /** Index of the first element >= target (length if none). */
    int lowerBound(double target) {
        int lo = 0;
        int hi = sortedPrices.length;
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            if (sortedPrices[mid] < target) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return lo;
    }

    /** Index of the first element > target (length if none). */
    int upperBound(double target) {
        int lo = 0;
        int hi = sortedPrices.length;
        while (lo < hi) {
            int mid = lo + (hi - lo) / 2;
            if (sortedPrices[mid] <= target) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return lo;
    }

    public int size() {
        return sortedPrices.length;
    }

    public OptionalDouble min() {
        return sortedPrices.length == 0 ? OptionalDouble.empty() : OptionalDouble.of(sortedPrices[0]);
    }

    public OptionalDouble max() {
        return sortedPrices.length == 0 ? OptionalDouble.empty() : OptionalDouble.of(sortedPrices[sortedPrices.length - 1]);
    }
}
