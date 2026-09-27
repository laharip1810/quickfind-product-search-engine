package com.quickfind.controller;

import com.quickfind.dto.request.ProductRequest;
import com.quickfind.dto.request.StockUpdateRequest;
import com.quickfind.dto.response.PagedResponse;
import com.quickfind.dto.response.PriceRangeCountResponse;
import com.quickfind.dto.response.ProductDetailResponse;
import com.quickfind.dto.response.ProductSummaryResponse;
import com.quickfind.service.PriceIndexService;
import com.quickfind.service.ProductService;
import com.quickfind.service.TrendingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

/**
 * Product catalog endpoints. Controllers only translate HTTP to service calls.
 *
 * <p>The write endpoints (POST/PUT/PATCH/DELETE) are the admin surface. They are open in
 * the demo; adding Spring Security means restricting exactly these methods to ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final TrendingService trendingService;
    private final PriceIndexService priceIndexService;

    public ProductController(ProductService productService, TrendingService trendingService,
                             PriceIndexService priceIndexService) {
        this.productService = productService;
        this.trendingService = trendingService;
        this.priceIndexService = priceIndexService;
    }

    @GetMapping
    public PagedResponse<ProductSummaryResponse> list(@RequestParam(required = false) Integer page,
                                                      @RequestParam(required = false) Integer size,
                                                      @RequestParam(required = false) String sort) {
        return productService.list(page, size, sort);
    }

    @GetMapping("/{id}")
    public ProductDetailResponse get(@PathVariable Long id) {
        return productService.get(id);
    }

    @PostMapping
    public ResponseEntity<ProductDetailResponse> create(@Valid @RequestBody ProductRequest request) {
        ProductDetailResponse created = productService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ProductDetailResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @PatchMapping("/{id}/stock")
    public ProductDetailResponse updateStock(@PathVariable Long id, @Valid @RequestBody StockUpdateRequest request) {
        return productService.updateStock(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/trending")
    public List<ProductSummaryResponse> trending(@RequestParam(required = false) Integer limit) {
        return trendingService.trending(limit);
    }

    @GetMapping("/price-range-count")
    public PriceRangeCountResponse priceRangeCount(@RequestParam(required = false) String category,
                                                   @RequestParam(required = false) BigDecimal minPrice,
                                                   @RequestParam(required = false) BigDecimal maxPrice) {
        return priceIndexService.count(category, minPrice, maxPrice);
    }
}
