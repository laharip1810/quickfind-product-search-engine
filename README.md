# QuickFind: Product Search & Recommendation Engine

QuickFind is a product discovery platform. A shopper types *"black running shoes"* and gets relevance-ranked results with filters, sorting and pagination, as-you-type suggestions from a Trie, and recommendations that explain why each product was picked. Under the hood it is a layered Spring Boot service with MySQL, Redis and a React frontend.

It is built as an engineering project, not a tutorial CRUD app. Every algorithm is written out and unit-tested, every design trade-off is documented, and no performance number is claimed until you measure it (see [docs/PERFORMANCE.md](docs/PERFORMANCE.md)).

---

## The problem

Online catalogs have thousands of products and shoppers type short, messy queries. The system has to:

1. understand the query (case, punctuation, plurals, stopwords),
2. find relevant products across name, brand, category and description,
3. apply filters and sorting without returning thousands of rows,
4. suggest completions after a few keystrokes,
5. learn from behaviour (views, searches, wishlists) to recommend related products,
6. handle unavailable products sensibly,
7. stay fast as the catalog grows, and be honest about where it would stop scaling.

## The solution in one picture

```
React SPA ──/api──► Spring Boot ──► MySQL 8 (Flyway schema, B-tree indexes)
                       │   └─────► Redis 7 (cache-aside, versioned keys, fail-open)
                       ├─ Search: normalise → SQL narrows candidates → Java scores, sorts, pages
                       ├─ Autocomplete: in-memory Trie + bounded min-heap (top-k)
                       ├─ Recommendations: weighted, explainable score + diversity cap
                       └─ Interactions: views/searches/wishlist → trending, co-interaction, popularity
```

Full diagrams, packages and the caching design are in **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)**.

## Features

- **Search.** Tokenisation, stopwords and plural folding. An explainable relevance score (name > category > brand > color > description, with a word-coverage multiplier and a phrase bonus). All-terms matching with an any-term fallback. Out-of-stock items are demoted, not hidden.
- **Filters.** Category or subcategory, brand (several at once), color, size, price range, minimum rating and availability, with pagination metadata on every page.
- **Sorting.** Relevance, price ascending and descending, rating and popularity, validated with helpful errors.
- **Autocomplete.** A Trie of product names, name phrases, brands, categories and popular searches, with weighted, top-k results.
- **Recommendations.** Category, complementary category, brand, price, color, popularity, rating, co-interaction and personal interests. Every score comes with a breakdown and readable reasons, and there are at most 3 per subcategory.
- **Trending.** A HashMap counts weighted engagement over the last 7 days, and a PriorityQueue picks the top N.
- **Price facet.** Binary search over sorted prices gives a live "N products in this range" count.
- **Interactions.** PRODUCT_VIEW, SEARCH, ADD_TO_CART and WISHLIST, with the wishlist and recent-activity views.
- **Admin.** Create, edit, delete and restock products, with optimistic locking and field-level validation errors.
- **Redis caching.** Product details, suggestions, recommendations and trending, with invalidation after commit.
- **Consistent errors.** One JSON error format, with no stack traces and a request id on every response and log line.
- **Seed data.** 114 realistic products on first start, plus a bulk generator for 10k to 100k products.
- **Tests.** About 60 framework-free unit tests, Mockito service tests, MockMvc integration tests on H2, a MySQL Testcontainers test and a 42-request Postman collection.

## Tech stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.5 (Web, Data JPA/Hibernate, Validation, Actuator), Maven |
| Database | MySQL 8 with Flyway migrations (H2 in MySQL mode for tests and the demo profile) |
| Cache | Redis 7 (Spring Data Redis / Lettuce) |
| Frontend | React 19, Vite 6, React Router 7, Axios, plain CSS |
| Testing | JUnit 5, Mockito, Spring Boot Test, MockMvc, AssertJ, Testcontainers, JaCoCo, Postman |
| Delivery | Docker multi-stage builds, Docker Compose, nginx (serves the SPA and proxies `/api`) |

---

## Quick start (Docker, recommended)

