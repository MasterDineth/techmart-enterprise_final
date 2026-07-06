

CREATE DATABASE IF NOT EXISTS techmart
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;


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

CREATE TABLE IF NOT EXISTS inventory_item (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id    BIGINT NOT NULL,
    warehouse_id  BIGINT NOT NULL,
    quantity      INT NOT NULL DEFAULT 0,
    reserved      INT NOT NULL DEFAULT 0,
    version       BIGINT NOT NULL DEFAULT 0,
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
