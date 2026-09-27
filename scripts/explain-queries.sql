-- Run in MySQL to see how the main search queries use (or do not use) the indexes:
--   docker compose exec mysql sh -c 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' < scripts/explain-queries.sql
-- Compare the output with FLYWAY_TARGET=1 (no V2 indexes) and with the default target.

-- 1. Category + price range, sorted by price (browse path, uses idx_products_category_sale_price)
EXPLAIN ANALYZE
SELECT p.id, p.name, p.sale_price FROM products p
WHERE p.category_id IN (SELECT id FROM categories WHERE name IN ('Running Shoes', 'Sneakers'))
  AND p.sale_price BETWEEN 2000 AND 5000
ORDER BY p.sale_price, p.id
LIMIT 20;

-- 2. Unfiltered browse by popularity (uses idx_products_popularity instead of a filesort)
EXPLAIN ANALYZE
SELECT p.id, p.name FROM products p ORDER BY p.popularity_score DESC, p.id LIMIT 20;

-- 3. Text candidate retrieval: LIKE '%token%' cannot use a B-tree index, so this is a scan.
--    This is the query that grows linearly with catalog size (see docs/PERFORMANCE.md).
EXPLAIN ANALYZE
SELECT p.id FROM products p
JOIN brands b ON b.id = p.brand_id
JOIN categories c ON c.id = p.category_id
WHERE (LOWER(p.name) LIKE '%running%' OR LOWER(p.description) LIKE '%running%'
       OR LOWER(b.name) LIKE '%running%' OR LOWER(c.name) LIKE '%running%')
ORDER BY p.popularity_score DESC
LIMIT 2000;

-- 4. Co-interaction self-join (uses idx_interactions_product_type)
EXPLAIN ANALYZE
SELECT other.product_id, COUNT(*) FROM user_interactions src
JOIN user_interactions other ON other.user_id = src.user_id
WHERE src.product_id = 1 AND other.product_id <> 1
GROUP BY other.product_id;
