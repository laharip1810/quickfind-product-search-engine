package com.quickfind.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickfind.config.QuickFindProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;

/**
 * Redis-backed cache: JSON values, a TTL on every key, fail-open behaviour.
 *
 * <p>If Redis is down, the request falls through to MySQL. To avoid paying the connection
 * timeout on every request, Redis is skipped for a short back-off window after a failure.
 * Hits, misses and bypasses are counted in the "quickfind.cache.requests" metric
 * (visible at /actuator/metrics/quickfind.cache.requests).
 */
@Component
@ConditionalOnProperty(prefix = "quickfind.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RedisCacheStore implements CacheStore {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheStore.class);
    private static final String METRIC = "quickfind.cache.requests";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final Clock clock;
    private final Duration failureBackoff;
    private volatile long unavailableUntilMillis;

    public RedisCacheStore(StringRedisTemplate redis, ObjectMapper objectMapper, MeterRegistry meterRegistry,
                           Clock clock, QuickFindProperties properties) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.clock = clock;
        this.failureBackoff = properties.cache().failureBackoff();
    }

    @Override
    public <T> Optional<T> get(String key, TypeReference<T> type) {
        String cache = CacheKeys.cacheName(key);
        if (isBackingOff()) {
            count(cache, "bypass");
            return Optional.empty();
        }
        try {
            String json = redis.opsForValue().get(key);
            if (json == null) {
                count(cache, "miss");
                log.debug("Cache MISS {}", key);
                return Optional.empty();
            }
            T value = objectMapper.readValue(json, type);
            count(cache, "hit");
            log.debug("Cache HIT {}", key);
            return Optional.ofNullable(value);
        } catch (JsonProcessingException e) {
            log.warn("Unreadable cache entry {} (evicting): {}", key, e.getOriginalMessage());
            evict(key);
            count(cache, "miss");
            return Optional.empty();
        } catch (RuntimeException e) {
            markUnavailable("get", e);
            count(cache, "error");
            return Optional.empty();
        }
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        if (value == null || isBackingOff()) {
            return;
        }
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (JsonProcessingException e) {
            log.warn("Could not serialise value for cache key {}: {}", key, e.getOriginalMessage());
        } catch (RuntimeException e) {
            markUnavailable("put", e);
        }
    }

    @Override
    public void evict(String key) {
        if (isBackingOff()) {
            return;
        }
        try {
            redis.delete(key);
            log.debug("Cache EVICT {}", key);
        } catch (RuntimeException e) {
            markUnavailable("evict", e);
        }
    }

    @Override
    public long version(String versionKey) {
        if (isBackingOff()) {
            return 0L;
        }
        try {
            String value = redis.opsForValue().get(versionKey);
            return value == null ? 0L : Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        } catch (RuntimeException e) {
            markUnavailable("version", e);
            return 0L;
        }
    }

    @Override
    public void bumpVersion(String versionKey) {
        if (isBackingOff()) {
            return;
        }
        try {
            Long next = redis.opsForValue().increment(versionKey);
            log.debug("Cache version {} -> {}", versionKey, next);
        } catch (RuntimeException e) {
            markUnavailable("bumpVersion", e);
        }
    }

    private boolean isBackingOff() {
        return clock.millis() < unavailableUntilMillis;
    }

    private void markUnavailable(String operation, RuntimeException e) {
        unavailableUntilMillis = clock.millis() + failureBackoff.toMillis();
        log.warn("Redis {} failed ({}); serving from the database for the next {}s",
                operation, e.getClass().getSimpleName(), failureBackoff.toSeconds());
    }

    private void count(String cache, String result) {
        meterRegistry.counter(METRIC, "cache", cache, "result", result).increment();
    }
}
