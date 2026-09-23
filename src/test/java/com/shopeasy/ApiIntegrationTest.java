package com.shopeasy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopeasy.model.*;
import com.shopeasy.repository.*;
import com.shopeasy.security.JwtUtil;
import com.shopeasy.service.CartService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** API real, filtros JWT, servicios y persistencia; cada caso prepara sus propios datos. */
@SpringBootTest
@Tag("integration")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired CartItemRepository carts;
    @Autowired OrderRepository orders;
    @Autowired OrderItemRepository orderItems;
    @Autowired JwtUtil jwt;
    @Autowired BCryptPasswordEncoder encoder;
    @Autowired CartService cartService;
    User customer, other, admin;
    Category category;
    Product product;
    String token, otherToken, adminToken;

    @BeforeEach void seed() {
        orderItems.deleteAll(); orders.deleteAll(); carts.deleteAll();
        products.deleteAll(); categories.deleteAll(); users.deleteAll();
        customer = user("customer@test.com", "ROLE_USER");
        other = user("other@test.com", "ROLE_USER");
        admin = user("admin@test.com", "ROLE_ADMIN");
        token = jwt.generateToken(customer.getEmail());
        otherToken = jwt.generateToken(other.getEmail());
        adminToken = jwt.generateToken(admin.getEmail());
        category = new Category(); category.setName("Electronics"); category.setDescription("Devices");
        category = categories.save(category);
        product = product("Keyboard", "12.35", 10);
    }

    User user(String email, String role) {
        Role savedRole = roles.findByName(role).orElseGet(() -> roles.save(new Role(null, role)));
        User u = new User(); u.setName("Test User"); u.setEmail(email);
        u.setPassword(encoder.encode("password123")); u.setRoles(Set.of(savedRole));
        return users.save(u);
    }
    Product product(String name, String price, int stock) {
        Product p = new Product(); p.setName(name); p.setDescription("Description");
        p.setPrice(new BigDecimal(price)); p.setStock(stock); p.setCategory(category); p.setImageUrl("image.png");
        return products.save(p);
    }
    ResultActions call(String method, String path, String bearer, Object body) throws Exception {
        MockHttpServletRequestBuilder request = request(org.springframework.http.HttpMethod.valueOf(method), path);
        if (bearer != null) request.header("Authorization", "Bearer " + bearer);
        if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body));
        return mvc.perform(request);
    }
    JsonNode body(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }
    long add(String bearer, Product p, int quantity) throws Exception {
        return body(call("POST", "/api/cart", bearer, Map.of("productId", p.getId(), "quantity", quantity))
                .andExpect(status().isOk())).get("id").asLong();
    }
    long checkout() throws Exception {
        add(token, product, 2);
        return body(call("POST", "/api/orders", token, null).andExpect(status().isCreated())).get("id").asLong();
    }
    Map<String, Object> productBody(long categoryId) {
        return Map.of("name", "Mouse", "description", "Wireless", "price", 25.50,
                "stock", 4, "imageUrl", "mouse.png", "categoryId", categoryId);
    }

    @Test void registerPersistsEncodedPasswordAndUsableToken() throws Exception {
        JsonNode response = body(call("POST", "/api/auth/register", null,
                Map.of("name", "Ana", "email", "ana@test.com", "password", "secret123"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ROLE_USER"))
                .andExpect(jsonPath("$.name").value("Ana")).andExpect(jsonPath("$.password").doesNotExist()));
        User saved = users.findByEmail("ana@test.com").orElseThrow();
        assertThat(saved.getPassword()).isNotEqualTo("secret123");
        assertThat(encoder.matches("secret123", saved.getPassword())).isTrue();
        assertThat(saved.getRoles()).extracting(Role::getName).containsExactly("ROLE_USER");
        call("GET", "/api/cart", response.get("token").asText(), null).andExpect(status().isOk());
    }
    @Test void duplicateEmailIsBadRequestAndDoesNotCreateUser() throws Exception {
        call("POST", "/api/auth/register", null, Map.of("name", "Duplicate", "email", customer.getEmail(), "password", "new"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").isString());
        assertThat(users.count()).isEqualTo(3);
    }
    @Test void loginReturnsSignedTokenAndRole() throws Exception {
        JsonNode response = body(call("POST", "/api/auth/login", null,
                Map.of("email", admin.getEmail(), "password", "password123"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ROLE_ADMIN")));
        assertThat(jwt.extractEmail(response.get("token").asText())).isEqualTo(admin.getEmail());
    }
    @ParameterizedTest @ValueSource(strings = {"customer@test.com", "missing@test.com"})
    void invalidCredentialsAreUnauthorized(String email) throws Exception {
        call("POST", "/api/auth/login", null, Map.of("email", email, "password", "incorrect"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").isString());
    }
    @ParameterizedTest @ValueSource(strings = {"/api/products", "/api/categories"})
    void catalogIsPublic(String path) throws Exception {
        call("GET", path, null, null).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }
    @Test void productDetailAndCategoryFilter() throws Exception {
        call("GET", "/api/products/" + product.getId(), null, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Keyboard")).andExpect(jsonPath("$.price").value(12.35))
                .andExpect(jsonPath("$.categoryId").value(category.getId())).andExpect(jsonPath("$.categoryName").value("Electronics"))
                .andExpect(jsonPath("$.createdAt").exists());
        call("GET", "/api/products?categoryId=" + category.getId(), null, null).andExpect(jsonPath("$.length()").value(1));
        call("GET", "/api/products?categoryId=999999", null, null).andExpect(jsonPath("$.length()").value(0));
        call("GET", "/api/categories/" + category.getId(), null, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Devices"));
    }
    @Test void adminProductCrud() throws Exception {
        long id = body(call("POST", "/api/products", adminToken, productBody(category.getId()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.stock").value(4))).get("id").asLong();
        Map<String, Object> changed = new HashMap<>(productBody(category.getId())); changed.put("name", "Updated");
        call("PUT", "/api/products/" + id, adminToken, changed).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated")).andExpect(jsonPath("$.price").value(25.5));
        assertThat(products.findById(id).orElseThrow().getName()).isEqualTo("Updated");
        call("DELETE", "/api/products/" + id, adminToken, null).andExpect(status().isNoContent());
        assertThat(products.existsById(id)).isFalse();
    }
    @Test void adminCategoryCrud() throws Exception {
        long id = body(call("POST", "/api/categories", adminToken, Map.of("name", "Books", "description", "Reading"))
                .andExpect(status().isCreated())).get("id").asLong();
        call("PUT", "/api/categories/" + id, adminToken, Map.of("name", "Novels", "description", "Fiction"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Novels"));
        assertThat(categories.findById(id).orElseThrow().getDescription()).isEqualTo("Fiction");
        call("DELETE", "/api/categories/" + id, adminToken, null).andExpect(status().isNoContent());
        assertThat(categories.existsById(id)).isFalse();
    }
    @ParameterizedTest @CsvSource({"GET,/api/products/999999", "PUT,/api/products/999999", "DELETE,/api/products/999999",
            "GET,/api/categories/999999", "PUT,/api/categories/999999", "DELETE,/api/categories/999999",
            "GET,/api/users/999999", "GET,/api/orders/999999", "PUT,/api/orders/999999/status",
            "PUT,/api/cart/999999?quantity=2", "DELETE,/api/cart/999999"})
    void missingResourcesReturn404(String method, String path) throws Exception {
        Object payload = path.contains("status") ? Map.of("status", "ENVIADO") : productBody(category.getId());
        call(method, path, adminToken, payload).andExpect(status().isNotFound()).andExpect(jsonPath("$.error").isString());
    }
    @ParameterizedTest @ValueSource(strings = {"POST", "PUT"})
    void missingCategoryRejectsProductWrite(String method) throws Exception {
        call(method, "/api/products" + (method.equals("PUT") ? "/" + product.getId() : ""), adminToken, productBody(999999))
                .andExpect(status().isNotFound());
        assertThat(products.count()).isEqualTo(1);
        assertThat(products.findById(product.getId()).orElseThrow().getName()).isEqualTo("Keyboard");
    }
    @ParameterizedTest @CsvSource({"GET,/api/cart", "POST,/api/cart", "PUT,/api/cart/1?quantity=2", "DELETE,/api/cart/1",
            "POST,/api/orders", "GET,/api/orders/my", "GET,/api/orders", "GET,/api/orders/1", "PUT,/api/orders/1/status",
            "GET,/api/users", "GET,/api/users/1", "POST,/api/products", "PUT,/api/products/1", "DELETE,/api/products/1",
            "POST,/api/categories", "PUT,/api/categories/1", "DELETE,/api/categories/1"})
    void anonymousCannotAccessProtectedEndpoints(String method, String path) throws Exception {
        call(method, path, null, Map.of()).andExpect(status().isForbidden());
    }
    @ParameterizedTest @CsvSource({"GET,/api/users", "GET,/api/users/1", "GET,/api/orders", "GET,/api/orders/1",
            "PUT,/api/orders/1/status", "POST,/api/products", "PUT,/api/products/1", "DELETE,/api/products/1",
            "POST,/api/categories", "PUT,/api/categories/1", "DELETE,/api/categories/1"})
    void customerCannotAccessAdminEndpoints(String method, String path) throws Exception {
        call(method, path, token, Map.of()).andExpect(status().isForbidden());
    }
    @Test void invalidJwtCannotAccessCartButPublicCatalogWorks() throws Exception {
        call("GET", "/api/cart", "invalid", null).andExpect(status().isForbidden());
        call("GET", "/api/products", "invalid", null).andExpect(status().isOk());
    }
    @Test void cartAddsMergesUpdatesAndRemovesItems() throws Exception {
        call("GET", "/api/cart", token, null).andExpect(jsonPath("$.length()").value(0));
        long id = add(token, product, 2);
        assertThat(add(token, product, 1)).isEqualTo(id);
        call("GET", "/api/cart", token, null).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].quantity").value(3)).andExpect(jsonPath("$[0].subtotal").value(37.05))
                .andExpect(jsonPath("$[0].productName").value("Keyboard")).andExpect(jsonPath("$[0].productImageUrl").value("image.png"));
        call("PUT", "/api/cart/" + id + "?quantity=4", token, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal").value(49.4));
        call("DELETE", "/api/cart/" + id, token, null).andExpect(status().isNoContent());
        assertThat(carts.findByUserEmail(customer.getEmail())).isEmpty();
    }
    @Test void cartIsIsolatedAndOtherUserCannotModifyOrRemoveItem() throws Exception {
        long id = add(token, product, 2);
        call("GET", "/api/cart", otherToken, null).andExpect(jsonPath("$.length()").value(0));
        call("PUT", "/api/cart/" + id + "?quantity=5", otherToken, null).andExpect(status().isBadRequest());
        call("DELETE", "/api/cart/" + id, otherToken, null).andExpect(status().isBadRequest());
        assertThat(carts.findById(id).orElseThrow().getQuantity()).isEqualTo(2);
    }
    @Test void missingProductCannotBeAdded() throws Exception {
        call("POST", "/api/cart", token, Map.of("productId", 999999, "quantity", 1)).andExpect(status().isNotFound());
        assertThat(carts.count()).isZero();
    }
    @Test void clearCartOnlyRemovesCurrentUsersItems() throws Exception {
        add(token, product, 1); add(otherToken, product, 2);
        cartService.clearCart(customer.getEmail());
        assertThat(carts.findByUserEmail(customer.getEmail())).isEmpty();
        assertThat(carts.findByUserEmail(other.getEmail())).hasSize(1);
    }
    @Test void checkoutCalculatesTotalPersistsItemsDecrementsStockAndClearsOnlyOwnCart() throws Exception {
        Product second = product("Mouse", "0.10", 3);
        add(token, product, 2); add(token, second, 3); add(otherToken, product, 1);
        JsonNode response = body(call("POST", "/api/orders", token, null).andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(25.0)).andExpect(jsonPath("$.status").value("PENDIENTE"))
                .andExpect(jsonPath("$.items.length()").value(2)).andExpect(jsonPath("$.userEmail").value(customer.getEmail())));
        assertThat(orders.existsById(response.get("id").asLong())).isTrue();
        assertThat(orderItems.count()).isEqualTo(2);
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(8);
        assertThat(products.findById(second.getId()).orElseThrow().getStock()).isZero();
        assertThat(carts.findByUserEmail(customer.getEmail())).isEmpty();
        assertThat(carts.findByUserEmail(other.getEmail())).hasSize(1);
    }
    @Test void emptyCartCannotCheckout() throws Exception {
        call("POST", "/api/orders", token, null).andExpect(status().isBadRequest());
        assertThat(orders.count()).isZero();
    }
    @Test void insufficientStockLeavesEntireCartAndInventoryUnchanged() throws Exception {
        Product second = product("Scarce", "1.00", 1);
        add(token, product, 2); add(token, second, 2);
        call("POST", "/api/orders", token, null).andExpect(status().isBadRequest());
        assertThat(orders.count()).isZero(); assertThat(orderItems.count()).isZero();
        assertThat(products.findById(product.getId()).orElseThrow().getStock()).isEqualTo(10);
        assertThat(products.findById(second.getId()).orElseThrow().getStock()).isEqualTo(1);
        assertThat(carts.findByUserEmail(customer.getEmail())).hasSize(2);
    }
    @Test void orderPriceIsHistoricalAndOrdersAreIsolated() throws Exception {
        long id = checkout();
        product.setPrice(new BigDecimal("99.00")); products.save(product);
        call("GET", "/api/orders/my", token, null).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].items[0].unitPrice").value(12.35));
        call("GET", "/api/orders/my", otherToken, null).andExpect(jsonPath("$.length()").value(0));
        call("GET", "/api/orders", adminToken, null).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        call("GET", "/api/orders/" + id, adminToken, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].subtotal").value(24.7));
    }
    @ParameterizedTest @EnumSource(OrderStatus.class)
    void adminCanSetEverySupportedOrderStatus(OrderStatus status) throws Exception {
        long id = checkout();
        call("PUT", "/api/orders/" + id + "/status", adminToken, Map.of("status", status.name()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(status.name()));
        assertThat(orders.findById(id).orElseThrow().getStatus()).isEqualTo(status);
    }
    @Test void adminCanReadUsersWithoutPasswordExposure() throws Exception {
        call("GET", "/api/users", adminToken, null).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[*].password").doesNotExist());
        call("GET", "/api/users/" + customer.getId(), adminToken, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(customer.getEmail())).andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER")).andExpect(jsonPath("$.password").doesNotExist());
    }
    @Test void allowedCorsPreflight() throws Exception {
        mvc.perform(options("/api/cart").header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST").header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }
    @Test void unknownCorsOriginIsRejected() throws Exception {
        mvc.perform(options("/api/cart").header("Origin", "https://untrusted.example")
                .header("Access-Control-Request-Method", "POST")).andExpect(status().isForbidden());
    }
    @Test void customRepositoryQueries() {
        assertThat(products.findByNameContainingIgnoreCase("KEY")).extracting(Product::getId).containsExactly(product.getId());
        assertThat(products.findByNameContainingIgnoreCase("missing")).isEmpty();
        assertThat(categories.existsByName("Electronics")).isTrue();
        assertThat(categories.existsByName("missing")).isFalse();
    }
}
