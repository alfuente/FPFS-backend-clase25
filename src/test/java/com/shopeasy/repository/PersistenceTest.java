package com.shopeasy.repository;

import com.shopeasy.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest(showSql = false)
@Tag("integration")
@ActiveProfiles("test")
class PersistenceTest {
    @Autowired TestEntityManager em;
    @Autowired UserRepository users;
    @Autowired CategoryRepository categories;
    @Autowired RoleRepository roles;
    @Autowired CartItemRepository carts;
    @Autowired OrderRepository orders;

    User user(String email) {
        User user = new User(); user.setEmail(email); user.setName("User"); user.setPassword("encoded");
        return users.saveAndFlush(user);
    }
    @Test void emailMustBeUnique() {
        user("duplicate@test.com");
        assertThatThrownBy(() -> user("duplicate@test.com")).isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test void categoryNameMustBeUnique() {
        Category first = new Category(); first.setName("Books"); categories.saveAndFlush(first);
        Category duplicate = new Category(); duplicate.setName("Books");
        assertThatThrownBy(() -> categories.saveAndFlush(duplicate)).isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test void roleNameMustBeUnique() {
        roles.saveAndFlush(new Role(null, "ROLE_USER"));
        assertThatThrownBy(() -> roles.saveAndFlush(new Role(null, "ROLE_USER"))).isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test void cartUserAndProductMustBeUniqueTogether() {
        User user = user("cart@test.com");
        Product product = new Product(); product.setName("Product"); product.setPrice(BigDecimal.ONE); em.persistAndFlush(product);
        CartItem first = new CartItem(); first.setUser(user); first.setProduct(product); carts.saveAndFlush(first);
        CartItem duplicate = new CartItem(); duplicate.setUser(user); duplicate.setProduct(product);
        assertThatThrownBy(() -> carts.saveAndFlush(duplicate)).isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test void requiredUserFieldsCannotBeNull() {
        assertThatThrownBy(() -> users.saveAndFlush(new User())).isInstanceOf(DataIntegrityViolationException.class);
    }
    @Test void orderHistoryIsNewestFirstAndFilteredByOwner() {
        User owner = user("owner@test.com"); User other = user("other@test.com");
        Order old = order(owner); Order recent = order(owner); order(other);
        // Explicit timestamps avoid timing-dependent assertions and sleeps.
        em.getEntityManager().createQuery("update Order o set o.createdAt = :date where o.id = :id")
                .setParameter("date", LocalDateTime.of(2020, 1, 1, 0, 0)).setParameter("id", old.getId()).executeUpdate();
        em.getEntityManager().createQuery("update Order o set o.createdAt = :date where o.id = :id")
                .setParameter("date", LocalDateTime.of(2021, 1, 1, 0, 0)).setParameter("id", recent.getId()).executeUpdate();
        em.clear();
        assertThat(orders.findByUserEmailOrderByCreatedAtDesc(owner.getEmail())).extracting(Order::getId)
                .containsExactly(recent.getId(), old.getId());
    }
    Order order(User user) {
        Order order = new Order(); order.setUser(user); order.setTotal(BigDecimal.TEN);
        return orders.saveAndFlush(order);
    }
}
