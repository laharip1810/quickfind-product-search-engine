-- QuickFind baseline schema (MySQL 8).
-- Also runs on H2 in MySQL mode, which the test suite and the "h2" demo profile use.

CREATE TABLE users (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    email        VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    role         VARCHAR(20)  NOT NULL,
    created_at   DATETIME(6)  NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('CUSTOMER', 'ADMIN'))
);

-- Two-level hierarchy: a top-level category (parent_id NULL) such as "Footwear"
-- and its subcategories such as "Running Shoes". Products always reference a subcategory.
CREATE TABLE categories (
    id        BIGINT       NOT NULL AUTO_INCREMENT,
    name      VARCHAR(100) NOT NULL,
    parent_id BIGINT       NULL,
    CONSTRAINT pk_categories PRIMARY KEY (id),
    CONSTRAINT uk_categories_parent_name UNIQUE (parent_id, name),
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories (id)
);

CREATE TABLE brands (
    id   BIGINT       NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT pk_brands PRIMARY KEY (id),
    CONSTRAINT uk_brands_name UNIQUE (name)
);

CREATE TABLE colors (
    id       BIGINT      NOT NULL AUTO_INCREMENT,
    name     VARCHAR(50) NOT NULL,
    hex_code VARCHAR(7)  NOT NULL,
    CONSTRAINT pk_colors PRIMARY KEY (id),
    CONSTRAINT uk_colors_name UNIQUE (name)
);

CREATE TABLE products (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    name             VARCHAR(200)  NOT NULL,
    description      VARCHAR(2000) NOT NULL,
    brand_id         BIGINT        NOT NULL,
    category_id      BIGINT        NOT NULL,
    price            DECIMAL(10,2) NOT NULL,
    discount_percent DECIMAL(5,2)  NOT NULL DEFAULT 0,
    -- Denormalised price after discount. Filters and sorts use it, so it must be a real
    -- (indexable) column rather than an expression. The entity keeps it in sync.
    sale_price       DECIMAL(10,2) NOT NULL,
    rating           DECIMAL(2,1)  NOT NULL DEFAULT 0,
    review_count     INT           NOT NULL DEFAULT 0,
    stock_quantity   INT           NOT NULL DEFAULT 0,
    popularity_score DOUBLE        NOT NULL DEFAULT 0,
    image_url        VARCHAR(500)  NULL,
    version          BIGINT        NOT NULL DEFAULT 0,
    created_at       DATETIME(6)   NOT NULL,
    updated_at       DATETIME(6)   NOT NULL,
    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT uk_products_brand_name UNIQUE (brand_id, name),
    CONSTRAINT fk_products_brand FOREIGN KEY (brand_id) REFERENCES brands (id),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT chk_products_price CHECK (price > 0),
    CONSTRAINT chk_products_sale_price CHECK (sale_price >= 0),
    CONSTRAINT chk_products_discount CHECK (discount_percent BETWEEN 0 AND 90),
    CONSTRAINT chk_products_rating CHECK (rating BETWEEN 0 AND 5),
    CONSTRAINT chk_products_reviews CHECK (review_count >= 0),
    CONSTRAINT chk_products_stock CHECK (stock_quantity >= 0),
    CONSTRAINT chk_products_popularity CHECK (popularity_score >= 0)
);

CREATE TABLE product_sizes (
    product_id BIGINT      NOT NULL,
    size_label VARCHAR(20) NOT NULL,
    CONSTRAINT pk_product_sizes PRIMARY KEY (product_id, size_label),
    CONSTRAINT fk_product_sizes_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);

CREATE TABLE product_colors (
    product_id BIGINT NOT NULL,
    color_id   BIGINT NOT NULL,
    CONSTRAINT pk_product_colors PRIMARY KEY (product_id, color_id),
    CONSTRAINT fk_product_colors_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT fk_product_colors_color FOREIGN KEY (color_id) REFERENCES colors (id)
);

-- product_id is NULL for SEARCH events (they carry query_text instead).
CREATE TABLE user_interactions (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT       NOT NULL,
    product_id BIGINT       NULL,
    event_type VARCHAR(20)  NOT NULL,
    query_text VARCHAR(200) NULL,
    created_at DATETIME(6)  NOT NULL,
    CONSTRAINT pk_user_interactions PRIMARY KEY (id),
    CONSTRAINT fk_interactions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_interactions_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT chk_interactions_type CHECK (event_type IN ('PRODUCT_VIEW', 'SEARCH', 'ADD_TO_CART', 'WISHLIST'))
);
