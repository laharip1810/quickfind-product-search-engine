package com.quickfind.cache;

import com.fasterxml.jackson.core.type.TypeReference;

import java.time.Duration;
import java.util.Optional;

/**
 * Minimal cache-aside API. Implementations must never throw: a cache failure is a miss,
 * never a failed request.
 */
public interface CacheStore {

    <T> Optional<T> get(String key, TypeReference<T> type);

    void put(String key, Object value, Duration ttl);

    void evict(String key);

    /** Current value of a version counter (0 if unset or unavailable). */
    long version(String versionKey);

    /** Increments a version counter, orphaning every key built from the old version. */
    void bumpVersion(String versionKey);
}
