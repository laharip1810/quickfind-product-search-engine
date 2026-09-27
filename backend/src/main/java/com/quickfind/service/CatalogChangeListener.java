package com.quickfind.service;

import com.quickfind.cache.CacheKeys;
import com.quickfind.cache.CacheStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Keeps derived data consistent after a catalog change.
 *
 * <p>Runs AFTER the transaction commits. Evicting before the commit would leave a window
 * where a concurrent request reads the old row from MySQL and puts it straight back into
 * Redis, so the stale value would survive until its TTL.
 */
@Component
public class CatalogChangeListener {

    private static final Logger log = LoggerFactory.getLogger(CatalogChangeListener.class);

    private final CacheStore cacheStore;
    private final AutocompleteService autocompleteService;
    private final PriceIndexService priceIndexService;

    public CatalogChangeListener(CacheStore cacheStore, AutocompleteService autocompleteService,
                                 PriceIndexService priceIndexService) {
        this.cacheStore = cacheStore;
        this.autocompleteService = autocompleteService;
        this.priceIndexService = priceIndexService;
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void onProductChanged(ProductChangedEvent event) {
        cacheStore.evict(CacheKeys.product(event.productId()));
        cacheStore.bumpVersion(CacheKeys.CATALOG_VERSION);
        autocompleteService.markDirty();
        priceIndexService.markDirty();
        log.info("Product {} {}: evicted product cache, bumped catalog version, scheduled index rebuilds",
                event.productId(), event.changeType());
    }
}
