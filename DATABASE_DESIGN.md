# ShopEasy — Diseño de Base de Datos

## Resumen

Base de datos relacional MySQL para un ecommerce MVP. Cubre autenticación con roles, catálogo de productos, carrito de compras y gestión de órdenes.

---

## Diagrama ER

```
┌─────────────┐        ┌───────────────┐        ┌──────────────┐
│    roles    │        │     users     │        │  categories  │
│─────────────│        │───────────────│        │──────────────│
│ id (PK)     │        │ id (PK)       │        │ id (PK)      │
│ name        │        │ name          │        │ name         │
└──────┬──────┘        │ email         │        │ description  │
       │               │ password      │        └──────┬───────┘
       │ N:M           │ enabled       │               │ 1:N
       │               └───┬─────┬────┘               │
┌──────┴──────┐            │     │               ┌─────┴──────────┐
│  user_roles │            │     │               │    products    │
│─────────────│            │     │               │────────────────│
│ user_id(FK) │            │     │               │ id (PK)        │
│ role_id(FK) │            │     │               │ name           │
└─────────────┘            │     │               │ description    │
                           │     │               │ price          │
                    1:N    │     │ 1:N           │ stock          │
                           │     │               │ image_url      │
                    ┌──────┴─┐ ┌─┴──────────┐   │ category_id(FK)│
                    │ orders │ │ cart_items │   └──────┬─────────┘
                    │────────│ │────────────│          │
                    │ id(PK) │ │ id (PK)    │          │ N:1
                    │user_id │ │ user_id(FK)│◄─────────┘
                    │status  │ │ product_id │
                    │total   │ │ quantity   │
                    │created │ └────────────┘
                    └───┬────┘
                        │ 1:N
                   ┌────┴──────────┐
                   │  order_items  │
                   │───────────────│
                   │ id (PK)       │
                   │ order_id (FK) │
                   │ product_id(FK)│
                   │ quantity      │
                   │ unit_price    │
                   └───────────────┘
```

---

## Descripción de entidades

### `roles`
Almacena los roles disponibles en el sistema. Sólo existen dos: `ROLE_ADMIN` y `ROLE_USER`.

| Columna | Tipo         | Restricciones      |
|---------|--------------|--------------------|
| id      | BIGINT       | PK, AUTO_INCREMENT |
| name    | VARCHAR(50)  | NOT NULL, UNIQUE   |

---

### `users`
Representa a todos los usuarios del sistema (clientes y administradores). El rol determina los permisos.

| Columna   | Tipo          | Restricciones              |
|-----------|---------------|----------------------------|
| id        | BIGINT        | PK, AUTO_INCREMENT         |
| name      | VARCHAR(100)  | NOT NULL                   |
| email     | VARCHAR(150)  | NOT NULL, UNIQUE           |
| password  | VARCHAR(255)  | NOT NULL (bcrypt)          |
| enabled   | BOOLEAN       | NOT NULL, DEFAULT TRUE     |
| created_at| TIMESTAMP     | DEFAULT CURRENT_TIMESTAMP  |

---

### `user_roles` (tabla pivote)
Relación N:M entre usuarios y roles.

| Columna  | Tipo   | Restricciones         |
|----------|--------|-----------------------|
| user_id  | BIGINT | FK → users(id)        |
| role_id  | BIGINT | FK → roles(id)        |

> Clave primaria compuesta: `(user_id, role_id)`

---

### `categories`
Categorías que agrupan los productos del catálogo.

| Columna     | Tipo         | Restricciones      |
|-------------|--------------|--------------------|
| id          | BIGINT       | PK, AUTO_INCREMENT |
| name        | VARCHAR(100) | NOT NULL, UNIQUE   |
| description | TEXT         | NULL               |

---

### `products`
Artículos disponibles para la venta.

| Columna     | Tipo           | Restricciones          |
|-------------|----------------|------------------------|
| id          | BIGINT         | PK, AUTO_INCREMENT     |
| name        | VARCHAR(200)   | NOT NULL               |
| description | TEXT           | NULL                   |
| price       | DECIMAL(10,2)  | NOT NULL               |
| stock       | INT            | NOT NULL, DEFAULT 0    |
| image_url   | VARCHAR(500)   | NULL                   |
| category_id | BIGINT         | FK → categories(id)    |
| created_at  | TIMESTAMP      | DEFAULT CURRENT_TIMESTAMP |

---

### `cart_items`
Carrito de compras temporal. Cada fila es un producto en el carrito de un usuario.

| Columna    | Tipo   | Restricciones          |
|------------|--------|------------------------|
| id         | BIGINT | PK, AUTO_INCREMENT     |
| user_id    | BIGINT | FK → users(id)         |
| product_id | BIGINT | FK → products(id)      |
| quantity   | INT    | NOT NULL, DEFAULT 1    |

> Índice UNIQUE en `(user_id, product_id)` para evitar duplicados.

---

### `orders`
Cabecera de una orden de compra. Se crea cuando el usuario confirma el carrito.

