package com.quickfind.service;

import com.quickfind.config.QuickFindProperties;
import com.quickfind.dto.request.SearchRequest;
import com.quickfind.dto.response.SearchHitResponse;
import com.quickfind.dto.response.SearchResponse;
import com.quickfind.entity.Product;
import com.quickfind.mapper.ProductMapper;
import com.quickfind.repository.ProductRepository;
import com.quickfind.repository.ProductSpecifications;
import com.quickfind.search.MatchMode;
import com.quickfind.search.RankedDocument;
import com.quickfind.search.RelevanceScore;
import com.quickfind.search.RelevanceScorer;
import com.quickfind.search.SearchDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Search pipeline:
 * <ol>
 *   <li>Validate the request and resolve filter names to ids.</li>
 *   <li>Normalise the query into tokens.</li>
 *   <li>No tokens: filter, sort and paginate entirely in MySQL.</li>
 *   <li>With tokens: MySQL returns up to {@code candidateLimit} candidates (filters
 *       + broad text match, most popular first). Java scores every candidate, drops
 *       non-matches, sorts with the chosen comparator and cuts out the requested page.
 *       All terms must match; if nothing does, the search falls back to any term.</li>
 * </ol>
 */
@Service
public class ProductSearchService {

    private static final Logger log = LoggerFactory.getLogger(ProductSearchService.class);

    private final ProductRepository productRepository;
    private final SearchRequestValidator validator;
    private final RelevanceScorer scorer;
    private final ProductMapper mapper;
    private final int candidateLimit;

    public ProductSearchService(ProductRepository productRepository, SearchRequestValidator validator,
                                RelevanceScorer scorer, ProductMapper mapper, QuickFindProperties properties) {
        this.productRepository = productRepository;
        this.validator = validator;
        this.scorer = scorer;
        this.mapper = mapper;
        this.candidateLimit = properties.search().candidateLimit();
    }

    /** Result of scoring one candidate set; byId is an O(1) lookup from ranked document back to entity. */
    private record Candidates(List<RankedDocument> ranked, Map<Long, Product> byId, boolean truncated, MatchMode mode) {
    }

    @Transactional(readOnly = true)
    public SearchResponse search(SearchRequest request) {
        long start = System.nanoTime();
        SearchCriteria criteria = validator.validate(request);
        Specification<Product> filters = ProductSpecifications.filteredBy(
                criteria.categoryIds(), criteria.brandIds(), criteria.colorIds(), criteria.productSize(),
                criteria.minPrice(), criteria.maxPrice(), criteria.minRating(), criteria.inStockOnly());

        SearchResponse response = criteria.hasQuery()
                ? rankedSearch(criteria, filters, start)
                : browse(criteria, filters, start);

        log.info("search query='{}' tokens={} mode={} results={} truncated={} took={}ms",
                criteria.query(), criteria.tokens(), response.matchMode(), response.totalElements(),
                response.candidatesTruncated(), response.tookMs());
        return response;
    }

    private SearchResponse browse(SearchCriteria c, Specification<Product> filters, long start) {
        Page<Product> page = productRepository.findAll(filters,
                PageRequest.of(c.page(), c.size(), ProductSorts.forOption(c.sort())));
        List<SearchHitResponse> hits = page.getContent().stream()
                .map(p -> new SearchHitResponse(mapper.toSummary(p), null))
                .toList();
        return new SearchResponse(hits, page.getNumber(), page.getSize(), page.getTotalElements(),
                page.getTotalPages(), ProductSorts.effectiveName(c.sort()), c.query(), c.tokens(),
                MatchMode.NONE.name(), false, elapsedMs(start));
    }

    private SearchResponse rankedSearch(SearchCriteria c, Specification<Product> filters, long start) {
        Candidates candidates = score(c, filters, MatchMode.ALL_TERMS);
        if (candidates.ranked().isEmpty()) {
            candidates = score(c, filters, MatchMode.ANY_TERM);
        }

        List<RankedDocument> ranked = new ArrayList<>(candidates.ranked());
        ranked.sort(c.sort().comparator());

        int total = ranked.size();
        int from = Math.min(c.page() * c.size(), total);
        int to = Math.min(from + c.size(), total);
        List<SearchHitResponse> hits = new ArrayList<>(to - from);
        for (RankedDocument doc : ranked.subList(from, to)) {
            Product product = candidates.byId().get(doc.document().id());
            hits.add(new SearchHitResponse(mapper.toSummary(product), doc.score()));
        }
        int totalPages = (int) Math.ceil((double) total / c.size());
        return new SearchResponse(hits, c.page(), c.size(), total, totalPages, c.sort().value(), c.query(),
                c.tokens(), candidates.mode().name(), candidates.truncated(), elapsedMs(start));
    }

    private Candidates score(SearchCriteria c, Specification<Product> filters, MatchMode mode) {
        Specification<Product> spec = filters.and(ProductSpecifications.matchesText(c.tokens(), mode));
        Page<Product> page = productRepository.findAll(spec,
                PageRequest.of(0, candidateLimit, ProductSorts.byPopularity()));

        List<RankedDocument> ranked = new ArrayList<>();
        Map<Long, Product> byId = new HashMap<>();
        for (Product product : page.getContent()) {
            SearchDocument doc = mapper.toSearchDocument(product);
            RelevanceScore relevance = scorer.score(c.tokens(), doc);
            boolean keep = mode == MatchMode.ALL_TERMS ? relevance.matchesAllTokens() : relevance.matchesAnyToken();
            if (keep) {
                ranked.add(new RankedDocument(doc, relevance));
                byId.put(product.getId(), product);
            }
        }
        boolean truncated = page.getTotalElements() > page.getNumberOfElements();
        return new Candidates(ranked, byId, truncated, mode);
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
