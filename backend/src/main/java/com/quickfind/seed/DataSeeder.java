package com.quickfind.seed;

import com.quickfind.cache.CacheKeys;
import com.quickfind.cache.CacheStore;
import com.quickfind.service.CatalogLookupService;
import com.quickfind.service.PriceIndexService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Loads the demo catalog on startup when the products table is empty, so the app works
 * immediately after `docker compose up`. Disable with SEED_ENABLED=false.
 * The autocomplete Trie is built afterwards, on ApplicationReadyEvent.
 */
@Component
@Order(1)
@ConditionalOnProperty(prefix = "quickfind.seed", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements ApplicationRunner {

    private final SeedService seedService;
    private final CatalogLookupService lookup;
    private final PriceIndexService priceIndexService;
    private final CacheStore cacheStore;

    public DataSeeder(SeedService seedService, CatalogLookupService lookup, PriceIndexService priceIndexService,
                      CacheStore cacheStore) {
        this.seedService = seedService;
        this.lookup = lookup;
        this.priceIndexService = priceIndexService;
        this.cacheStore = cacheStore;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (seedService.seedIfEmpty()) {
            lookup.refresh();
            priceIndexService.markDirty();
            cacheStore.bumpVersion(CacheKeys.CATALOG_VERSION);
        }
    }
}
