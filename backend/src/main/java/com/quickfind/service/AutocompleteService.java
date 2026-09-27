package com.quickfind.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.quickfind.cache.CacheKeys;
import com.quickfind.cache.CacheStore;
import com.quickfind.config.QuickFindProperties;
import com.quickfind.dto.response.SuggestionResponse;
import com.quickfind.exception.InvalidSearchRequestException;
import com.quickfind.repository.ProductNameView;
import com.quickfind.repository.ProductRepository;
import com.quickfind.repository.SearchTermCountView;
import com.quickfind.repository.UserInteractionRepository;
import com.quickfind.search.ProductSearchTrie;
import com.quickfind.search.SuggestionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Autocomplete backed by {@link ProductSearchTrie}.
 *
 * <p>Trie contents and weights:
 * <ul>
 *   <li>product names (the most popular N), weight 1 + popularity</li>
 *   <li>trailing name phrases such as "running shoes", weight 0.5 · (1 + popularity), summed
 *       over every product whose name ends that way</li>
 *   <li>brands and subcategories, weight = summed (1 + popularity) of their products</li>
 *   <li>popular search queries from SEARCH interactions, weight 20 per search</li>
 * </ul>
 *
 * <p>Lifecycle: built when the app is ready. A catalog change marks it dirty, and a
 * scheduled check rebuilds it off to the side and swaps a volatile reference. Several
 * changes in quick succession therefore cost one rebuild, and readers never block.
 */
@Service
public class AutocompleteService {

    private static final Logger log = LoggerFactory.getLogger(AutocompleteService.class);
    private static final TypeReference<SuggestionResponse> RESPONSE_TYPE = new TypeReference<>() {
    };
    private static final int MAX_PREFIX_LENGTH = 100;
    private static final int POPULAR_QUERY_LIMIT = 500;
    private static final double QUERY_WEIGHT_PER_SEARCH = 20.0;

    private final ProductRepository productRepository;
    private final UserInteractionRepository interactionRepository;
    private final CacheStore cacheStore;
    private final int defaultLimit;
    private final int maxLimit;
    private final int maxProductTerms;
    private final Duration ttl;

    private volatile ProductSearchTrie trie = new ProductSearchTrie();
    /** False until something changes; the initial build happens on ApplicationReadyEvent. */
    private final AtomicBoolean dirty = new AtomicBoolean(false);

    public AutocompleteService(ProductRepository productRepository, UserInteractionRepository interactionRepository,
                               CacheStore cacheStore, QuickFindProperties properties) {
        this.productRepository = productRepository;
        this.interactionRepository = interactionRepository;
        this.cacheStore = cacheStore;
        this.defaultLimit = properties.suggestions().defaultLimit();
        this.maxLimit = properties.suggestions().maxLimit();
        this.maxProductTerms = properties.suggestions().maxProductTerms();
        this.ttl = properties.cache().suggestionTtl();
    }

    public SuggestionResponse suggest(String prefix, Integer limitParam) {
        if (prefix == null || prefix.isBlank()) {
            throw InvalidSearchRequestException.invalidRequest("prefix is required");
        }
        if (prefix.length() > MAX_PREFIX_LENGTH) {
            throw InvalidSearchRequestException.invalidRequest("prefix must be at most " + MAX_PREFIX_LENGTH + " characters");
        }
        int limit = limitParam == null ? defaultLimit : limitParam;
        if (limit < 1 || limit > maxLimit) {
            throw InvalidSearchRequestException.invalidRequest("limit must be between 1 and " + maxLimit);
        }

        String normalized = ProductSearchTrie.normalizeKey(prefix);
        String key = CacheKeys.suggestions(cacheStore.version(CacheKeys.SUGGESTION_VERSION), normalized, limit);
        Optional<SuggestionResponse> cached = cacheStore.get(key, RESPONSE_TYPE);
        if (cached.isPresent()) {
            return cached.get();
        }

        List<SuggestionResponse.Item> items = trie.getSuggestions(prefix, limit).stream()
                .map(s -> new SuggestionResponse.Item(s.text(), s.type().name(), s.score(), s.productId()))
                .toList();
        SuggestionResponse response = new SuggestionResponse(prefix.trim(), items);
        cacheStore.put(key, response, ttl);
        return response;
    }

