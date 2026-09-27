package com.quickfind.service;

import com.quickfind.dto.response.PriceRangeCountResponse;
import com.quickfind.exception.InvalidSearchRequestException;
import com.quickfind.repository.ProductPriceView;
import com.quickfind.repository.ProductRepository;
import com.quickfind.search.PriceRangeIndex;
import com.quickfind.util.TextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Price-facet counts served from per-category {@link PriceRangeIndex} instances
 * (binary search over sorted in-stock prices). Rebuilt lazily on the first request after a
 * catalog change.
 */
@Service
public class PriceIndexService {

    private static final Logger log = LoggerFactory.getLogger(PriceIndexService.class);
    private static final Long ALL = -1L;

    private final ProductRepository productRepository;
    private final CatalogLookupService lookup;
    private final AtomicBoolean dirty = new AtomicBoolean(true);
    private volatile Map<Long, PriceRangeIndex> indexes = Map.of();

    public PriceIndexService(ProductRepository productRepository, CatalogLookupService lookup) {
        this.productRepository = productRepository;
        this.lookup = lookup;
    }

    public PriceRangeCountResponse count(String category, BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice != null && minPrice.signum() < 0 || maxPrice != null && maxPrice.signum() < 0) {
            throw InvalidSearchRequestException.invalidFilter("minPrice and maxPrice must be zero or positive");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw InvalidSearchRequestException.invalidFilter("minPrice must not be greater than maxPrice");
        }
        String categoryName = TextUtils.clean(category);
        Long key = ALL;
        if (categoryName != null) {
            key = lookup.categoryByName(categoryName)
                    .orElseThrow(() -> InvalidSearchRequestException.invalidFilter("Unknown category '" + categoryName + "'"))
                    .id();
        }
        ensureFresh();
        PriceRangeIndex index = indexes.getOrDefault(key, PriceRangeIndex.empty());
        double min = minPrice == null ? 0.0 : minPrice.doubleValue();
        double max = maxPrice == null ? Double.MAX_VALUE : maxPrice.doubleValue();
        return new PriceRangeCountResponse(categoryName, minPrice, maxPrice, index.countBetween(min, max));
    }

    public void markDirty() {
        dirty.set(true);
    }

    private void ensureFresh() {
        if (dirty.compareAndSet(true, false)) {
            rebuild();
        }
    }

    private synchronized void rebuild() {
        long start = System.nanoTime();
        try {
            Map<Long, List<Double>> pricesByKey = new HashMap<>();
            for (ProductPriceView view : productRepository.findInStockPriceViews()) {
                double price = view.getSalePrice().doubleValue();
                pricesByKey.computeIfAbsent(ALL, k -> new ArrayList<>()).add(price);
                pricesByKey.computeIfAbsent(view.getCategoryId(), k -> new ArrayList<>()).add(price);
                if (view.getParentCategoryId() != null) {
                    pricesByKey.computeIfAbsent(view.getParentCategoryId(), k -> new ArrayList<>()).add(price);
                }
            }
            Map<Long, PriceRangeIndex> next = new HashMap<>();
            pricesByKey.forEach((key, prices) ->
                    next.put(key, new PriceRangeIndex(prices.stream().mapToDouble(Double::doubleValue).toArray())));
            indexes = Map.copyOf(next);
            log.info("Price index rebuilt for {} keys in {} ms", next.size(), (System.nanoTime() - start) / 1_000_000);
        } catch (RuntimeException e) {
            dirty.set(true);
            throw e;
        }
    }
}
