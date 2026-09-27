package com.quickfind.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.quickfind.cache.CacheKeys;
import com.quickfind.cache.CacheStore;
import com.quickfind.config.QuickFindProperties;
import com.quickfind.dto.response.RecommendationResponse;
import com.quickfind.entity.Category;
import com.quickfind.entity.Product;
import com.quickfind.exception.InvalidSearchRequestException;
import com.quickfind.exception.ProductNotFoundException;
import com.quickfind.exception.ResourceNotFoundException;
import com.quickfind.mapper.ProductMapper;
import com.quickfind.recommendation.CategoryRelations;
import com.quickfind.recommendation.DiversityFilter;
import com.quickfind.recommendation.RecommendationCandidate;
import com.quickfind.recommendation.RecommendationContext;
import com.quickfind.recommendation.RecommendationScorer;
import com.quickfind.recommendation.RecommendationWeights;
import com.quickfind.recommendation.ScoreBreakdown;
import com.quickfind.repository.CoInteractionView;
import com.quickfind.repository.ProductRepository;
import com.quickfind.repository.ProductSpecifications;
import com.quickfind.repository.UserInteractionRepository;
import com.quickfind.repository.UserRepository;
import com.quickfind.util.TopK;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * "You may also like" for a product page.
 *
 * <ol>
 *   <li>Gather co-interaction strengths (users who engaged with this product also engaged with...).</li>
 *   <li>Retrieve up to 500 in-stock candidates related by parent category, complementary
 *       subcategory, brand or co-interaction.</li>
 *   <li>Score each candidate with {@link RecommendationScorer} (explainable weighted sum).</li>
 *   <li>Keep the best 3·limit with a bounded heap ({@link TopK}), then apply a diversity cap per
 *       subcategory ({@link DiversityFilter}).</li>
 * </ol>
 */