    public void markDirty() {
        dirty.set(true);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void buildOnStartup() {
        rebuild();
    }

    @Scheduled(fixedDelayString = "${quickfind.suggestions.rebuild-check-interval-ms:2000}",
            initialDelayString = "${quickfind.suggestions.rebuild-check-interval-ms:2000}")
    public void rebuildIfDirty() {
        if (dirty.compareAndSet(true, false)) {
            rebuild();
        }
    }

    /** Popular queries change without any catalog event, so refresh periodically. */
    @Scheduled(fixedDelayString = "${quickfind.suggestions.popular-query-refresh-interval-ms:300000}",
            initialDelayString = "${quickfind.suggestions.popular-query-refresh-interval-ms:300000}")
    public void refreshPopularQueries() {
        markDirty();
    }

    /** Builds a new Trie from the database and swaps it in. */
    public synchronized void rebuild() {
        long start = System.nanoTime();
        try {
            ProductSearchTrie next = new ProductSearchTrie();
            Map<String, Double> brandWeights = new HashMap<>();
            Map<String, Double> categoryWeights = new HashMap<>();

            List<ProductNameView> products = productRepository.findAllNameViews();
            int productTerms = 0;
            for (ProductNameView product : products) {
                double weight = 1.0 + Math.max(0.0, product.getPopularityScore());
                if (productTerms < maxProductTerms) {
                    next.insert(product.getName(), SuggestionType.PRODUCT, weight, product.getId());
                    productTerms++;
                }
                for (String phrase : trailingPhrases(product.getName(), product.getBrandName())) {
                    next.insert(phrase, SuggestionType.PHRASE, weight * 0.5, null);
                }
                brandWeights.merge(product.getBrandName(), weight, Double::sum);
                categoryWeights.merge(product.getCategoryName(), weight, Double::sum);
            }
            brandWeights.forEach((name, weight) -> next.insert(name, SuggestionType.BRAND, weight, null));
            categoryWeights.forEach((name, weight) -> next.insert(name, SuggestionType.CATEGORY, weight, null));

            List<SearchTermCountView> queries =
                    interactionRepository.findTopSearchTerms(PageRequest.of(0, POPULAR_QUERY_LIMIT));
            for (SearchTermCountView query : queries) {
                next.insert(query.getTerm(), SuggestionType.QUERY, query.getHits() * QUERY_WEIGHT_PER_SEARCH, null);
            }

            trie = next;
            cacheStore.bumpVersion(CacheKeys.SUGGESTION_VERSION);
            log.info("Autocomplete Trie rebuilt: {} terms from {} products and {} popular queries in {} ms",
                    next.size(), products.size(), queries.size(), (System.nanoTime() - start) / 1_000_000);
        } catch (RuntimeException e) {
            // Keep serving the previous Trie; try again on the next scheduled check.
            dirty.set(true);
            log.error("Autocomplete Trie rebuild failed; keeping the previous version", e);
        }
    }

    public int termCount() {
        return trie.size();
    }

    /**
     * The last 1, 2 and 3 words of a product name, without the brand and without trailing
     * size/volume tokens: "Nike Pegasus 41 Running Shoes" gives "shoes" and "running shoes";
     * "American Tourister Casual Laptop Backpack 32L" gives "backpack", "laptop backpack" and
     * "casual laptop backpack". Phrases containing digits ("41 running shoes") are skipped.
     * This is how "run" can suggest "running shoes", "running socks" and so on, although
     * product names start with the brand.
     */
    static List<String> trailingPhrases(String productName, String brandName) {
        String name = productName.trim();
        if (brandName != null && name.toLowerCase(Locale.ROOT).startsWith(brandName.toLowerCase(Locale.ROOT) + " ")) {
            name = name.substring(brandName.length()).trim();
        }
        List<String> words = new ArrayList<>(Arrays.asList(name.split("\\s+")));
        while (!words.isEmpty() && containsDigit(words.get(words.size() - 1))) {
            words.remove(words.size() - 1);
        }
        List<String> phrases = new ArrayList<>(3);
        for (int n = 1; n <= 3; n++) {
            if (words.size() < n || (n == 1 && words.get(words.size() - 1).length() < 3)) {
                continue;
            }
            List<String> tail = words.subList(words.size() - n, words.size());
            boolean clean = tail.stream().allMatch(w -> Character.isLetter(w.charAt(0)) && !containsDigit(w));
            if (clean) {
                phrases.add(String.join(" ", tail).toLowerCase(Locale.ROOT));
            }
        }
        return phrases;
    }

    private static boolean containsDigit(String word) {
        return word.chars().anyMatch(Character::isDigit);
    }
}
