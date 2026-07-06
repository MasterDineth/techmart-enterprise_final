-- =============================================================================
--  TechMart Online - MySQL schema (optimized for enterprise-scale reads/writes)
--  Usage:  mysql -u root -p < db/schema.sql
-- =============================================================================

CREATE DATABASE IF NOT EXISTS techmart
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Dedicated application user matching the datasource credentials.
CREATE USER IF NOT EXISTS 'techmart'@'%' IDENTIFIED BY 'techmart';
GRANT ALL PRIVILEGES ON techmart.* TO 'techmart'@'%';
FLUSH PRIVILEGES;

USE techmart;

-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS product (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku          VARCHAR(64)  NOT NULL,
    name         VARCHAR(255) NOT NULL,
    description  VARCHAR(1024),
    category     VARCHAR(128),
    price        DECIMAL(12,2) NOT NULL,
    UNIQUE KEY uk_product_sku (sku),
    KEY idx_product_category (category)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS warehouse (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    code      VARCHAR(32)  NOT NULL,
    name      VARCHAR(255) NOT NULL,
    location  VARCHAR(255),
    UNIQUE KEY uk_warehouse_code (code)
) ENGINE=InnoDB;

-- Inventory per (product, warehouse) - the multi-warehouse sync core.
CREATE TABLE IF NOT EXISTS inventory_item (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id    BIGINT NOT NULL,
    warehouse_id  BIGINT NOT NULL,
    quantity      INT NOT NULL DEFAULT 0,
    reserved      INT NOT NULL DEFAULT 0,
    version       BIGINT NOT NULL DEFAULT 0,   -- optimistic lock: prevents overselling
    UNIQUE KEY uk_inv_prod_wh (product_id, warehouse_id),
    KEY idx_inv_product (product_id),
    CONSTRAINT fk_inv_product   FOREIGN KEY (product_id)   REFERENCES product(id),
    CONSTRAINT fk_inv_warehouse FOREIGN KEY (warehouse_id) REFERENCES warehouse(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS customer (
    id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    name   VARCHAR(255) NOT NULL,
    email  VARCHAR(255) NOT NULL,
    UNIQUE KEY uk_customer_email (email)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS orders (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id  BIGINT NOT NULL,
    status       VARCHAR(32) NOT NULL,
    total        DECIMAL(12,2) NOT NULL DEFAULT 0,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_orders_customer (customer_id),
    KEY idx_orders_status (status),
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES customer(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS order_item (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id    BIGINT NOT NULL,
    product_id  BIGINT NOT NULL,
    quantity    INT NOT NULL,
    unit_price  DECIMAL(12,2) NOT NULL,
    KEY idx_orderitem_order (order_id),
    CONSTRAINT fk_orderitem_order   FOREIGN KEY (order_id)   REFERENCES orders(id),
    CONSTRAINT fk_orderitem_product FOREIGN KEY (product_id) REFERENCES product(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS notification_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    type        VARCHAR(64) NOT NULL,
    channel     VARCHAR(32) NOT NULL,
    recipient   VARCHAR(255),
    message     VARCHAR(2048),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_notif_created (created_at)
) ENGINE=InnoDB;

-- ============ User Authentication Tables ============
CREATE TABLE IF NOT EXISTS app_user (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    username                VARCHAR(64) NOT NULL UNIQUE,
    email                   VARCHAR(255) NOT NULL UNIQUE,
    password_hash           VARCHAR(255) NOT NULL,
    active                  BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login              TIMESTAMP NULL,
    concurrent_session_count INT NOT NULL DEFAULT 0,
    KEY idx_user_active (active),
    KEY idx_user_email (email),
    KEY idx_user_created (created_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_session (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    session_token   VARCHAR(255) NOT NULL UNIQUE,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_accessed   TIMESTAMP NULL,
    user_agent      VARCHAR(512),
    ip_address      VARCHAR(45),
    KEY idx_session_token (session_token),
    KEY idx_session_user (user_id),
    KEY idx_session_active (active),
    KEY idx_session_created (created_at),
    CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES app_user(id)
) ENGINE=InnoDB;

-- ============ Auth Load Test Metrics ============
CREATE TABLE IF NOT EXISTS auth_metrics (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type          VARCHAR(32) NOT NULL,
    response_time_ms    INT NOT NULL,
    success             BOOLEAN NOT NULL DEFAULT TRUE,
    error_message       VARCHAR(255),
    thread_id           VARCHAR(64),
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_metrics_type (event_type),
    KEY idx_metrics_created (created_at),
    KEY idx_metrics_success (success)
) ENGINE=InnoDB;
