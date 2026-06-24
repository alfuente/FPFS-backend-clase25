CREATE DATABASE IF NOT EXISTS shopeasy;
USE shopeasy;

CREATE TABLE IF NOT EXISTS roles (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS users (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100)  NOT NULL,
    email      VARCHAR(150)  NOT NULL UNIQUE,
    password   VARCHAR(255)  NOT NULL,
    enabled    BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE TABLE IF NOT EXISTS categories (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE IF NOT EXISTS products (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(200)  NOT NULL,
    description TEXT,
    price       DECIMAL(10,2) NOT NULL,
    stock       INT           NOT NULL DEFAULT 0,
    image_url   VARCHAR(500),
    category_id BIGINT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES categories(id)
);

CREATE TABLE IF NOT EXISTS cart_items (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity   INT    NOT NULL DEFAULT 1,
    UNIQUE KEY uq_cart_item (user_id, product_id),
    CONSTRAINT fk_cart_user    FOREIGN KEY (user_id)    REFERENCES users(id),
    CONSTRAINT fk_cart_product FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE IF NOT EXISTS orders (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT        NOT NULL,
    status     ENUM('PENDIENTE','ENVIADO','ENTREGADO','CANCELADO') NOT NULL DEFAULT 'PENDIENTE',
    total      DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS order_items (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id   BIGINT        NOT NULL,
    product_id BIGINT        NOT NULL,
    quantity   INT           NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_oi_order   FOREIGN KEY (order_id)   REFERENCES orders(id),
    CONSTRAINT fk_oi_product FOREIGN KEY (product_id) REFERENCES products(id)
);

-- Datos de ejemplo
INSERT IGNORE INTO roles (name) VALUES ('ROLE_ADMIN'), ('ROLE_USER');

INSERT IGNORE INTO categories (name, description) VALUES
    ('Electrónica',   'Smartphones, laptops, accesorios tecnológicos'),
    ('Ropa',          'Camisetas, pantalones, zapatillas'),
    ('Hogar',         'Muebles, decoración, electrodomésticos'),
    ('Libros',        'Ficción, educación, técnicos');

INSERT IGNORE INTO products (name, description, price, stock, category_id) VALUES
    ('Smartphone XL',   'Pantalla 6.5", 128 GB, cámara 50MP',   599.99,  30, 1),
    ('Laptop Pro 15',   'Intel i7, 16 GB RAM, SSD 512 GB',      1199.99, 15, 1),
    ('Auriculares BT',  'Bluetooth 5.0, cancelación de ruido',     89.99, 50, 1),
    ('Camiseta Básica', 'Algodón 100%, tallas S-XXL',              19.99, 100, 2),
    ('Zapatillas Run',  'Suela amortiguada, transpirables',         79.99,  40, 2),
    ('Lámpara LED',     'Luz cálida/fría regulable, USB-C',         34.99,  60, 3),
    ('Clean Code',      'Robert C. Martin — Programación limpia',   39.99,  25, 4),
    ('El Principito',   'Antoine de Saint-Exupéry',                 12.99,  80, 4);
