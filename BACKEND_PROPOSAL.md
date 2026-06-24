# ShopEasy — Propuesta de Backend (Spring Boot)

## Stack y dependencias

| Dependencia                    | Propósito                                     |
|--------------------------------|-----------------------------------------------|
| `spring-boot-starter-web`      | API REST con controladores y JSON             |
| `spring-boot-starter-data-jpa` | Acceso a datos con Hibernate / JPA            |
| `spring-boot-starter-security` | Autenticación y autorización                  |
| `mysql-connector-j`            | Driver de conexión a MySQL                    |
| `modelmapper`                  | Conversión entre entidades y DTOs             |
| `jjwt-api` + `jjwt-impl`      | Generación y validación de tokens JWT         |
| `lombok`                       | Reducción de boilerplate (getters, constructores) |

---

## Estructura de paquetes

```
src/main/java/com/shopeasy/
│
├── config/
│   ├── SecurityConfig.java          ← Configuración de Spring Security + JWT filter
│   ├── WebConfig.java               ← CORS para permitir el frontend en localhost:5173
│   └── ModelMapperConfig.java       ← Bean de ModelMapper
│
├── controller/
│   ├── AuthController.java          ← POST /api/auth/register, /api/auth/login
│   ├── ProductController.java       ← CRUD de productos
│   ├── CategoryController.java      ← CRUD de categorías
│   ├── CartController.java          ← Ver, agregar y quitar del carrito
│   ├── OrderController.java         ← Crear orden, ver mis órdenes, admin
│   └── UserController.java          ← Listar usuarios (ADMIN)
│
├── service/
│   ├── AuthService.java             ← (interfaz)
│   ├── ProductService.java          ← (interfaz)
│   ├── CategoryService.java         ← (interfaz)
│   ├── CartService.java             ← (interfaz)
│   ├── OrderService.java            ← (interfaz)
│   ├── UserService.java             ← (interfaz)
│   └── impl/
│       ├── AuthServiceImpl.java
│       ├── ProductServiceImpl.java
│       ├── CategoryServiceImpl.java
│       ├── CartServiceImpl.java
│       ├── OrderServiceImpl.java
│       └── UserServiceImpl.java
│
├── repository/
│   ├── UserRepository.java          ← extends JpaRepository<User, Long>
│   ├── RoleRepository.java
│   ├── ProductRepository.java
│   ├── CategoryRepository.java
│   ├── CartItemRepository.java
│   ├── OrderRepository.java
│   └── OrderItemRepository.java
│
├── model/
│   ├── User.java
│   ├── Role.java
│   ├── Product.java
│   ├── Category.java
│   ├── CartItem.java
│   ├── Order.java
│   └── OrderItem.java
│
├── dto/
│   ├── request/
│   │   ├── RegisterRequest.java
│   │   ├── LoginRequest.java
│   │   ├── ProductRequest.java
│   │   ├── CategoryRequest.java
│   │   ├── CartItemRequest.java     ← { productId, quantity }
│   │   └── OrderStatusRequest.java  ← { status }
│   └── response/
│       ├── AuthResponse.java        ← { token, email, name, role }
│       ├── ProductResponse.java
│       ├── CategoryResponse.java
│       ├── CartItemResponse.java
│       ├── OrderResponse.java
│       └── OrderItemResponse.java
│
├── mapper/
│   ├── ProductMapper.java           ← Product ↔ ProductRequest/Response
│   ├── CategoryMapper.java
│   ├── CartMapper.java
│   └── OrderMapper.java
│
├── security/
│   ├── JwtUtil.java                 ← generateToken(), validateToken(), extractEmail()
│   ├── JwtAuthFilter.java           ← OncePerRequestFilter que lee el header Authorization
│   └── UserDetailsServiceImpl.java  ← loadUserByUsername() busca en UserRepository
│
└── exception/
    ├── GlobalExceptionHandler.java  ← @ControllerAdvice
    └── ResourceNotFoundException.java
```

---

## Configuración `application.properties`

```properties
# Base de datos
spring.datasource.url=jdbc:mysql://localhost:3306/shopeasy?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=tu_password

# JPA / Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

# JWT
jwt.secret=shopeasy_super_secret_key_2024
jwt.expiration=86400000

# Puerto
server.port=8080
```

---

## Entidades JPA — anotaciones clave

### `User.java`
```java
@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true)
    private String email;

    private String password;
    private boolean enabled = true;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Order> orders = new ArrayList<>();
}
```

### `Product.java`
```java
@Entity
@Table(name = "products")
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private BigDecimal price;
    private int stock;
    private String imageUrl;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;
}
```

### `Order.java`
```java
@Entity
@Table(name = "orders")
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDIENTE;

    private BigDecimal total;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items = new ArrayList<>();
}
```

---

## Endpoints y seguridad

### Autenticación (público)

| Método | Ruta                   | Body (JSON)                             | Respuesta                    |
|--------|------------------------|-----------------------------------------|------------------------------|
| POST   | `/api/auth/register`   | `{ name, email, password }`             | `{ token, email, name, role }` |
| POST   | `/api/auth/login`      | `{ email, password }`                   | `{ token, email, name, role }` |

### Productos

| Método | Ruta                   | Acceso  | Descripción                  |
|--------|------------------------|---------|------------------------------|
| GET    | `/api/products`        | Público | Listar todos los productos   |
| GET    | `/api/products/{id}`   | Público | Ver detalle de un producto   |
| POST   | `/api/products`        | ADMIN   | Crear producto               |
| PUT    | `/api/products/{id}`   | ADMIN   | Actualizar producto          |
| DELETE | `/api/products/{id}`   | ADMIN   | Eliminar producto            |

