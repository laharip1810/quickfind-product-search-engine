# API reference

Base URL: `http://localhost:8080` (or `http://localhost:3000`, which goes through the frontend's nginx proxy). All bodies are JSON. Prices are INR.

Example responses are shortened, and their ids and scores are illustrative. A runnable version of every request below is in [`postman/QuickFind.postman_collection.json`](../postman/QuickFind.postman_collection.json).

## Summary

| Method | Path | Purpose | Success |
|---|---|---|---|
| GET | `/api/products` | Paginated catalog | 200 |
| GET | `/api/products/{id}` | Product detail (Redis-cached) | 200 |
| POST | `/api/products` | Create a product (admin) | 201 + `Location` |
| PUT | `/api/products/{id}` | Replace a product (admin, optimistic locking) | 200 |
| PATCH | `/api/products/{id}/stock` | Change stock (admin) | 200 |
| DELETE | `/api/products/{id}` | Delete a product (admin) | 204 |
| GET | `/api/products/search` | Search + filters + sort + pagination | 200 |
| GET | `/api/search/suggestions` | Trie autocomplete | 200 |
| GET | `/api/products/{id}/recommendations` | Explainable recommendations | 200 |
| GET | `/api/products/trending` | Top-N by recent engagement | 200 |
| GET | `/api/products/price-range-count` | Price facet count (binary search) | 200 |
| POST | `/api/interactions` | Record PRODUCT_VIEW / SEARCH / ADD_TO_CART / WISHLIST | 201 (200 if already wishlisted) |
| GET | `/api/interactions/wishlist` | Wishlisted products | 200 |
| DELETE | `/api/interactions/wishlist/{productId}` | Remove from wishlist | 204 |
| GET | `/api/interactions/recent` | Recent activity | 200 |
| GET | `/api/categories` | Category tree | 200 |
| GET | `/api/brands` | Brands | 200 |
| GET | `/api/colors` | Colors with hex codes | 200 |
| GET | `/api/sizes` | Size labels in use | 200 |
| GET | `/api/users/demo` | The demo user used when no `userId` is sent | 200 |
| GET | `/actuator/health` | Health (DB + Redis) | 200 |

## Products

### `GET /api/products?page=0&size=20&sort=popularity`

| Param | Default | Rules |
|---|---|---|
| `page` | 0 | ≥ 0 |
| `size` | 20 | 1–100 |
| `sort` | `relevance` (= popularity without a query) | `relevance`, `price_asc`, `price_desc`, `rating`, `popularity` |

```json
{
  "content": [ { "id": 1, "name": "Nike Pegasus 41 Running Shoes", "brand": "Nike", "category": "Footwear",
                 "subcategory": "Running Shoes", "price": 11895.00, "discountPercent": 10.00, "salePrice": 10705.50,
                 "rating": 4.6, "reviewCount": 2140, "stockQuantity": 45, "inStock": true, "popularityScore": 76.72,
                 "colors": [ { "id": 1, "name": "Black", "hexCode": "#111827" } ], "imageUrl": null } ],
  "page": 0, "size": 20, "totalElements": 114, "totalPages": 6, "sort": "popularity"
}
```

### `GET /api/products/{id}`

Returns the summary fields plus `description`, `brandId`, `categoryId`, `subcategoryId`, `sizes`, `version`, `createdAt` and `updatedAt`. It is served from Redis when cached. Returns 404 `PRODUCT_NOT_FOUND` if the id doesn't exist.

### `POST /api/products` · `PUT /api/products/{id}`

```json
{
  "name": "Puma Velocity Nitro 4 Running Shoes",
  "description": "Nitro foam trainer for tempo runs",
  "brandId": 3,
  "categoryId": 4,
  "price": 12999.00,
  "discountPercent": 20,
  "rating": 4.4,
  "reviewCount": 0,
  "stockQuantity": 25,
  "popularityScore": 0,
  "colorIds": [1, 5],
  "sizes": ["UK 7", "UK 8", "UK 9"],
  "imageUrl": null,
  "version": 0
}
```

| Field | Rules |
|---|---|
| `name` | required, 3–200 chars, unique per brand (409 `DUPLICATE_PRODUCT`) |
| `description` | required, ≤ 2000 chars |
| `brandId` | required, must exist (400 `INVALID_PRODUCT`) |
| `categoryId` | required, must be a **subcategory** such as Running Shoes (400 `INVALID_PRODUCT`) |
| `price` | required, 1.00–10,000,000.00, 2 decimals |
| `discountPercent` | 0–90 (default 0); `salePrice` is computed |
| `rating` | 0–5, 1 decimal |
| `reviewCount`, `popularityScore` | ≥ 0 |
| `stockQuantity` | required, 0–1,000,000 |
| `colorIds` | at least one, all must exist |
| `sizes` | optional labels, ≤ 20 chars each |
| `imageUrl` | optional `http(s)` URL |
| `version` | optional on PUT. If sent and different from the current version: 409 `VERSION_CONFLICT` |

### `PATCH /api/products/{id}/stock`

```json
{ "stockQuantity": 0 }
```

Every write evicts `product:{id}` from Redis after commit, bumps the catalog cache version, and schedules the Trie and price-index rebuilds.

## Search

### `GET /api/products/search`

| Param | Example | Notes |
|---|---|---|
| `query` | `black running shoes` | ≤ 200 chars; normalised into tokens |
| `category` | `Footwear` or `Running Shoes` | Top-level category or subcategory name (case-insensitive) |
| `brand` | `brand=Nike&brand=Puma` | Repeatable |
| `color` | `Black` | |
| `productSize` | `UK 9`, `M`, `32` | Named `productSize` because `size` is the page size |
| `minPrice`, `maxPrice` | `2000`, `5000` | Sale price after discount, inclusive |
| `minRating` | `4` | 0–5 |
| `inStock` | `true` | Default: out-of-stock products are included but ranked lower |
| `sort` | `relevance` | as above |
| `page`, `size` | `0`, `20` | as above |

```json
{
  "content": [ { "product": { "id": 1, "name": "Nike Pegasus 41 Running Shoes", "...": "..." }, "relevanceScore": 20.0 } ],
  "page": 0, "size": 20, "totalElements": 11, "totalPages": 1,
  "sort": "relevance",
  "query": "black running shoes",
  "normalizedTokens": ["black", "running", "shoe"],
  "matchMode": "ALL_TERMS",
  "candidatesTruncated": false,
  "tookMs": 14
}
```

- `matchMode`: `NONE` (no query), `ALL_TERMS`, or `ANY_TERM` (fallback when no product matches every word).
- `relevanceScore` is `null` without a query.
- `candidatesTruncated` is `true` when more than 2,000 products matched and only the most popular 2,000 were ranked.

### `GET /api/products/price-range-count?category=Footwear&minPrice=2000&maxPrice=5000`

```json
{ "category": "Footwear", "minPrice": 2000, "maxPrice": 5000, "count": 9 }
```

This counts in-stock products in the range using the in-memory sorted price array (two binary searches). The UI uses it for the live hint under the price inputs.

## Autocomplete

### `GET /api/search/suggestions?prefix=run&limit=8`

```json
{
  "prefix": "run",
  "suggestions": [
    { "text": "Running Shoes", "type": "CATEGORY", "score": 1596.31, "productId": null },
    { "text": "running shorts", "type": "PHRASE", "score": 172.9, "productId": null },
    { "text": "running socks", "type": "QUERY", "score": 155.2, "productId": null }
  ]
}
```

- `type` is one of `PRODUCT` (with `productId`), `CATEGORY`, `BRAND`, `QUERY` (popular search) or `PHRASE`.
- `limit` defaults to 8, maximum 20. A missing or blank prefix returns 400; an unknown prefix returns an empty list.

## Recommendations and trending

### `GET /api/products/{id}/recommendations?userId=1&limit=8`

```json
{
  "sourceProductId": 1, "userId": 1,
  "recommendations": [
    {
      "product": { "id": 97, "name": "Nike Dri-FIT Everyday Running Socks", "...": "..." },
      "score": 0.708,
      "breakdown": { "category": 0.21, "brand": 0.15, "price": 0.016, "coInteraction": 0.109,
                     "popularity": 0.096, "rating": 0.09, "color": 0.033, "userAffinity": 0.1 },
      "reasons": ["Pairs well with Running Shoes", "Same brand (Nike)",
                  "Shoppers who viewed this also engaged with it", "Highly rated (4.5★)"]
    }
  ]
}
```

- `userId` is optional (it adds the personal-interest bonus). `limit` is 1–20, default 8.
- Only in-stock products are returned, never the source product, and at most 3 per subcategory.

### `GET /api/products/trending?limit=8`

Returns a list of product summaries (in stock), with `limit` 1–50.

## Interactions

### `POST /api/interactions`

```json
{ "eventType": "PRODUCT_VIEW", "productId": 1 }
{ "eventType": "SEARCH", "query": "running shoes" }
{ "eventType": "WISHLIST", "productId": 1, "userId": 3 }
```

- `userId` is optional (defaults to the demo user).
- `PRODUCT_VIEW`, `ADD_TO_CART` and `WISHLIST` require `productId`; `SEARCH` requires `query`.
- Adding the same product to the wishlist twice returns the existing entry with 200.

`GET /api/interactions/wishlist`, `DELETE /api/interactions/wishlist/{productId}` and `GET /api/interactions/recent?limit=20` all accept an optional `userId`.

## Errors

Every error has the same shape and never contains a stack trace:

```json
{
  "timestamp": "2026-09-27T06:00:00Z",
  "status": 400,
  "error": "VALIDATION_FAILED",
  "message": "Request validation failed",
  "path": "/api/products",
  "fieldErrors": [ { "field": "price", "message": "price must be at least 1.00" } ]
}
```

| `error` | HTTP | When |
|---|---|---|
| `VALIDATION_FAILED` | 400 | Bean Validation failure, or a query parameter of the wrong type (`minPrice=abc`) |
| `INVALID_REQUEST` | 400 | Malformed JSON, unknown enum value, bad path variable, missing field for an event type |
| `INVALID_SEARCH_REQUEST` | 400 | Bad paging, query too long, missing/blank prefix, limit out of range |
| `INVALID_SORT` | 400 | Unsupported `sort` (the message lists the allowed values) |
| `INVALID_FILTER` | 400 | Unknown category/brand/color, negative price, `minPrice > maxPrice`, rating outside 0–5 |
| `INVALID_PRODUCT` | 400 | Unknown brand/colors, or a top-level category used as `categoryId` |
| `PRODUCT_NOT_FOUND` | 404 | Unknown product id |
| `RESOURCE_NOT_FOUND` | 404 | Unknown user, or an unknown endpoint |
| `METHOD_NOT_ALLOWED` | 405 | Wrong HTTP method |
| `DUPLICATE_PRODUCT` | 409 | Same brand + name already exists |
| `VERSION_CONFLICT` | 409 | Stale `version` on update, or a concurrent modification |
| `DATA_CONFLICT` | 409 | Database constraint violation |
| `DATABASE_ERROR` | 503 | Database unavailable |
| `INTERNAL_ERROR` | 500 | Anything unexpected (details only in the server log) |

Every response carries an `X-Request-Id` header. The same id is on every log line for that request.
