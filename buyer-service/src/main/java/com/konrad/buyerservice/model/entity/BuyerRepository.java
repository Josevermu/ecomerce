package com.konrad.buyerservice.model.entity;

import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repositorio mock en memoria.
 * En producción: JpaRepository<Buyer, String> con PostgreSQL.
 */
@Repository
public class BuyerRepository {

    private final Map<String, Buyer> store = new ConcurrentHashMap<>();

    public BuyerRepository() { loadMockData(); }

    private void loadMockData() {
        store.put("buyer-001", Buyer.builder()
                .id("buyer-001")
                .nombres("Ana").apellidos("Torres")
                .identificacion("55443322")
                .correo("comprador@gmail.com")
                .pais("Colombia").ciudad("Bogotá")
                .direccion("Cra 15 # 80-20 Apto 301")
                .telefono("3001112233")
                .twitter("@anatorres").instagram("ana.torres.co")
                .activo(true).creadoEn(LocalDateTime.now().minusDays(10))
                .build());

        store.put("buyer-002", Buyer.builder()
                .id("buyer-002")
                .nombres("Luis").apellidos("Herrera")
                .identificacion("99887766")
                .correo("luis.herrera@correo.com")
                .pais("Colombia").ciudad("Medellín")
                .direccion("Cll 50 # 40-10")
                .telefono("3109998877")
                .twitter("@luisH").instagram("")
                .activo(true).creadoEn(LocalDateTime.now().minusDays(5))
                .build());
    }

    public Buyer save(Buyer b) {
        if (b.getId() == null) b.setId("buyer-" + UUID.randomUUID().toString().substring(0, 8));
        store.put(b.getId(), b);
        return b;
    }

    public Optional<Buyer> findById(String id)       { return Optional.ofNullable(store.get(id)); }
    public Optional<Buyer> findByEmail(String email) {
        return store.values().stream()
                .filter(b -> b.getCorreo().equalsIgnoreCase(email)).findFirst();
    }
    public Optional<Buyer> findByIdentificacion(String id) {
        return store.values().stream()
                .filter(b -> b.getIdentificacion().equals(id)).findFirst();
    }
    public boolean existsByEmail(String email) { return findByEmail(email).isPresent(); }
    public List<Buyer> findAll()               { return new ArrayList<>(store.values()); }
}