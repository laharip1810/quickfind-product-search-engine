package com.quickfind.service;

import com.quickfind.config.QuickFindProperties;
import com.quickfind.dto.request.SearchRequest;
import com.quickfind.exception.InvalidSearchRequestException;
import com.quickfind.search.QueryNormalizer;
import com.quickfind.search.SortOption;
import com.quickfind.util.TextUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Validates search/list parameters and resolves filter names to ids.
 * Every problem becomes a 400 with a message that says what to send instead.
 */
@Component
public class SearchRequestValidator {

    static final int MAX_QUERY_LENGTH = 200;
    private static final BigDecimal FIVE = BigDecimal.valueOf(5);

    private final CatalogLookupService lookup;
    private final QueryNormalizer normalizer;
    private final int defaultPageSize;
    private final int maxPageSize;

    public SearchRequestValidator(CatalogLookupService lookup, QueryNormalizer normalizer,
                                  QuickFindProperties properties) {
        this.lookup = lookup;
        this.normalizer = normalizer;
        this.defaultPageSize = properties.search().defaultPageSize();
        this.maxPageSize = properties.search().maxPageSize();
    }

    public SearchCriteria validate(SearchRequest request) {
        String query = TextUtils.clean(request.query());
        if (query != null && query.length() > MAX_QUERY_LENGTH) {
            throw InvalidSearchRequestException.invalidRequest(
                    "query must be at most " + MAX_QUERY_LENGTH + " characters");
        }
        SortOption sort = parseSort(request.sort());
        int page = validatePage(request.page());
        int size = validateSize(request.size());

        validatePrice("minPrice", request.minPrice());
        validatePrice("maxPrice", request.maxPrice());
        if (request.minPrice() != null && request.maxPrice() != null
                && request.minPrice().compareTo(request.maxPrice()) > 0) {
            throw InvalidSearchRequestException.invalidFilter(
                    "minPrice (" + request.minPrice() + ") must not be greater than maxPrice (" + request.maxPrice() + ")");
        }
        if (request.minRating() != null
                && (request.minRating().signum() < 0 || request.minRating().compareTo(FIVE) > 0)) {
            throw InvalidSearchRequestException.invalidFilter("minRating must be between 0 and 5");
        }

        return new SearchCriteria(
                query,
                query == null ? List.of() : normalizer.normalizeQuery(query),
                resolveCategory(request.category()),
                resolveBrands(request.brand()),
                resolveColor(request.color()),
                TextUtils.clean(request.productSize()),
                request.minPrice(),
                request.maxPrice(),
                request.minRating(),
                Boolean.TRUE.equals(request.inStock()),
                sort,
                page,
                size);
    }

    public SortOption parseSort(String raw) {
        return SortOption.parse(raw).orElseThrow(() -> InvalidSearchRequestException.invalidSort(
                "Sort '" + raw + "' is not supported. Allowed: " + SortOption.allowedValues()));
    }

    public int validatePage(Integer page) {
        if (page == null) {
            return 0;
        }
        if (page < 0) {
            throw InvalidSearchRequestException.invalidRequest("page must be 0 or greater");
        }
        return page;
    }

    public int validateSize(Integer size) {
        if (size == null) {
            return defaultPageSize;
        }
        if (size < 1 || size > maxPageSize) {
            throw InvalidSearchRequestException.invalidRequest("size must be between 1 and " + maxPageSize);
        }
        return size;
    }

    /** Resolves a category name (top-level or subcategory) or numeric id to product category ids. */
    public Set<Long> resolveCategory(String raw) {
        String name = TextUtils.clean(raw);
        if (name == null) {
            return Set.of();
        }
        CatalogLookupService.CategoryInfo info = lookup.categoryByName(name)
                .or(() -> parseId(name).flatMap(lookup::categoryById))
                .orElseThrow(() -> InvalidSearchRequestException.invalidFilter("Unknown category '" + name + "'"));
        return info.matchingProductCategoryIds();
    }

    private Set<Long> resolveBrands(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return Set.of();
        }
        Set<Long> ids = new HashSet<>();
        for (String value : raw) {
            String name = TextUtils.clean(value);
            if (name == null) {
                continue;
            }
            ids.add(lookup.brandIdByName(name)
                    .orElseThrow(() -> InvalidSearchRequestException.invalidFilter("Unknown brand '" + name + "'")));
        }
        return ids;
    }

    private Set<Long> resolveColor(String raw) {
        String name = TextUtils.clean(raw);
        if (name == null) {
            return Set.of();
        }
        return Set.of(lookup.colorIdByName(name)
                .orElseThrow(() -> InvalidSearchRequestException.invalidFilter("Unknown color '" + name + "'")));
    }

    private static void validatePrice(String field, BigDecimal value) {
        if (value != null && value.signum() < 0) {
            throw InvalidSearchRequestException.invalidFilter(field + " must be zero or positive");
        }
    }

    private static Optional<Long> parseId(String value) {
        try {
            return Optional.of(Long.parseLong(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
