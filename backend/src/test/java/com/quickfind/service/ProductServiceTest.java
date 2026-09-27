package com.quickfind.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.quickfind.cache.CacheStore;
import com.quickfind.dto.request.StockUpdateRequest;
import com.quickfind.dto.response.ProductDetailResponse;
import com.quickfind.entity.Brand;
import com.quickfind.entity.Category;
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
import com.quickfind.search.QueryNormalizer;
import com.quickfind.support.TestData;
import com.quickfind.support.TestProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProductServiceTest {

    private ProductRepository productRepository;
    private BrandRepository brandRepository;
    private CategoryRepository categoryRepository;
    private ColorRepository colorRepository;
    private CacheStore cacheStore;
    private ApplicationEventPublisher events;
    private ProductService service;

    private final Category footwear = TestData.category(1, "Footwear", null);
    private final Category runningShoes = TestData.category(10, "Running Shoes", footwear);
    private final Brand nike = TestData.brand(100, "Nike");

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        brandRepository = mock(BrandRepository.class);
        categoryRepository = mock(CategoryRepository.class);
        colorRepository = mock(ColorRepository.class);
        cacheStore = mock(CacheStore.class);
        events = mock(ApplicationEventPublisher.class);
        SearchRequestValidator validator = new SearchRequestValidator(
                mock(CatalogLookupService.class), new QueryNormalizer(), TestProperties.defaults());
        service = new ProductService(productRepository, brandRepository, categoryRepository, colorRepository,
                new ProductMapper(), cacheStore, validator, events, TestProperties.defaults());

        when(brandRepository.findById(100L)).thenReturn(Optional.of(nike));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(runningShoes));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(footwear));
        when(colorRepository.findAllById(any())).thenReturn(List.of(TestData.color(1, "Black")));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @SuppressWarnings("unchecked")
    void cacheHitDoesNotTouchTheDatabase() {
        ProductDetailResponse cached = new ProductMapper().toDetail(
                TestData.product(1, "Nike Pegasus", nike, runningShoes, 10000, 5));
        when(cacheStore.get(eq("product:1"), any(TypeReference.class))).thenReturn(Optional.of(cached));

        assertThat(service.get(1L)).isSameAs(cached);
        verifyNoInteractions(productRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void cacheMissLoadsFromDatabaseAndPopulatesCache() {
        when(cacheStore.get(anyString(), any(TypeReference.class))).thenReturn(Optional.empty());
        when(productRepository.findWithDetailsById(1L))
                .thenReturn(Optional.of(TestData.product(1, "Nike Pegasus", nike, runningShoes, 10000, 5)));

        ProductDetailResponse response = service.get(1L);

        assertThat(response.name()).isEqualTo("Nike Pegasus");
        assertThat(response.category()).isEqualTo("Footwear");
        assertThat(response.subcategory()).isEqualTo("Running Shoes");
        assertThat(response.salePrice()).isEqualByComparingTo(new BigDecimal("9000.00"));
        verify(cacheStore).put(eq("product:1"), eq(response), eq(Duration.ofMinutes(30)));
    }

    @Test
    @SuppressWarnings("unchecked")
    void missingProductIsNotFoundAndNothingIsCached() {
        when(cacheStore.get(anyString(), any(TypeReference.class))).thenReturn(Optional.empty());
        when(productRepository.findWithDetailsById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L)).isInstanceOf(ProductNotFoundException.class);
        verify(cacheStore, never()).put(anyString(), any(), any());
    }

    @Test
    void createPublishesChangeEvent() {
        when(productRepository.existsByBrandIdAndNameIgnoreCase(100L, "Nike Test Runner")).thenReturn(false);

        ProductDetailResponse created = service.create(TestData.productRequest("  Nike   Test Runner ", 100, 10, null));

        assertThat(created.name()).isEqualTo("Nike Test Runner");
        assertThat(created.salePrice()).isEqualByComparingTo(new BigDecimal("4499.10"));
        verify(events).publishEvent(any(ProductChangedEvent.class));
    }

    @Test
    void createRejectsTopLevelCategory() {
        assertThatThrownBy(() -> service.create(TestData.productRequest("Nike Test Runner", 100, 1, null)))
                .isInstanceOf(InvalidProductException.class)
                .hasMessageContaining("subcategory");
    }

    @Test
    void createRejectsUnknownBrand() {
        when(brandRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(TestData.productRequest("Nike Test Runner", 999, 10, null)))
                .isInstanceOf(InvalidProductException.class)
                .hasMessageContaining("Brand 999");
    }

    @Test
    void createRejectsDuplicateNameForSameBrand() {
        when(productRepository.existsByBrandIdAndNameIgnoreCase(100L, "Nike Test Runner")).thenReturn(true);
        assertThatThrownBy(() -> service.create(TestData.productRequest("Nike Test Runner", 100, 10, null)))
                .isInstanceOf(DuplicateProductException.class);
        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void updateWithStaleVersionIsRejected() {
        when(productRepository.findWithDetailsById(1L))
                .thenReturn(Optional.of(TestData.product(1, "Nike Pegasus", nike, runningShoes, 10000, 5)));
        assertThatThrownBy(() -> service.update(1L, TestData.productRequest("Nike Pegasus", 100, 10, 7L)))
                .isInstanceOf(VersionConflictException.class);
        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void updateStockPublishesEventSoCachesAreInvalidated() {
        when(productRepository.findWithDetailsById(1L))
                .thenReturn(Optional.of(TestData.product(1, "Nike Pegasus", nike, runningShoes, 10000, 5)));
        ProductDetailResponse updated = service.updateStock(1L, new StockUpdateRequest(0));
        assertThat(updated.inStock()).isFalse();
        verify(events).publishEvent(new ProductChangedEvent(1L, ProductChangedEvent.ChangeType.STOCK_CHANGED));
    }
}
