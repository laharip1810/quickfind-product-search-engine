package com.quickfind.service;

import com.quickfind.dto.request.SearchRequest;
import com.quickfind.exception.ErrorCode;
import com.quickfind.exception.InvalidSearchRequestException;
import com.quickfind.search.QueryNormalizer;
import com.quickfind.search.SortOption;
import com.quickfind.support.TestProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SearchRequestValidatorTest {

    private CatalogLookupService lookup;
    private SearchRequestValidator validator;

    @BeforeEach
    void setUp() {
        lookup = mock(CatalogLookupService.class);
        when(lookup.categoryByName(anyString())).thenReturn(Optional.empty());
        when(lookup.categoryById(anyLong())).thenReturn(Optional.empty());
        when(lookup.brandIdByName(anyString())).thenReturn(Optional.empty());
        when(lookup.colorIdByName(anyString())).thenReturn(Optional.empty());
        when(lookup.categoryByName("Footwear")).thenReturn(Optional.of(
                new CatalogLookupService.CategoryInfo(1L, "Footwear", null, Set.of(10L, 11L))));
        when(lookup.categoryByName("Running Shoes")).thenReturn(Optional.of(
                new CatalogLookupService.CategoryInfo(10L, "Running Shoes", 1L, Set.of())));
        when(lookup.brandIdByName("Nike")).thenReturn(Optional.of(100L));
        when(lookup.brandIdByName("Puma")).thenReturn(Optional.of(101L));
        when(lookup.colorIdByName("Black")).thenReturn(Optional.of(7L));
        validator = new SearchRequestValidator(lookup, new QueryNormalizer(), TestProperties.defaults());
    }

    private static SearchRequest request(String query, String category, List<String> brands, String color,
                                         BigDecimal min, BigDecimal max, BigDecimal rating, String sort,
                                         Integer page, Integer size) {
        return new SearchRequest(query, category, brands, color, null, min, max, rating, null, sort, page, size);
    }

    @Test
    void appliesDefaultsAndNormalisesQuery() {
        SearchCriteria c = validator.validate(request("  Black Running SHOES ", null, null, null,
                null, null, null, null, null, null));
        assertThat(c.tokens()).containsExactly("black", "running", "shoe");
        assertThat(c.sort()).isEqualTo(SortOption.RELEVANCE);
        assertThat(c.page()).isZero();
        assertThat(c.size()).isEqualTo(20);
    }

    @Test
    void resolvesTopLevelCategoryToItsSubcategories() {
        SearchCriteria c = validator.validate(request(null, "Footwear", null, null, null, null, null, null, 0, 10));
        assertThat(c.categoryIds()).containsExactlyInAnyOrder(10L, 11L);
        assertThat(c.hasQuery()).isFalse();
    }

    @Test
    void resolvesSubcategoryBrandsAndColor() {
        SearchCriteria c = validator.validate(request(null, "Running Shoes", List.of("Nike", "Puma"), "Black",
                null, null, null, "price_asc", 0, 10));
        assertThat(c.categoryIds()).containsExactly(10L);
        assertThat(c.brandIds()).containsExactlyInAnyOrder(100L, 101L);
        assertThat(c.colorIds()).containsExactly(7L);
        assertThat(c.sort()).isEqualTo(SortOption.PRICE_ASC);
    }

    @Test
    void rejectsInvalidSortWithAllowedValues() {
        assertThatThrownBy(() -> validator.validate(request(null, null, null, null, null, null, null, "cheapest", 0, 10)))
                .isInstanceOf(InvalidSearchRequestException.class)
                .hasMessageContaining("Allowed")
                .extracting(e -> ((InvalidSearchRequestException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_SORT);
    }

    @Test
    void rejectsInvertedPriceRangeAndBadRating() {
        assertThatThrownBy(() -> validator.validate(request(null, null, null, null,
                new BigDecimal("5000"), new BigDecimal("2000"), null, null, 0, 10)))
                .hasMessageContaining("minPrice");
        assertThatThrownBy(() -> validator.validate(request(null, null, null, null,
                new BigDecimal("-1"), null, null, null, 0, 10)))
                .hasMessageContaining("minPrice");
        assertThatThrownBy(() -> validator.validate(request(null, null, null, null,
                null, null, new BigDecimal("6"), null, 0, 10)))
                .hasMessageContaining("minRating");
    }

    @Test
    void rejectsUnknownFilterValues() {
        assertThatThrownBy(() -> validator.validate(request(null, "Spaceships", null, null, null, null, null, null, 0, 10)))
                .hasMessageContaining("Unknown category");
        assertThatThrownBy(() -> validator.validate(request(null, null, List.of("Acme"), null, null, null, null, null, 0, 10)))
                .hasMessageContaining("Unknown brand");
        assertThatThrownBy(() -> validator.validate(request(null, null, null, "Plaid", null, null, null, null, 0, 10)))
                .hasMessageContaining("Unknown color");
    }

    @Test
    void rejectsBadPaging() {
        assertThatThrownBy(() -> validator.validate(request(null, null, null, null, null, null, null, null, -1, 10)))
                .hasMessageContaining("page");
        assertThatThrownBy(() -> validator.validate(request(null, null, null, null, null, null, null, null, 0, 0)))
                .hasMessageContaining("size");
        assertThatThrownBy(() -> validator.validate(request(null, null, null, null, null, null, null, null, 0, 101)))
                .hasMessageContaining("size");
    }

    @Test
    void rejectsOverlongQuery() {
        String longQuery = "a".repeat(SearchRequestValidator.MAX_QUERY_LENGTH + 1);
        assertThatThrownBy(() -> validator.validate(request(longQuery, null, null, null, null, null, null, null, 0, 10)))
                .hasMessageContaining("query");
    }
}
