-- ============================================================
-- pposiraegi ecommerce schema DDL
-- PostgreSQL 15+
-- ============================================================

-- ============================================================
-- 1. categories
-- ============================================================
CREATE TABLE IF NOT EXISTS categories (
    id             BIGINT       NOT NULL,
    parent_id      BIGINT,
    name           VARCHAR(50)  NOT NULL,
    depth          INT          NOT NULL,
    display_order  INT          NOT NULL DEFAULT 0,
    created_at     TIMESTAMP    NOT NULL,
    updated_at     TIMESTAMP,
    PRIMARY KEY (id)
);

-- ============================================================
-- 2. products
-- ============================================================
CREATE TABLE IF NOT EXISTS products (
    id              BIGINT          NOT NULL,
    category_id     BIGINT          NOT NULL,
    name            VARCHAR(100)    NOT NULL,
    description     TEXT,
    brand_name      VARCHAR(50),
    origin_price    NUMERIC(12, 2)  NOT NULL,
    sale_price      NUMERIC(12, 2)  NOT NULL,
    thumbnail_url   TEXT,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PREPARING',
    average_rating  NUMERIC(2, 1)   NOT NULL DEFAULT 0.0,
    review_count    INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

-- ============================================================
-- 3. product_images
-- ============================================================
CREATE TABLE IF NOT EXISTS product_images (
    id             BIGINT      NOT NULL,
    product_id     BIGINT      NOT NULL,
    image_url      TEXT        NOT NULL,
    image_type     VARCHAR(20),
    display_order  INT         DEFAULT 0,
    created_at     TIMESTAMP   NOT NULL,
    updated_at     TIMESTAMP,
    PRIMARY KEY (id)
);

-- ============================================================
-- 4. product_options
-- ============================================================
CREATE TABLE IF NOT EXISTS product_options (
    id             BIGINT      NOT NULL,
    product_id     BIGINT      NOT NULL,
    name           VARCHAR(50) NOT NULL,
    display_order  INT         DEFAULT 1,
    created_at     TIMESTAMP   NOT NULL,
    updated_at     TIMESTAMP,
    PRIMARY KEY (id)
);

-- ============================================================
-- 5. product_option_values
-- ============================================================
CREATE TABLE IF NOT EXISTS product_option_values (
    id             BIGINT      NOT NULL,
    option_id      BIGINT      NOT NULL,
    value          VARCHAR(50) NOT NULL,
    display_order  INT         DEFAULT 1,
    created_at     TIMESTAMP   NOT NULL,
    updated_at     TIMESTAMP,
    PRIMARY KEY (id)
);

-- ============================================================
-- 6. product_skus
-- ============================================================
CREATE TABLE IF NOT EXISTS product_skus (
    id                BIGINT          NOT NULL,
    product_id        BIGINT          NOT NULL,
    sku_code          VARCHAR(100),
    combination_key   VARCHAR(255),
    status            VARCHAR(20)     NOT NULL,
    additional_price  NUMERIC(12, 2)  DEFAULT 0,
    stock_quantity    INT             NOT NULL DEFAULT 0,
    deleted_at        TIMESTAMP,
    created_at        TIMESTAMP       NOT NULL,
    updated_at        TIMESTAMP,
    PRIMARY KEY (id)
);

-- ============================================================
-- 7. sku_option_mappings
-- ============================================================
CREATE TABLE IF NOT EXISTS sku_option_mappings (
    id               BIGINT    NOT NULL,
    sku_id           BIGINT    NOT NULL,
    option_value_id  BIGINT    NOT NULL,
    created_at       TIMESTAMP NOT NULL,
    PRIMARY KEY (id)
);

-- ============================================================
-- 8. time_deals
-- ============================================================
CREATE TABLE IF NOT EXISTS time_deals (
    id               BIGINT      NOT NULL,
    product_id       BIGINT      NOT NULL,
    total_quantity   INT         NOT NULL,
    remain_quantity  INT         NOT NULL,
    start_time       TIMESTAMP   NOT NULL,
    end_time         TIMESTAMP   NOT NULL,
    status           VARCHAR(20) NOT NULL,
    created_at       TIMESTAMP   NOT NULL,
    updated_at       TIMESTAMP,
    PRIMARY KEY (id)
);

-- ============================================================
-- 9. users
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id                 BIGINT       NOT NULL,
    email              VARCHAR(255) NOT NULL UNIQUE,
    password_hash      VARCHAR(255) NOT NULL,
    name               VARCHAR(255) NOT NULL,
    nickname           VARCHAR(255) NOT NULL,
    profile_image_url  TEXT,
    phone_number       VARCHAR(20),
    status             VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    deleted_at         TIMESTAMP,
    created_at         TIMESTAMP    NOT NULL,
    updated_at         TIMESTAMP,
    PRIMARY KEY (id)
);

