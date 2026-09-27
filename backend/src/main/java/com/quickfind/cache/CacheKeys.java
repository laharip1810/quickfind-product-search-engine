package com.quickfind.cache;

/**
 * Every Redis key the application uses, in one place.
 *
 * <p>Suggestion and recommendation keys embed a version number. A catalog change bumps the
 * version, which invalidates all old entries in O(1) (they simply stop being read and
 * expire through their TTL). The alternative, deleting keys by pattern, needs SCAN/KEYS
 * over the keyspace, and it is easy to miss a key: renaming one product changes the
 * suggestions for every prefix of its old and new name.
 */
public final class CacheKeys {

    /** Bumped on every product create, update, stock change or delete. */
    public static final String CATALOG_VERSION = "catalog:version";

    /** Bumped every time the autocomplete Trie is rebuilt. */
    public static final String SUGGESTION_VERSION = "suggest:version";

    private CacheKeys() {
    }

    public static String product(long id) {
        return "product:" + id;
    }

    public static String suggestions(long version, String normalizedPrefix, int limit) {
        return "suggest:v" + version + ":" + limit + ":" + normalizedPrefix;
    }

    public static String recommendations(long version, long productId, Long userId, int limit) {
        return "reco:v" + version + ":" + productId + ":" + (userId == null ? "anon" : userId) + ":" + limit;
    }

    public static String trending(long version, int limit) {
        return "trending:v" + version + ":" + limit;
    }

    /** Cache name used as a metric tag: the part before the first ':'. */
    public static String cacheName(String key) {
        int idx = key.indexOf(':');
        return idx < 0 ? key : key.substring(0, idx);
    }
}
