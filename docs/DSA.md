# Data structures and algorithms

Each structure below is used because it solves a specific problem in the product. The last section lists where a structure was deliberately **not** used.

Notation: n = number of items processed, k = number of results wanted, L = length of a string, p = length of a prefix.

## Summary

| Structure | Class | Problem solved | Time | Space |
|---|---|---|---|---|
| Trie | `search/ProductSearchTrie` | Autocomplete by prefix | insert O(L); lookup O(p); top-k O(p + m log k) | O(total characters) |
| PriorityQueue (bounded min-heap) | `util/TopK`, inside the Trie | Best k of n without a full sort | O(n log k) | O(k) |
| HashMap | many (below) | O(1) lookups, frequency counting, grouping | O(1) average | O(n) |
| Comparator chains (TimSort) | `search/SortOption` | Multi-key, stable sorting | O(n log n) | O(n) |
| Binary search over a sorted array | `search/PriceRangeIndex` | Price facet counts | O(log n) per query, O(n log n) build | O(n) |

## 1. Trie: autocomplete

**Problem.** The user types "run" and should immediately see *Running Shoes, running shorts, running socks, running jacket*. Scanning every product name for each keystroke costs O(N · L) and grows with the catalog.

**Why a Trie.** Finding the node for a prefix costs O(p), independent of catalog size. Every term that starts with the prefix lives in that node's subtree.

**Implementation (`ProductSearchTrie`, `TrieNode`).**

- Children are a `HashMap<Character, TrieNode>` rather than a 26-slot array, because product text contains digits, spaces and hyphens and most nodes have one or two children. The map is created lazily, so leaf nodes don't pay for one.
- A terminal node stores the display text, a `SuggestionType` (PRODUCT / CATEGORY / BRAND / QUERY / PHRASE), an accumulated weight and, for products, the product id.
- Inserting the same text again (case-insensitive) **adds** to its weight. "running shoes" is inserted as a category, as a phrase from 16 product names and as a popular query, and ranks accordingly. The higher-priority type provides the label.
- `getSuggestions(prefix, k)` walks to the prefix node (O(p)), then does an iterative DFS of the subtree. Each terminal goes into a **min-heap of size k**; when the heap exceeds k, the weakest is removed. Total O(p + m log k) for m nodes in the subtree, instead of O(m log m) to sort them all.
- A missing prefix returns an empty list; it is never null and never throws.

**What goes in** (built by `AutocompleteService`):

| Term | Weight |
|---|---|
| Product name (top 10,000 by popularity) | 1 + popularity |
| Last 1–3 words of each name, brand and size tokens removed ("backpack", "running shoes", "everyday running socks") | 0.5 · (1 + popularity), summed |
| Brand name, subcategory name | Σ (1 + popularity) of their products |
| Popular search query (from SEARCH interactions) | 20 per search |

The trailing phrases are what make "run" work, since product names start with the brand ("Nike Pegasus 41 Running Shoes"). Trailing size tokens ("32L", "750ml") are dropped, and phrases containing digits ("41 running shoes") are skipped.

**Complexity.** Insert O(L). Prefix check O(p). Suggestions O(p + m log k). Space O(total characters), less where terms share prefixes. Capping product-name terms at 10,000 bounds memory at 100k+ products. Low-popularity names would never reach the top k anyway.

**Concurrency.** The Trie is not thread-safe for writes, so it is never written after construction. A rebuild creates a new Trie and swaps a `volatile` reference. Catalog changes set a dirty flag, and a scheduled check rebuilds at most every 2 s, so a burst of edits costs one rebuild.

## 2. PriorityQueue: top-k selection

**Problem.** Recommendations score up to 500 candidates and need the best 8. Trending counts engagement for every recently touched product and needs the top N. Autocomplete needs the best k terms of a subtree.

**Why a heap.** Sorting all n items is O(n log n). A min-heap that never holds more than k items is O(n log k) time and O(k) space. For "8 of 500", log 8 ≈ 3 versus log 500 ≈ 9 comparisons per element. The heap is ordered **worst-first**, so its head is always the element to evict.

**Where.** `util/TopK.select(items, k, bestFirst)` is used by `RecommendationService` (best 3·limit candidates, then the diversity pass), `TrendingService` (top N products by weighted engagement) and the user's top-3 subcategories. `ProductSearchTrie` has the same pattern inline.

## 3. HashMap

| Where | Key → value | Why |
|---|---|---|
| `CatalogLookupService` | lowercase brand/category/color name → id (and id → info) | Filters arrive as names (`?brand=Nike`); O(1) resolution instead of a query per filter. Immutable snapshot swapped atomically. |
| `ProductSearchService` | product id → entity | After scoring and sorting framework-free `SearchDocument`s, map results back to entities in O(1). |
| `RecommendationService` | candidate id → co-interaction strength | O(1) lookup per candidate while scoring. |
| `RecommendationService` | subcategory id → interaction count | Frequency count of the user's recent interests. |
| `TrendingService` | product id → weighted engagement | One pass over recent events: `merge(id, weight, Double::sum)`. |
| `PriceIndexService` | category id → price list → `PriceRangeIndex` | Group prices per category in one pass. |
| `DiversityFilter` | subcategory id → items taken | Enforce "at most 3 per subcategory". |
| `TrieNode.children` | character → child node | See the Trie section. |