-- ============================================================
-- 10. user_addresses
-- ============================================================
CREATE TABLE IF NOT EXISTS user_addresses (
    id                      BIGINT       NOT NULL,
    user_id                 BIGINT       NOT NULL,
    recipient_name          VARCHAR(255) NOT NULL,
    phone_number            VARCHAR(20)  NOT NULL,
    secondary_phone_number  VARCHAR(20),
    zip_code                VARCHAR(20)  NOT NULL,
    base_address            VARCHAR(255) NOT NULL,
    detail_address          VARCHAR(255),
    request_message         VARCHAR(255),
    is_default              BOOLEAN      NOT NULL,
    last_used_at            TIMESTAMP,
    created_at              TIMESTAMP    NOT NULL,
    updated_at              TIMESTAMP,
    PRIMARY KEY (id)
);

-- ============================================================
-- 11. user_refresh_tokens
-- ============================================================
CREATE TABLE IF NOT EXISTS user_refresh_tokens (
    id           BIGINT       NOT NULL,
    user_id      BIGINT       NOT NULL UNIQUE,
    token_value  VARCHAR(512) NOT NULL UNIQUE,
    ip_address   VARCHAR(50),
    device_info  VARCHAR(255),
    expires_at   TIMESTAMP    NOT NULL,
    created_at   TIMESTAMP    NOT NULL,
    PRIMARY KEY (id)
);

-- ============================================================
-- 12. orders
-- ============================================================
CREATE TABLE IF NOT EXISTS orders (
    id            BIGINT          NOT NULL,
    user_id       BIGINT          NOT NULL,
    order_number  BIGINT          NOT NULL UNIQUE,
    checkout_id   BIGINT          NOT NULL UNIQUE,
    total_amount  NUMERIC(12, 2)  NOT NULL,
    pg_imp_uid    VARCHAR(100)    NOT NULL UNIQUE,
    status        VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    created_at    TIMESTAMP       NOT NULL,
    updated_at    TIMESTAMP,
    CONSTRAINT uq_orders_checkout_id  UNIQUE (checkout_id),
    CONSTRAINT uq_orders_pg_imp_uid   UNIQUE (pg_imp_uid),
    PRIMARY KEY (id)
);

-- ============================================================
-- 13. order_items
-- ============================================================
CREATE TABLE IF NOT EXISTS order_items (
    id               BIGINT          NOT NULL,
    order_id         BIGINT          NOT NULL,
    product_id       BIGINT          NOT NULL,
    sku_id           BIGINT          NOT NULL,
    shipment_id      BIGINT,
    product_name     VARCHAR(255)    NOT NULL,
    sku_name         VARCHAR(100)    NOT NULL,
    quantity         INT             NOT NULL,
    unit_price       NUMERIC(12, 2)  NOT NULL,
    discount_amount  NUMERIC(12, 2)  NOT NULL,
    status           VARCHAR(30),
    created_at       TIMESTAMP       NOT NULL,
    PRIMARY KEY (id)
);

-- ============================================================
-- 14. shipments
-- ============================================================
CREATE TABLE IF NOT EXISTS shipments (
    id               BIGINT       NOT NULL,
    order_id         BIGINT       NOT NULL UNIQUE,
    receiver_name    VARCHAR(255) NOT NULL,
    receiver_phone   VARCHAR(20)  NOT NULL,
    zip_code         VARCHAR(20)  NOT NULL,
    base_address     VARCHAR(255) NOT NULL,
    detail_address   VARCHAR(255),
    request_message  VARCHAR(255),
    carrier_name     VARCHAR(100),
    tracking_number  VARCHAR(100),
    status           VARCHAR(30),
    created_at       TIMESTAMP    NOT NULL,
    updated_at       TIMESTAMP,
    PRIMARY KEY (id)
);