Prerequisites: Docker Desktop (or Docker Engine with Compose v2) and Git.

```bash
git clone <your-repo-url> quickfind
cd quickfind
cp .env.example .env          # Windows PowerShell: Copy-Item .env.example .env
# edit .env and change DB_PASSWORD and MYSQL_ROOT_PASSWORD
docker compose up --build
```

The first build downloads Maven and npm dependencies, which takes a few minutes. Once `backend` reports healthy:

| What | URL |
|---|---|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080/api/products |
| Health | http://localhost:8080/actuator/health |
| Cache metrics | http://localhost:8080/actuator/metrics/quickfind.cache.requests |

Stop with `docker compose down`. Add `-v` to also delete the MySQL volume and start fresh.

## Running locally without Docker for the app

Prerequisites: **JDK 17+**, **Node.js 20.19+ or 22+**. Maven is not required, because `mvnw` downloads it.

**Option A: zero setup (in-memory H2, no Redis).** Good for a first look:

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
# Windows PowerShell: .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=h2"
```

**Option B: real MySQL + Redis in Docker, app on your machine.** Good for development:

```bash
cp .env.example .env
docker compose up -d mysql redis        # MySQL on localhost:3307, Redis on localhost:6379
cd backend && ./mvnw spring-boot:run    # reads ../.env automatically
```

Then start the frontend in a second terminal:

```bash
cd frontend
npm install
npm run dev                              # http://localhost:5173, proxies /api to localhost:8080
```

### Configuration

All settings come from environment variables. `.env` is loaded by Docker Compose, and by the backend when started with `mvnw`. Never commit `.env`.

| Variable | Default | Used for |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `3307` / `quickfind` | MySQL location (inside Docker: `mysql:3306`) |
| `DB_USERNAME` / `DB_PASSWORD` | – (required) | MySQL credentials; the MySQL container is created with them |
| `MYSQL_ROOT_PASSWORD` | – (required) | MySQL container root password |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Redis location (inside Docker: `redis:6379`) |
| `CACHE_ENABLED` | `true` | `false` switches to a no-op cache |
| `SEED_ENABLED` | `true` | Load the demo catalog into an empty database |
| `FLYWAY_TARGET` | `latest` | `1` = skip the V2 performance indexes (benchmarks) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Only needed when calling the API from another origin |
| `MYSQL_HOST_PORT` / `REDIS_HOST_PORT` / `BACKEND_HOST_PORT` / `FRONTEND_HOST_PORT` | `3307` / `6379` / `8080` / `3000` | Ports published by Docker Compose |

MySQL is published on **3307**, so it doesn't clash with a MySQL server you may already have on 3306. If Redis is not running, the app keeps working from MySQL: cache errors are logged, Redis is skipped for 30 seconds, and `/actuator/health` shows Redis as `DOWN`.

## API

The full reference, with parameters, bodies and error codes, is in **[docs/API.md](docs/API.md)**. The main endpoints:

```
GET    /api/products?page=0&size=20&sort=price_asc
GET    /api/products/{id}
POST   /api/products                     PUT /api/products/{id}
PATCH  /api/products/{id}/stock          DELETE /api/products/{id}
GET    /api/products/search?query=black running shoes&category=Footwear&color=Black&minPrice=2000&maxPrice=12000&minRating=4&page=0&size=20
GET    /api/search/suggestions?prefix=run
GET    /api/products/{id}/recommendations?userId=1
GET    /api/products/trending?limit=8
GET    /api/products/price-range-count?category=Footwear&minPrice=2000&maxPrice=5000
POST   /api/interactions                 {"eventType":"PRODUCT_VIEW","productId":1}
GET    /api/interactions/wishlist        DELETE /api/interactions/wishlist/{productId}
GET    /api/interactions/recent
GET    /api/categories  /api/brands  /api/colors  /api/sizes  /api/users/demo
```

Example requests:

```bash
curl "http://localhost:8080/api/products/search?query=black%20running%20shoes&size=5"
curl "http://localhost:8080/api/search/suggestions?prefix=run"
curl "http://localhost:8080/api/products/search?sort=cheapest"        # 400 INVALID_SORT, lists allowed values
curl -X POST http://localhost:8080/api/interactions -H "Content-Type: application/json" \
     -d '{"eventType":"SEARCH","query":"running shoes"}'
