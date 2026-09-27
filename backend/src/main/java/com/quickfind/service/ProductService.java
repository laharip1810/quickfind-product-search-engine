package com.quickfind.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.quickfind.cache.CacheKeys;
import com.quickfind.cache.CacheStore;
import com.quickfind.config.QuickFindProperties;
import com.quickfind.dto.request.ProductRequest;
import com.quickfind.dto.request.StockUpdateRequest;
import com.quickfind.dto.response.PagedResponse;
import com.quickfind.dto.response.ProductDetailResponse;
import com.quickfind.dto.response.ProductSummaryResponse;
import com.quickfind.entity.Brand;
import com.quickfind.entity.Category;
import com.quickfind.entity.Color;
import com.quickfind.entity.Product;
import com.quickfind.exception.DuplicateProductException;
import com.quickfind.exception.InvalidProductException;
import com.quickfind.exception.ProductNotFoundException;
import com.quickfind.exception.VersionConflictException;
import com.quickfind.mapper.ProductMapper;
import com.quickfind.repository.BrandRepository;
import com.quickfind.repository.CategoryRepository;
import com.quickfind.repository.ColorRepository;
import com.quickfind.repository.ProductRepository;
import com.quickfind.search.SortOption;
import com.quickfind.util.TextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Product catalog CRUD with cache-aside reads of product details. */
@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    private static final TypeReference<ProductDetailResponse> DETAIL_TYPE = new TypeReference<>() {
    };

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ColorRepository colorRepository;
    private final ProductMapper mapper;
    private final CacheStore cacheStore;
    private final SearchRequestValidator validator;
    private final ApplicationEventPublisher events;
    private final Duration productTtl;

    public ProductService(ProductRepository productRepository, BrandRepository brandRepository,
                          CategoryRepository categoryRepository, ColorRepository colorRepository,
                          ProductMapper mapper, CacheStore cacheStore, SearchRequestValidator validator,
                          ApplicationEventPublisher events, QuickFindProperties properties) {
        this.productRepository = productRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.colorRepository = colorRepository;
        this.mapper = mapper;
        this.cacheStore = cacheStore;
        this.validator = validator;
        this.events = events;
        this.productTtl = properties.cache().productTtl();
    }

    /** Paginated catalog. "relevance" has no meaning without a query, so it falls back to popularity. */
    @Transactional(readOnly = true)
    public PagedResponse<ProductSummaryResponse> list(Integer page, Integer size, String sort) {
        int pageNumber = validator.validatePage(page);
        int pageSize = validator.validateSize(size);
        SortOption option = validator.parseSort(sort);
        Page<Product> result = productRepository.findAll(
                PageRequest.of(pageNumber, pageSize, ProductSorts.forOption(option)));
        return new PagedResponse<>(
                result.getContent().stream().map(mapper::toSummary).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages(),
                ProductSorts.effectiveName(option));
    }

    /** Cache-aside: Redis first, MySQL on a miss, then populate Redis. */
    @Transactional(readOnly = true)
    public ProductDetailResponse get(Long id) {
        String key = CacheKeys.product(id);
        Optional<ProductDetailResponse> cached = cacheStore.get(key, DETAIL_TYPE);
        if (cached.isPresent()) {
            return cached.get();
        }
        ProductDetailResponse response = mapper.toDetail(findProduct(id));
        cacheStore.put(key, response, productTtl);
        return response;
    }

    @Transactional
    public ProductDetailResponse create(ProductRequest request) {
        Brand brand = requireBrand(request.brandId());
        String name = TextUtils.clean(request.name());
        if (productRepository.existsByBrandIdAndNameIgnoreCase(brand.getId(), name)) {
            throw new DuplicateProductException(brand.getName(), name);
        }
        Product product = new Product();
        apply(product, request, brand, name);
        Product saved = productRepository.save(product);
        events.publishEvent(new ProductChangedEvent(saved.getId(), ProductChangedEvent.ChangeType.CREATED));
        log.info("Created product {} '{}'", saved.getId(), saved.getName());
        return mapper.toDetail(saved);
    }

    @Transactional
    public ProductDetailResponse update(Long id, ProductRequest request) {
        Product product = findProduct(id);
        if (request.version() != null && !request.version().equals(product.getVersion())) {
            throw new VersionConflictException(id, request.version(), product.getVersion());
        }
        Brand brand = requireBrand(request.brandId());
        String name = TextUtils.clean(request.name());
        if (productRepository.existsByBrandIdAndNameIgnoreCaseAndIdNot(brand.getId(), name, id)) {
            throw new DuplicateProductException(brand.getName(), name);
        }
        apply(product, request, brand, name);
        Product saved = productRepository.saveAndFlush(product);
        events.publishEvent(new ProductChangedEvent(id, ProductChangedEvent.ChangeType.UPDATED));
        log.info("Updated product {} (version {})", id, saved.getVersion());
        return mapper.toDetail(saved);
    }

    @Transactional
    public ProductDetailResponse updateStock(Long id, StockUpdateRequest request) {
        Product product = findProduct(id);
        int previous = product.getStockQuantity();
        product.setStockQuantity(request.stockQuantity());
        Product saved = productRepository.saveAndFlush(product);
        events.publishEvent(new ProductChangedEvent(id, ProductChangedEvent.ChangeType.STOCK_CHANGED));
        log.info("Stock of product {} changed {} -> {}", id, previous, request.stockQuantity());
        return mapper.toDetail(saved);
    }

    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
        productRepository.delete(product);
        events.publishEvent(new ProductChangedEvent(id, ProductChangedEvent.ChangeType.DELETED));
        log.info("Deleted product {}", id);
    }

    private Product findProduct(Long id) {
        return productRepository.findWithDetailsById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    private Brand requireBrand(Long brandId) {
        return brandRepository.findById(brandId)
                .orElseThrow(() -> new InvalidProductException("Brand " + brandId + " does not exist"));
    }

    private void apply(Product product, ProductRequest request, Brand brand, String name) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new InvalidProductException("Category " + request.categoryId() + " does not exist"));
        if (!category.isSubcategory()) {
            throw new InvalidProductException("categoryId must reference a subcategory (for example Running Shoes), "
                    + "not the top-level category '" + category.getName() + "'");
        }
        List<Color> colors = colorRepository.findAllById(request.colorIds());
        if (colors.size() != request.colorIds().size()) {
            Set<Long> missing = new HashSet<>(request.colorIds());
            colors.forEach(c -> missing.remove(c.getId()));
            throw new InvalidProductException("Unknown colorIds: " + missing);
        }

        product.setName(name);
        product.setDescription(request.description().trim());
        product.setBrand(brand);
        product.setCategory(category);
        product.setPrice(request.price());
        product.setDiscountPercent(request.discountPercent() == null ? BigDecimal.ZERO : request.discountPercent());
        product.setRating(request.rating());
        product.setReviewCount(request.reviewCount() == null ? 0 : request.reviewCount());
        product.setStockQuantity(request.stockQuantity());
        if (request.popularityScore() != null) {
            product.setPopularityScore(request.popularityScore());
        }
        product.setImageUrl(TextUtils.clean(request.imageUrl()));
        product.replaceColors(new LinkedHashSet<>(colors));
        product.replaceSizes(request.sizes() == null ? Set.of()
                : request.sizes().stream().map(TextUtils::clean).collect(Collectors.toCollection(LinkedHashSet::new)));
    }
}
