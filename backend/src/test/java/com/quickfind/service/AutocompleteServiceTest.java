package com.quickfind.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.quickfind.cache.CacheKeys;
import com.quickfind.cache.CacheStore;
import com.quickfind.dto.response.SuggestionResponse;
import com.quickfind.exception.InvalidSearchRequestException;
import com.quickfind.repository.ProductNameView;
import com.quickfind.repository.ProductRepository;
import com.quickfind.repository.SearchTermCountView;
import com.quickfind.repository.UserInteractionRepository;
import com.quickfind.support.TestProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AutocompleteServiceTest {

    private ProductRepository productRepository;
    private UserInteractionRepository interactionRepository;
    private CacheStore cacheStore;
    private AutocompleteService service;

    private static ProductNameView view(long id, String name, double popularity, String brand, String category) {
        return new ProductNameView() {
            public Long getId() { return id; }
            public String getName() { return name; }
            public double getPopularityScore() { return popularity; }
            public String getBrandName() { return brand; }
            public String getCategoryName() { return category; }
        };
    }

    private static SearchTermCountView term(String text, long hits) {
        return new SearchTermCountView() {
            public String getTerm() { return text; }
            public Long getHits() { return hits; }
        };
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        productRepository = mock(ProductRepository.class);
        interactionRepository = mock(UserInteractionRepository.class);
        cacheStore = mock(CacheStore.class);
        when(cacheStore.get(anyString(), any(TypeReference.class))).thenReturn(Optional.empty());
        when(productRepository.findAllNameViews()).thenReturn(List.of(
                view(1, "Nike Pegasus 41 Running Shoes", 70, "Nike", "Running Shoes"),
                view(2, "Nike Dri-FIT Everyday Running Socks", 60, "Nike", "Sports Accessories"),
                view(3, "Adidas Own The Run Running Shorts", 50, "Adidas", "Shorts")));
        when(interactionRepository.findTopSearchTerms(any(Pageable.class)))
                .thenReturn(List.of(term("running shoes", 5)));
        service = new AutocompleteService(productRepository, interactionRepository, cacheStore, TestProperties.defaults());
        service.rebuild();
    }

    @Test
    void runSuggestsRunningPhrasesWithTheCategoryLabelWinning() {
        SuggestionResponse response = service.suggest("run", 8);
        List<String> texts = response.suggestions().stream().map(SuggestionResponse.Item::text).toList();
        assertThat(texts).startsWith("Running Shoes");
        assertThat(texts).contains("running socks", "running shorts");
    }

    @Test
    void brandPrefixReturnsBrandFirstThenProducts() {
        SuggestionResponse response = service.suggest("nik", 5);
        assertThat(response.suggestions().get(0).text()).isEqualTo("Nike");
        assertThat(response.suggestions().get(0).type()).isEqualTo("BRAND");
        assertThat(response.suggestions()).anyMatch(s -> s.productId() != null && s.productId() == 1L);
    }

    @Test
    void rebuildBumpsSuggestionCacheVersion() {
        verify(cacheStore).bumpVersion(CacheKeys.SUGGESTION_VERSION);
    }

    @Test
    void resultsAreCachedUnderAVersionedKey() {
        when(cacheStore.version(CacheKeys.SUGGESTION_VERSION)).thenReturn(3L);
        SuggestionResponse response = service.suggest("Run", 8);
        verify(cacheStore).put(eq("suggest:v3:8:run"), eq(response), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void cachedResponseIsReturnedWithoutTouchingTheTrie() {
        SuggestionResponse cached = new SuggestionResponse("zz", List.of());
        when(cacheStore.get(eq("suggest:v0:8:zz"), any(TypeReference.class))).thenReturn(Optional.of(cached));
        assertThat(service.suggest("zz", 8)).isSameAs(cached);
        verify(cacheStore, never()).put(eq("suggest:v0:8:zz"), any(), any());
    }

    @Test
    void validatesPrefixAndLimit() {
        assertThatThrownBy(() -> service.suggest(null, 5)).isInstanceOf(InvalidSearchRequestException.class);
        assertThatThrownBy(() -> service.suggest("  ", 5)).isInstanceOf(InvalidSearchRequestException.class);
        assertThatThrownBy(() -> service.suggest("run", 0)).isInstanceOf(InvalidSearchRequestException.class);
        assertThatThrownBy(() -> service.suggest("run", 21)).isInstanceOf(InvalidSearchRequestException.class);
    }

    @Test
    void unknownPrefixGivesEmptyList() {
        assertThat(service.suggest("qqq", 5).suggestions()).isEmpty();
    }

    @Test
    void trailingPhrasesDropBrandAndTokensWithDigits() {
        assertThat(AutocompleteService.trailingPhrases("Nike Pegasus 41 Running Shoes", "Nike"))
                .containsExactly("shoes", "running shoes");
        assertThat(AutocompleteService.trailingPhrases("Nike Dri-FIT Everyday Running Socks", "Nike"))
                .containsExactly("socks", "running socks", "everyday running socks");
        assertThat(AutocompleteService.trailingPhrases("American Tourister Casual Laptop Backpack 32L", "American Tourister"))
                .containsExactly("backpack", "laptop backpack", "casual laptop backpack");
        assertThat(AutocompleteService.trailingPhrases("Nike 2000", "Nike")).isEmpty();
    }
}
