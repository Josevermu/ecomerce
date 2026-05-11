package com.konrad.authservice.model.entity;

import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repositorio mock con datos en memoria.
 * En producción: JpaRepository<User, String> con PostgreSQL.
 */
@Repository
public class UserRepository {

    private final Map<String, User> store = new ConcurrentHashMap<>();

    public UserRepository() {
        loadMockData();
    }

    // -------------------------------------------------------------------------
    // MOCK DATA — usuarios pre-cargados para pruebas
    // -------------------------------------------------------------------------
    private void loadMockData() {
        // Contraseñas "hasheadas" con BCrypt (mock: solo el texto plano marcado)
        // En producción: BCryptPasswordEncoder.encode(password)
        store.put("u-001", User.builder()
            .id("u-001").email("admin@konrad.com")
            .passwordHash("$MOCK$Admin123")
            .role(User.Role.ADMIN)
            .active(true).createdAt(LocalDateTime.now().minusDays(90)).build());

        store.put("u-002", User.builder()
            .id("u-002").email("director@konrad.com")
            .passwordHash("$MOCK$Director1")
            .role(User.Role.DIRECTOR)
            .active(true).createdAt(LocalDateTime.now().minusDays(60)).build());

        store.put("u-003", User.builder()
            .id("u-003").email("vendedor@tienda.com")
            .passwordHash("$MOCK$Seller123")
            .role(User.Role.SELLER).relatedEntityId("seller-001")
            .active(true).createdAt(LocalDateTime.now().minusDays(30)).build());

        store.put("u-004", User.builder()
            .id("u-004").email("comprador@gmail.com")
            .passwordHash("$MOCK$Buyer1234")
            .role(User.Role.BUYER).relatedEntityId("buyer-001")
            .active(true).createdAt(LocalDateTime.now().minusDays(10)).build());
    }

    public Optional<User> findByEmail(String email) {
        return store.values().stream()
            .filter(u -> u.getEmail().equalsIgnoreCase(email))
            .findFirst();
    }

    public Optional<User> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public User save(User user) {
        if (user.getId() == null) {
            user.setId("u-" + UUID.randomUUID().toString().substring(0, 8));
        }
        store.put(user.getId(), user);
        return user;
    }

    public List<User> findAll() {
        return new ArrayList<>(store.values());
    }

    public boolean existsByEmail(String email) {
        return store.values().stream().anyMatch(u -> u.getEmail().equalsIgnoreCase(email));
    }
}