| Columna    | Tipo           | Restricciones                                         |
|------------|----------------|-------------------------------------------------------|
| id         | BIGINT         | PK, AUTO_INCREMENT                                    |
| user_id    | BIGINT         | FK → users(id)                                        |
| status     | ENUM           | ('PENDIENTE','ENVIADO','ENTREGADO','CANCELADO')        |
| total      | DECIMAL(10,2)  | NOT NULL                                              |
| created_at | TIMESTAMP      | DEFAULT CURRENT_TIMESTAMP                             |

---

### `order_items`
Detalle de cada producto dentro de una orden. El `unit_price` se almacena para conservar el precio histórico aunque el producto cambie de precio después.

| Columna    | Tipo           | Restricciones      |
|------------|----------------|--------------------|
| id         | BIGINT         | PK, AUTO_INCREMENT |
| order_id   | BIGINT         | FK → orders(id)    |
| product_id | BIGINT         | FK → products(id)  |
| quantity   | INT            | NOT NULL           |
| unit_price | DECIMAL(10,2)  | NOT NULL           |

---

## Relaciones resumidas

| Relación                         | Tipo | Notas                                         |
|----------------------------------|------|-----------------------------------------------|
| `users` → `user_roles` → `roles` | N:M  | Un usuario puede tener más de un rol          |
| `categories` → `products`        | 1:N  | Una categoría contiene muchos productos       |
| `users` → `orders`               | 1:N  | Un usuario tiene muchas órdenes               |
| `orders` → `order_items`         | 1:N  | Una orden tiene muchas líneas de detalle      |
| `order_items` → `products`       | N:1  | Muchos ítems pueden referenciar el mismo producto |
| `users` → `cart_items`           | 1:N  | Un usuario tiene ítems en su carrito          |
| `cart_items` → `products`        | N:1  | Muchos ítems de carrito pueden ser del mismo producto |

---

## DDL — Creación de tablas

```sql
-- Crear y usar la base de datos
CREATE DATABASE IF NOT EXISTS shopeasy;
USE shopeasy;

-- Roles
CREATE TABLE roles (
    id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- Usuarios
CREATE TABLE users (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100)  NOT NULL,
    email      VARCHAR(150)  NOT NULL UNIQUE,
    password   VARCHAR(255)  NOT NULL,
    enabled    BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

-- Tabla pivote usuarios-roles
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- Categorías
CREATE TABLE categories (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT
);

-- Productos
CREATE TABLE products (
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

-- Carrito
CREATE TABLE cart_items (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity   INT    NOT NULL DEFAULT 1,
    UNIQUE KEY uq_cart_item (user_id, product_id),
    CONSTRAINT fk_cart_user    FOREIGN KEY (user_id)    REFERENCES users(id),
    CONSTRAINT fk_cart_product FOREIGN KEY (product_id) REFERENCES products(id)
);

-- Órdenes
CREATE TABLE orders (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT        NOT NULL,
    status     ENUM('PENDIENTE','ENVIADO','ENTREGADO','CANCELADO') NOT NULL DEFAULT 'PENDIENTE',
    total      DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Detalle de orden
CREATE TABLE order_items (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id   BIGINT        NOT NULL,
    product_id BIGINT        NOT NULL,
    quantity   INT           NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_oi_order   FOREIGN KEY (order_id)   REFERENCES orders(id),
    CONSTRAINT fk_oi_product FOREIGN KEY (product_id) REFERENCES products(id)
);
```

---

## Datos de ejemplo

```sql
-- Roles
INSERT INTO roles (name) VALUES ('ROLE_ADMIN'), ('ROLE_USER');

-- Categorías
INSERT INTO categories (name, description) VALUES
    ('Electrónica',   'Smartphones, laptops, accesorios tecnológicos'),
    ('Ropa',          'Camisetas, pantalones, zapatillas'),
    ('Hogar',         'Muebles, decoración, electrodomésticos'),
    ('Libros',        'Ficción, educación, técnicos');

-- Productos de ejemplo
INSERT INTO products (name, description, price, stock, category_id) VALUES
    ('Smartphone XL',    'Pantalla 6.5", 128 GB, cámara 50MP',  599.99, 30, 1),
    ('Laptop Pro 15',    'Intel i7, 16 GB RAM, SSD 512 GB',    1199.99, 15, 1),
    ('Auriculares BT',   'Bluetooth 5.0, cancelación de ruido',   89.99, 50, 1),
    ('Camiseta Básica',  'Algodón 100%, tallas S-XXL',            19.99, 100, 2),
    ('Zapatillas Run',   'Suela amortiguada, transpirables',       79.99,  40, 2),
    ('Lámpara LED',      'Luz cálida/fría regulable, USB-C',       34.99,  60, 3),
    ('Clean Code',       'Robert C. Martin — Programación limpia', 39.99,  25, 4),
    ('El Principito',    'Antoine de Saint-Exupéry',               12.99,  80, 4);
```
