package com.quickfind.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/** Used when quickfind.cache.enabled=false (tests, the h2 demo profile, benchmark baselines). */
@Component
@ConditionalOnProperty(prefix = "quickfind.cache", name = "enabled", havingValue = "false")
public class NoOpCacheStore implements CacheStore {

    @Override
    public <T> Optional<T> get(String key, TypeReference<T> type) {
        return Optional.empty();
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        // caching disabled
    }

    @Override
    public void evict(String key) {
        // caching disabled
    }

    @Override
    public long version(String versionKey) {
        return 0L;
    }

    @Override
    public void bumpVersion(String versionKey) {
        // caching disabled
    }
}
