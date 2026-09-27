-- Performance indexes, kept separate from V1 so their effect can be measured:
-- start a fresh database with FLYWAY_TARGET=1, benchmark, then restart with the
-- default target to apply this migration and benchmark again (see docs/PERFORMANCE.md).
--
-- InnoDB already created single-column indexes for every foreign key in V1
-- (brand_id, category_id, user_id, product_id), so they are not repeated here.

-- Category page / category filter + price range or price sort.
CREATE INDEX idx_products_category_sale_price ON products (category_id, sale_price);

-- Brand filter + price range or price sort.
CREATE INDEX idx_products_brand_sale_price ON products (brand_id, sale_price);

-- Unfiltered browsing sorted by price, rating or popularity (ORDER BY ... LIMIT n).
CREATE INDEX idx_products_sale_price ON products (sale_price);
CREATE INDEX idx_products_rating ON products (rating);
CREATE INDEX idx_products_popularity ON products (popularity_score);

-- "Recent activity for user X" and the per-user category-affinity lookup.
CREATE INDEX idx_interactions_user_created ON user_interactions (user_id, created_at);

-- Co-interaction lookup ("who else engaged with product X").
CREATE INDEX idx_interactions_product_type ON user_interactions (product_id, event_type);

-- Trending window scan (created_at >= now - 7 days).
CREATE INDEX idx_interactions_created ON user_interactions (created_at);
