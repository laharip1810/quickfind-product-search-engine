# Performance benchmarking

**No numbers in this repository were invented.** The tables below are empty on purpose. Fill them in with results you measure yourself, using the scripts below. Resume or interview claims should only quote numbers from these runs, together with the hardware they ran on.

## What is measured

| Scenario | Endpoint | Path through the system |
|---|---|---|
| Text query | `search?query=black running shoes` | SQL candidate scan (LIKE) → Java scoring → in-memory sort/page |
| Broad query | `search?query=shoes` | Same, but many candidates (tests the 2,000-candidate cap) |
| Filters only | `search?category=Footwear&minPrice=2000&maxPrice=5000&sort=price_asc` | Pure SQL: indexed WHERE + ORDER BY + LIMIT |
| Query + filters | `search?query=running&category=Footwear&minRating=4&sort=price_asc` | Filters shrink the candidate set before scoring |
| Autocomplete | `search/suggestions?prefix=run` | Redis hit, otherwise an in-memory Trie lookup |
| Product detail | `products/{id}` | Redis hit, otherwise a MySQL primary-key lookup + 2 batch loads |
| Recommendations | `products/{id}/recommendations` | Redis hit, otherwise co-interaction query + ≤ 500 candidates scored |
| Trending | `products/trending` | Redis hit, otherwise a scan of the 7-day event window |

For each scenario the scripts record the **first (cold) request** after flushing Redis separately (a cache miss, plus JIT warm-up for the first run), then N warm requests. They report average, p50, p95 and max.

## How to run

```bash
# 1. Start the stack (Redis on, all indexes)
cp .env.example .env
docker compose up -d --build

# 2. Baseline: 114 seed products
scripts/benchmark.sh "114 products, V2 indexes, Redis on" 50

# 3. Grow the catalog and repeat. Generated rows add up: 10k, +40k = 50k, +50k = 100k.
scripts/generate-test-data.sh 10000
scripts/benchmark.sh "10k products, V2 indexes, Redis on" 50
scripts/generate-test-data.sh 40000
scripts/benchmark.sh "50k products, V2 indexes, Redis on" 50
scripts/generate-test-data.sh 50000
scripts/benchmark.sh "100k products, V2 indexes, Redis on" 50
```

On Windows PowerShell, use `.\scripts\generate-test-data.ps1 -Count 10000` and `.\scripts\benchmark.ps1 -Label "10k products" -Iterations 50`.

Results are written to `benchmark-results/<timestamp>.md`, which is git-ignored. Copy the tables you want to keep into this file.

### Indexes: before and after

```bash
docker compose down -v                                   # fresh database
export FLYWAY_TARGET=1                                   # V1 only (FK and unique indexes)
docker compose up -d --build
scripts/generate-test-data.sh 100000                     # the generator must also run with target 1
scripts/benchmark.sh "100k products, V1 only" 50
docker compose exec mysql sh -c 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' < scripts/explain-queries.sql > explain-v1.txt

unset FLYWAY_TARGET
docker compose up -d                                     # backend is recreated; Flyway applies V2 on startup
scripts/benchmark.sh "100k products, V1 + V2" 50
docker compose exec mysql sh -c 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' < scripts/explain-queries.sql > explain-v2.txt
```

On PowerShell, use `$env:FLYWAY_TARGET="1"` instead of `export`, and `Remove-Item Env:FLYWAY_TARGET` instead of `unset`.

Compare the `EXPLAIN ANALYZE` output as well as the timings. Look for `Index range scan on p using idx_products_category_sale_price` replacing `Table scan` + `Sort`.

### Cached vs uncached

- **Per request:** the "Cold" column is the cache miss; the warm columns are hits for the cached scenarios.
- **Whole run:** set `CACHE_ENABLED=false` in `.env`, run `docker compose up -d backend`, run the benchmark, then switch it back.
- **Hit/miss counts:** `curl "localhost:8080/actuator/metrics/quickfind.cache.requests?tag=result:hit"` (also `result:miss`, `result:error`, and `tag=cache:product` for a single cache).
- **Database query time:** `explain-queries.sql` shows per-query execution time. For every statement Hibernate runs, temporarily set `logging.level.org.hibernate.SQL=DEBUG` and `spring.jpa.properties.hibernate.generate_statistics=true`.

## Results

Record the machine as well: CPU, RAM, OS, Docker Desktop version. Docker Desktop on Windows/macOS adds virtualisation overhead, so compare runs on the same machine only.

**Machine:** _fill in_

| Catalog | Indexes | Cache | Text query p50 / p95 | Broad query p50 / p95 | Filters only p50 / p95 | Detail cold → warm | Recos cold → warm |
|---|---|---|---|---|---|---|---|
| 114 | V1+V2 | on | | | | | |
| 10k | V1+V2 | on | | | | | |
| 50k | V1+V2 | on | | | | | |
| 100k | V1 only | on | | | | | |
| 100k | V1+V2 | on | | | | | |
| 100k | V1+V2 | off | | | | | |

## What to expect, and why

These are predictions from the design, to be confirmed or refuted by the measurements.

- **Filters-only browsing** should stay nearly flat as the catalog grows once V2 is applied, because MySQL can walk `(category_id, sale_price)` in order and stop after `LIMIT`. Without V2 it has to read and sort every row in the category.
- **Text queries grow roughly linearly with catalog size.** `LIKE '%token%'` cannot use a B-tree index, so candidate retrieval scans. The popularity index lets MySQL stop at 2,000 matches when sorting by popularity, which helps common words more than rare ones. This is the known bottleneck, and at a larger scale it is the reason to move search to an inverted index (Elasticsearch/OpenSearch). The response flag `candidatesTruncated` shows when the cap was hit.
- **Java scoring** is O(candidates × query words × field words), bounded by the 2,000 cap, so it should be a small, roughly constant share of the time.
- **Cached endpoints** (detail, suggestions, recommendations, trending) should be dominated by HTTP overhead on a hit, and independent of catalog size.
- **Autocomplete** on a miss is an in-memory Trie lookup, O(prefix + subtree log k). It should not depend on catalog size, although a larger catalog makes Trie rebuilds (after writes) take longer.

## Known limits

- The Trie and the price index live in each backend instance's memory, and rebuild after writes (about 2 s later for the Trie; lazily for prices).
- Search results are not cached (there are too many filter combinations for a good hit rate). If measurements show hot repeated queries, a short-TTL cache keyed by the normalised request is an easy addition.
- There is no protection against cache stampedes (many concurrent misses on one key). This is acceptable at demo scale; request coalescing or early refresh would be the fix.