@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);
    private static final TypeReference<RecommendationResponse> RESPONSE_TYPE = new TypeReference<>() {
    };
    private static final int USER_HISTORY_SIZE = 200;
    private static final int USER_TOP_CATEGORIES = 3;

    private record Scored(Product product, ScoreBreakdown breakdown) {
    }

    private static final Comparator<Scored> BEST_FIRST =
            Comparator.comparingDouble((Scored s) -> s.breakdown().total()).reversed()
                    .thenComparingLong(s -> s.product().getId());

    private final ProductRepository productRepository;
    private final UserInteractionRepository interactionRepository;
    private final UserRepository userRepository;
    private final CatalogLookupService lookup;
    private final RecommendationScorer scorer;
    private final ProductMapper mapper;
    private final CacheStore cacheStore;
    private final QuickFindProperties.Recommendations settings;
    private final Duration ttl;

    public RecommendationService(ProductRepository productRepository, UserInteractionRepository interactionRepository,
                                 UserRepository userRepository, CatalogLookupService lookup,
                                 RecommendationScorer scorer, ProductMapper mapper, CacheStore cacheStore,
                                 QuickFindProperties properties) {
        this.productRepository = productRepository;
        this.interactionRepository = interactionRepository;
        this.userRepository = userRepository;
        this.lookup = lookup;
        this.scorer = scorer;
        this.mapper = mapper;
        this.cacheStore = cacheStore;
        this.settings = properties.recommendations();
        this.ttl = properties.cache().recommendationTtl();
    }

    @Transactional(readOnly = true)
    public RecommendationResponse recommend(Long productId, Long userId, Integer limitParam) {
        int limit = limitParam == null ? settings.defaultLimit() : limitParam;
        if (limit < 1 || limit > settings.maxLimit()) {
            throw InvalidSearchRequestException.invalidRequest("limit must be between 1 and " + settings.maxLimit());
        }
        if (userId != null && !userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", userId);
        }
        Product source = productRepository.findWithDetailsById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        String key = CacheKeys.recommendations(cacheStore.version(CacheKeys.CATALOG_VERSION), productId, userId, limit);
        Optional<RecommendationResponse> cached = cacheStore.get(key, RESPONSE_TYPE);
        if (cached.isPresent()) {
            return cached.get();
        }

        long start = System.nanoTime();
        Category sourceCategory = source.getCategory();
        Map<Long, Double> coInteraction = normalizedCoInteractions(productId);
        Set<Long> complementIds = lookup.categoryIdsByNames(CategoryRelations.complementsOf(sourceCategory.getName()));
        Long parentId = sourceCategory.getParent() == null ? null : sourceCategory.getParent().getId();

        List<Product> candidates = productRepository.findAll(
                ProductSpecifications.recommendationCandidates(productId, parentId, complementIds,
                        source.getBrand().getId(), coInteraction.keySet()),
                PageRequest.of(0, settings.candidateLimit(), ProductSorts.byPopularity())).getContent();

        double maxPopularity = candidates.stream().mapToDouble(Product::getPopularityScore).max().orElse(0.0);
        Set<Long> preferred = userId == null ? Set.of() : topCategoriesForUser(userId);
        RecommendationContext context = new RecommendationContext(complementIds, coInteraction, maxPopularity, preferred);
        RecommendationCandidate sourceCandidate = mapper.toCandidate(source);

        List<Scored> scored = new ArrayList<>(candidates.size());
        for (Product candidate : candidates) {
            scored.add(new Scored(candidate, scorer.score(sourceCandidate, mapper.toCandidate(candidate), context)));
        }
        List<Scored> best = TopK.select(scored, limit * 3, BEST_FIRST);
        List<Scored> chosen = DiversityFilter.select(best, limit, settings.maxPerSubcategory(),
                s -> s.product().getCategory().getId());

        List<RecommendationResponse.Item> items = chosen.stream()
                .map(s -> new RecommendationResponse.Item(mapper.toSummary(s.product()), s.breakdown().total(),
                        s.breakdown().components(), reasons(source, s)))
                .toList();
        RecommendationResponse response = new RecommendationResponse(productId, userId, items);
        cacheStore.put(key, response, ttl);
        log.info("recommendations product={} user={} candidates={} returned={} took={}ms", productId, userId,
                candidates.size(), items.size(), (System.nanoTime() - start) / 1_000_000);
        return response;
    }

    /** Co-interaction strength per product, divided by the strongest one (0..1). */
    private Map<Long, Double> normalizedCoInteractions(Long productId) {
        List<CoInteractionView> rows = interactionRepository.findCoInteractions(productId);
        double max = rows.stream().mapToDouble(r -> r.getStrength().doubleValue()).max().orElse(0.0);
        Map<Long, Double> normalized = new HashMap<>();
        if (max > 0) {
            for (CoInteractionView row : rows) {
                normalized.put(row.getProductId().longValue(), row.getStrength().doubleValue() / max);
            }
        }
        return normalized;
    }

    /** The user's most frequent subcategories among their recent interactions (HashMap frequency count + top-k). */
    private Set<Long> topCategoriesForUser(Long userId) {
        List<Long> recent = interactionRepository.findRecentCategoryIdsByUser(userId, PageRequest.of(0, USER_HISTORY_SIZE));
        Map<Long, Integer> frequency = new HashMap<>();
        for (Long categoryId : recent) {
            frequency.merge(categoryId, 1, Integer::sum);
        }
        Comparator<Map.Entry<Long, Integer>> byCount = Map.Entry.<Long, Integer>comparingByValue().reversed()
                .thenComparing(Map.Entry.comparingByKey());
        Set<Long> top = new HashSet<>();
        TopK.select(frequency.entrySet(), USER_TOP_CATEGORIES, byCount).forEach(e -> top.add(e.getKey()));
        return top;
    }

    private static List<String> reasons(Product source, Scored scored) {
        Product candidate = scored.product();
        Map<String, Double> c = scored.breakdown().components();
        List<String> reasons = new ArrayList<>();
        double categoryShare = c.getOrDefault("category", 0.0) / RecommendationWeights.CATEGORY;
        if (categoryShare >= RecommendationWeights.SAME_SUBCATEGORY - 1e-9) {
            reasons.add("Also in " + candidate.getCategory().getName());
        } else if (categoryShare >= RecommendationWeights.COMPLEMENTARY_SUBCATEGORY - 1e-9) {
            reasons.add("Pairs well with " + source.getCategory().getName());
        } else if (categoryShare > 0 && candidate.getCategory().getParent() != null) {
            reasons.add("Related " + candidate.getCategory().getParent().getName());
        }
        if (c.getOrDefault("brand", 0.0) > 0) {
            reasons.add("Same brand (" + candidate.getBrand().getName() + ")");
        }
        if (c.getOrDefault("price", 0.0) / RecommendationWeights.PRICE >= 0.8) {
            reasons.add("Similar price");
        }
        if (c.getOrDefault("coInteraction", 0.0) > 0) {
            reasons.add("Shoppers who viewed this also engaged with it");
        }
        if (candidate.getRating().doubleValue() >= 4.5) {
            reasons.add("Highly rated (" + candidate.getRating() + "★)");
        }
        if (c.containsKey("userAffinity")) {
            reasons.add("Matches your recent interests");
        }
        return reasons;
    }
}