```

**Postman:** import `postman/QuickFind.postman_collection.json`, then run the folders in order. There are 42 requests with test scripts, and the first folder stores the ids that later requests use. From the command line: `npx newman run postman/QuickFind.postman_collection.json`.

## Database

The schema has 8 normalised tables (users, categories with self-referencing subcategories, brands, colors, products, product_sizes, product_colors, user_interactions). It uses foreign keys, unique and CHECK constraints, `DECIMAL` money, a denormalised indexed `sale_price`, and optimistic locking. The ER diagram and index rationale are in **[docs/DATABASE.md](docs/DATABASE.md)**.

## Data structures and algorithms

| Structure | Used for | Complexity |
|---|---|---|
| **Trie** | Autocomplete | lookup O(p); top-k O(p + m log k) |
| **PriorityQueue** (bounded min-heap) | Top-k suggestions, recommendations, trending | O(n log k) |
| **HashMap** | Filter-name resolution, id → entity, frequency counts, co-interaction scores | O(1) average |
| **Comparator chains** | Five sort orders with deterministic tie-breaks | O(n log n) |
| **Binary search** | Price-range facet counts over a sorted array | O(log n) |

Why each one was chosen, and where one was deliberately **not** used (for example, binary search for the price *filter*, where the database index is the better tool), is in **[docs/DSA.md](docs/DSA.md)**.

**Search score:** `Σ(5·name + 4·category + 3·brand + 2·color + 1·description) × coverage + 3 (phrase in name)`, × 0.5 if out of stock.

**Recommendation score:** `0.30 category + 0.15 brand + 0.15 price + 0.15 co-interaction + 0.10 popularity + 0.10 rating + 0.05 color (+0.10 personal interest)`.

## Caching strategy

The pattern is cache-aside with a TTL on every key. Product details are evicted by key after a write commits. Suggestions, recommendations and trending use **versioned keys**, so one write invalidates all of them in O(1) without scanning Redis. Redis errors are treated as misses (fail-open), and hits and misses are counted. Details are in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md#caching-redis).

## Testing

```bash
cd backend
./mvnw verify                  # Windows: .\mvnw.cmd verify
```

This runs every test and writes a coverage report to `backend/target/site/jacoco/index.html` (open it in a browser). No MySQL or Redis is needed: integration tests use H2 in MySQL mode and a no-op cache. If Docker is running, `MySqlIntegrationTest` also runs the real migrations and queries against MySQL 8.4 with Testcontainers; otherwise it is skipped.

| Level | Classes | What they cover |
|---|---|---|
| Algorithm unit tests (no Spring) | `QueryNormalizerTest`, `RelevanceScorerTest`, `SortOptionTest`, `ProductSearchTrieTest`, `PriceRangeIndexTest`, `RecommendationScorerTest`, `DiversityFilterTest`, `CategoryRelationsTest`, `TopKTest`, `SizeOrderTest`, `CatalogSeedDataTest` | Scoring weights and worked examples, plural folding, all sort orders and tie-breaks, Trie insert/prefix/limit/merge/missing prefix, binary-search boundaries, recommendation formula, seed-data integrity |
| Validation | `ProductRequestValidationTest`, `SearchRequestValidatorTest` | Bean Validation rules; sort/filter/paging validation |
| Service (Mockito) | `ProductServiceTest`, `AutocompleteServiceTest`, `CatalogChangeListenerTest` | Cache hit/miss/populate, invalidation events, duplicates, version conflicts, versioned suggestion keys |
| API integration (MockMvc, H2) | `ProductApiIntegrationTest`, `SearchApiIntegrationTest`, `SuggestionApiIntegrationTest`, `RecommendationApiIntegrationTest`, `InteractionApiIntegrationTest`, `CatalogApiIntegrationTest` | Full HTTP → DB paths, including every error case |
| MySQL (Testcontainers) | `MySqlIntegrationTest` | Flyway + Hibernate validation + search + native query on real MySQL |
| API (Postman/Newman) | `postman/QuickFind.postman_collection.json` | 42 requests with assertions against a running stack |

## Performance benchmarking

```bash
scripts/generate-test-data.sh 10000                     # adds 10k products (Docker stack)
scripts/benchmark.sh "10k products, Redis on" 50        # p50/p95 per endpoint, cold vs warm
```

The methodology, before/after-index runs (`FLYWAY_TARGET=1`), cached vs uncached runs, `EXPLAIN ANALYZE` queries and an empty results table to fill in are in **[docs/PERFORMANCE.md](docs/PERFORMANCE.md)**.

## Screenshots

Add your own after running the stack, for example `docs/screenshots/home.png`, `search.png`, `product.png` and `admin.png`, and link them here. The UI has: a search-first home page with trending products; search results with a filter sidebar, active-filter chips, sort and pagination; a product page with explainable recommendations; a wishlist with recent activity; and an admin table with an add/edit form.

## Engineering decisions and trade-offs

- **SQL narrows, Java ranks.** Keeps relevance logic explicit and unit-testable. The cost is a scan for `LIKE '%token%'` and a 2,000-candidate cap, both surfaced in the API (`candidatesTruncated`) and in PERFORMANCE.md.
- **Flyway, not `ddl-auto=update`.** The schema is versioned and reviewable, and the performance indexes are a separate migration so they can be measured.
- **H2 for the default test run, plus a MySQL Testcontainers test.** `mvnw verify` works on any machine, while real-MySQL coverage runs wherever Docker is available.
- **Versioned cache keys and after-commit eviction.** Correct invalidation without scanning Redis, and no stale re-population race.
- **Fail-open cache.** Redis is an optimisation, never a dependency for correctness.
- **Generated product illustrations.** Every product gets an SVG illustration based on its subcategory and color, so there are no third-party image URLs or licensing issues. `imageUrl` is supported when real images exist.
- **No login in the MVP.** A demo user stands in, and admin writes are isolated on `/api/products` writes, so Spring Security plus `users.role` can be added without touching business logic.

## Future improvements

- Elasticsearch/OpenSearch for full-text search (BM25, typo tolerance, synonyms, faceting at scale), with the current scorer kept as a re-ranker.
- Spring Security: JWT login, `ROLE_ADMIN` on write endpoints, and real per-user sessions instead of the demo user.
- Cache-stampede protection (request coalescing) and short-TTL caching of hot search queries, if the benchmarks show they matter.
- Kafka or an outbox table for interactions, with streaming trending counts.
- Shared or distributed autocomplete (for example, a Redis sorted-set per prefix) for multi-instance deployments.
- Learning-to-rank from click-through data once there is enough of it.

---

## Suggested Git workflow

```bash
cd quickfind
git init
git add .
git commit -m "chore: initial import of QuickFind"
git branch -M main
git remote add origin https://github.com/<you>/quickfind.git
git push -u origin main
```

If you would rather build the history step by step as you study each part, commit in this order (each commit should build and pass its tests):

1. `chore: initialize spring boot project and docker compose for mysql and redis`
2. `feat(db): add flyway schema, entities and repositories`
3. `feat: add demo catalog seed data`
4. `feat: add product catalog CRUD with pagination and centralized errors`
5. `feat: implement product search with relevance scoring, filters and sorting`
6. `feat: add trie-based autocomplete`
7. `feat: add interaction tracking and trending products`
8. `feat: add explainable recommendation engine`
9. `feat: add redis caching with after-commit invalidation`
10. `feat: add price range facet with binary search index`
11. `test: add unit, service and API integration tests`
12. `feat(frontend): add react search UI, product page, wishlist and admin`
13. `chore: containerize backend and frontend`
14. `perf: add bulk data generator, benchmark scripts and V2 indexes`
15. `docs: add architecture, API, DSA, database and performance docs`

Don't fake timestamps or history. Commit when you actually do the work.