All are O(1) average per operation and O(n) space.

## 4. Sorting with Comparator chains

**Problem.** Five sort orders, each needing deterministic tie-breaking. Without a final unique key, two products with equal price could swap places between requests, and pagination would show one of them twice.

**Implementation.** `SortOption` holds one comparator per option, composed with `Comparator.comparing(...).reversed().thenComparing(...)`. Every chain ends with the product id.

| Option | Order |
|---|---|
| `relevance` | score ↓, popularity ↓, id ↑ |
| `price_asc` / `price_desc` | sale price ↑/↓, id ↑ |
| `rating` | rating ↓, review count ↓, id ↑ |
| `popularity` | popularity ↓, id ↑ |

`List.sort` is Java's stable TimSort: O(n log n), and near O(n) on already-sorted input. Sorting is only done in Java when a text query was scored. Without a query, the same orders are sent to MySQL as `ORDER BY` (`ProductSorts`), where the V2 indexes can serve them.

## 5. Relevance scoring (search)

This is not a single data structure, but it is the core algorithm, so its formula is documented here. For query tokens t₁…t_q (after normalisation):

```
tokenScore(t) = 5·[t in name] + 4·[t in subcategory or parent category] + 3·[t in brand]
              + 2·[t in a color name] + 1·[t in description]
coverage      = matchedTokens / q
score         = (Σ tokenScore) · coverage
              + 3   if the whole multi-word query appears in the name, in order
score         = score · 0.5   if out of stock (demoted, not hidden)
```

- "t in field" means some word of the normalised field **starts with** t, so a partially typed "runn" matches "running", but "unning" does not.
- Normalisation (`QueryNormalizer`): lowercase, strip apostrophes and punctuation, collapse whitespace, drop stopwords and one-letter tokens, and fold plurals (`shoes → shoe`, `watches → watch`, `accessories → accessory`). The same pipeline is applied to queries and fields.
- The `coverage` factor makes a product matching all of "black running shoes" beat one that matches "shoe" in five fields.
- Candidates must match **all** tokens. If none do, the search falls back to **any** token, and the response says `matchMode: ANY_TERM`.
- Worked example: Nike Pegasus 41 Running Shoes (Black, White) for "black running shoes". running = 5 + 4, shoe = 5 + 4, black = 2 → 20 × 1.0 = **20**. For "running shoes" it also gets the phrase bonus: 18 + 3 = **21**. Both cases are unit tests.

## 6. Binary search: price facet counts

**Problem.** While a shopper types a price range, the sidebar shows "N in-stock products in this price range" and updates on every (debounced) keystroke.

**Why binary search, and why only here.** The **price filter itself** is answered by MySQL with the `(category_id, sale_price)` B-tree index, which is the right tool. A second implementation in Java would add nothing. The facet count is different: it is recomputed repeatedly for the same category, only needs a count, and can tolerate being rebuilt lazily after catalog changes. A sorted `double[]` per category answers it with two binary searches:

```
count(min, max) = upperBound(max) − lowerBound(min)
lowerBound(x)   = first index with a[i] ≥ x
upperBound(x)   = first index with a[i] > x
```

**Complexity.** O(log n) per query. O(n log n) to build (sort). O(n) space. `PriceRangeIndex` implements the two searches by hand and is covered by boundary tests (duplicates, empty ranges, inverted ranges, empty index).

## 7. Recommendation scoring

For source product S and candidate C (full details in `RecommendationScorer`):

```
score = 0.30 · categorySim    1.0 same subcategory · 0.7 complementary subcategory · 0.5 same parent
      + 0.15 · brandSim       1 if same brand
      + 0.15 · priceSim       max(0, 1 − |pS − pC| / max(pS, pC))
      + 0.15 · coInteraction  strength / strongest   (users who engaged with S also engaged with C)
      + 0.10 · popularity     log(1 + pop) / log(1 + maxPop)
      + 0.10 · rating         rating / 5
      + 0.05 · colorSim       Jaccard(colors S, colors C) = |S ∩ C| / |S ∪ C|
      + 0.10 bonus            if C's subcategory is in the user's top-3 recent subcategories
```

- **Candidates** (≤ 500, in stock, not S): same parent category, a complementary subcategory, the same brand, or co-interacted with S.
- **Selection:** bounded heap for the best 3·limit, then a greedy diversity pass that takes at most 3 per subcategory. Without that pass, a running shoe's recommendations would be eight more running shoes.
- **Why both content and behaviour:** content similarity finds *similar* items. Complementary categories (curated in `CategoryRelations`) and co-interaction (learned from `user_interactions` with a self-join) find items that *go together*, such as socks and shorts for running shoes.
- **Popularity uses a log** so a few best-sellers don't drown out everything else.
- Each component is returned in `breakdown`, and turned into readable `reasons`.

## Where a structure was deliberately not used

- **No hand-written sort algorithm.** `Comparator` + TimSort is correct, stable and fast; re-implementing merge sort would only add risk.
- **No binary search for the price filter.** The database index already does it (see §6).
- **No in-memory inverted index for full-text search.** It would duplicate the database in every instance and need its own invalidation. The documented next step is Elasticsearch/OpenSearch (BM25, fuzzy matching, sharding).
- **The Trie is not used for search results,** only for suggestions. Search needs matches anywhere in the text and relevance ranking, not just prefixes.
