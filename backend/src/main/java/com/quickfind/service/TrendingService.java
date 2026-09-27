package com.quickfind.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.quickfind.cache.CacheKeys;
import com.quickfind.cache.CacheStore;
import com.quickfind.config.QuickFindProperties;
import com.quickfind.dto.response.ProductSummaryResponse;
import com.quickfind.entity.Product;
import com.quickfind.exception.InvalidSearchRequestException;
import com.quickfind.mapper.ProductMapper;
import com.quickfind.repository.ProductRepository;
import com.quickfind.repository.ProductSpecifications;
import com.quickfind.repository.RecentInteractionView;
import com.quickfind.repository.UserInteractionRepository;
import com.quickfind.util.TopK;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Top-N trending products: weighted engagement over the last few days.
 *
 * <p>A HashMap accumulates a weighted count per product (view 1, wishlist 3, add-to-cart 5)
 * in one pass over recent events, then a PriorityQueue of size N picks the winners in
 * O(n log N). If there are not enough recent events (a fresh install), the list is topped
 * up with the most popular in-stock products.
 *
 * <p>At larger scale this aggregation belongs in the database (GROUP BY over an indexed
 * window) or in a streaming counter. The in-memory version is bounded by
 * quickfind.trending.max-events and cached for five minutes.
 */
@Service
public class TrendingService {

    private static final TypeReference<List<ProductSummaryResponse>> LIST_TYPE = new TypeReference<>() {
    };
    private static final int MAX_LIMIT = 50;

    private final UserInteractionRepository interactionRepository;
    private final ProductRepository productRepository;
    private final ProductMapper mapper;
    private final CacheStore cacheStore;
    private final Clock clock;
    private final QuickFindProperties.Trending settings;
    private final Duration ttl;

    public TrendingService(UserInteractionRepository interactionRepository, ProductRepository productRepository,
                           ProductMapper mapper, CacheStore cacheStore, Clock clock, QuickFindProperties properties) {
        this.interactionRepository = interactionRepository;
        this.productRepository = productRepository;
        this.mapper = mapper;
        this.cacheStore = cacheStore;
        this.clock = clock;
        this.settings = properties.trending();
        this.ttl = properties.cache().trendingTtl();
    }

    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> trending(Integer limitParam) {
        int limit = limitParam == null ? 8 : limitParam;
        if (limit < 1 || limit > MAX_LIMIT) {
            throw InvalidSearchRequestException.invalidRequest("limit must be between 1 and " + MAX_LIMIT);
        }
        String key = CacheKeys.trending(cacheStore.version(CacheKeys.CATALOG_VERSION), limit);
        Optional<List<ProductSummaryResponse>> cached = cacheStore.get(key, LIST_TYPE);
        if (cached.isPresent()) {
            return cached.get();
        }

        LocalDateTime since = LocalDateTime.now(clock).minusDays(settings.windowDays());
        List<RecentInteractionView> events =
                interactionRepository.findRecentProductEvents(since, PageRequest.of(0, settings.maxEvents()));
        Map<Long, Double> engagement = new HashMap<>();
        for (RecentInteractionView event : events) {
            double weight = event.getEventType().weight();
            if (weight > 0) {
                engagement.merge(event.getProductId(), weight, Double::sum);
            }
        }

        // Ask for 2N so that out-of-stock winners can be skipped without a second query.
        Comparator<Map.Entry<Long, Double>> bestFirst = Map.Entry.<Long, Double>comparingByValue().reversed()
                .thenComparing(Map.Entry.comparingByKey());
        List<Long> rankedIds = TopK.select(engagement.entrySet(), limit * 2, bestFirst).stream()
                .map(Map.Entry::getKey).toList();

        Map<Long, Product> byId = productRepository.findAllByIdIn(rankedIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        Set<Product> selected = new LinkedHashSet<>();
        for (Long id : rankedIds) {
            Product product = byId.get(id);
            if (product != null && product.isInStock() && selected.size() < limit) {
                selected.add(product);
            }
        }
        if (selected.size() < limit) {
            List<Product> popular = productRepository.findAll(ProductSpecifications.inStock(),
                    PageRequest.of(0, limit * 2, ProductSorts.byPopularity())).getContent();
            for (Product product : popular) {
                if (selected.size() >= limit) {
                    break;
                }
                selected.add(product);
            }
        }

        List<ProductSummaryResponse> result = new ArrayList<>(selected.size());
        selected.forEach(p -> result.add(mapper.toSummary(p)));
        cacheStore.put(key, result, ttl);
        return result;
    }
}
