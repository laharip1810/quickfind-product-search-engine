package com.quickfind.service;

import com.quickfind.cache.CacheKeys;
import com.quickfind.cache.CacheStore;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CatalogChangeListenerTest {

    @Test
    void productChangeEvictsDetailBumpsVersionAndSchedulesRebuilds() {
        CacheStore cache = mock(CacheStore.class);
        AutocompleteService autocomplete = mock(AutocompleteService.class);
        PriceIndexService priceIndex = mock(PriceIndexService.class);
        CatalogChangeListener listener = new CatalogChangeListener(cache, autocomplete, priceIndex);

        listener.onProductChanged(new ProductChangedEvent(42L, ProductChangedEvent.ChangeType.UPDATED));

        verify(cache).evict("product:42");
        verify(cache).bumpVersion(CacheKeys.CATALOG_VERSION);
        verify(autocomplete).markDirty();
        verify(priceIndex).markDirty();
    }
}