### Categorías

| Método | Ruta                     | Acceso  | Descripción         |
|--------|--------------------------|---------|---------------------|
| GET    | `/api/categories`        | Público | Listar categorías   |
| GET    | `/api/categories/{id}`   | Público | Ver categoría       |
| POST   | `/api/categories`        | ADMIN   | Crear categoría     |
| PUT    | `/api/categories/{id}`   | ADMIN   | Actualizar          |
| DELETE | `/api/categories/{id}`   | ADMIN   | Eliminar            |

### Carrito

| Método | Ruta                   | Acceso | Descripción                    |
|--------|------------------------|--------|--------------------------------|
| GET    | `/api/cart`            | USER   | Ver los ítems del carrito      |
| POST   | `/api/cart`            | USER   | Agregar producto `{ productId, quantity }` |
| PUT    | `/api/cart/{itemId}`   | USER   | Cambiar cantidad               |
| DELETE | `/api/cart/{itemId}`   | USER   | Quitar ítem del carrito        |

### Órdenes

| Método | Ruta                        | Acceso | Descripción                           |
|--------|-----------------------------|--------|---------------------------------------|
| POST   | `/api/orders`               | USER   | Crear orden desde el carrito actual   |
| GET    | `/api/orders/my`            | USER   | Ver mis órdenes                       |
| GET    | `/api/orders`               | ADMIN  | Ver todas las órdenes                 |
| GET    | `/api/orders/{id}`          | ADMIN  | Ver detalle de una orden              |
| PUT    | `/api/orders/{id}/status`   | ADMIN  | Cambiar estado `{ status: "ENVIADO" }` |

### Usuarios

| Método | Ruta          | Acceso | Descripción       |
|--------|---------------|--------|-------------------|
| GET    | `/api/users`  | ADMIN  | Listar usuarios   |

---

## Spring Security — flujo de autenticación

```
Cliente                     Backend
  │                            │
  │── POST /api/auth/login ───►│
  │   { email, password }      │── UserDetailsServiceImpl.loadUserByUsername()
  │                            │── BCryptPasswordEncoder.matches()
  │◄── { token: "eyJ..." } ───│── JwtUtil.generateToken()
  │                            │
  │── GET /api/cart ──────────►│
  │   Authorization: Bearer    │── JwtAuthFilter.doFilterInternal()
  │   eyJ...                   │── JwtUtil.validateToken()
  │                            │── SecurityContextHolder.setAuthentication()
  │◄── [ cart items ] ────────│── CartController → solo si rol USER ✓
```

### `SecurityConfig.java` — lógica de rutas

```java
http
  .csrf(csrf -> csrf.disable())
  .authorizeHttpRequests(auth -> auth
      .requestMatchers("/api/auth/**").permitAll()
      .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**").permitAll()
      .requestMatchers("/api/admin/**").hasRole("ADMIN")
      .anyRequest().authenticated()
  )
  .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
  .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
```

---

## ModelMapper — ejemplo de uso

```java
// Configuración (ModelMapperConfig.java)
@Bean
public ModelMapper modelMapper() {
    return new ModelMapper();
}

// En ProductServiceImpl.java
public ProductResponse getById(Long id) {
    Product product = productRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
    return modelMapper.map(product, ProductResponse.class);
}

public Product create(ProductRequest request) {
    Product product = modelMapper.map(request, Product.class);
    return productRepository.save(product);
}
```

---

## Manejo de errores

### `ResourceNotFoundException.java`
```java
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

### `GlobalExceptionHandler.java`
```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleForbidden(AccessDeniedException ex) {
        return ResponseEntity.status(403).body(Map.of("error", "Acceso denegado"));
    }
}
```

---

## Flujo de creación de una orden

```
1. Usuario llama POST /api/orders
2. OrderServiceImpl.createOrder(userEmail):
   a. Busca al usuario por email (del JWT)
   b. Obtiene los CartItems del usuario
   c. Valida que haya stock disponible en cada producto
   d. Calcula el total
   e. Crea la entidad Order con status = PENDIENTE
   f. Convierte cada CartItem en OrderItem (guarda unit_price actual)
   g. Descuenta el stock de cada producto
   h. Vacía el carrito del usuario
   i. Retorna el OrderResponse
```

---

## Temas académicos cubiertos en el backend

| Tema              | Dónde se aplica                              |
|-------------------|----------------------------------------------|
| `@Entity` / `@Table` | Todas las entidades en `model/`           |
| `@OneToMany`      | `User.orders`, `Order.items`, `Category.products` |
| `@ManyToOne`      | `Product.category`, `OrderItem.product`      |
| `@ManyToMany`     | `User.roles` (con tabla pivote `user_roles`) |
| `JpaRepository`   | Todos los `repository/`                      |
| `@Service`        | Todas las implementaciones en `service/impl/` |
| `@RestController` | Todos los controladores                      |
| `ModelMapper`     | `service/impl/` al mapear entidades ↔ DTOs   |
| `BCryptPasswordEncoder` | `AuthServiceImpl` al registrar usuario |
| JWT + Spring Security | `security/` + `SecurityConfig`          |
| `@PreAuthorize`   | Métodos de controlador con restricción de rol |
| `@ControllerAdvice` | `GlobalExceptionHandler`                  |
