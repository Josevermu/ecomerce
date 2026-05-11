package com.konrad.paymentservice.model.entity;

import com.konrad.paymentservice.model.entity.Payment;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/** Mock en memoria. En producción: JpaRepository con PostgreSQL. */
@Repository
public class PaymentRepository {

    private final Map<String, Payment> store = new ConcurrentHashMap<>();

    public PaymentRepository() { loadMockData(); }

    private void loadMockData() {
        store.put("pay-001", Payment.builder()
            .id("pay-001").entityId("app-001").entityType("SUBSCRIPTION")
            .monto(99000.0).metodo("PSE").estado("APROBADO")
            .numeroAprobacion("PSE-A1B2C3D4")
            .fecha(LocalDateTime.now().minusDays(30)).build());

        store.put("pay-002", Payment.builder()
            .id("pay-002").entityId("ord-001").entityType("ORDER")
            .monto(250000.0).metodo("CREDIT_CARD").estado("APROBADO")
            .numeroAprobacion("CC-E5F6G7H8")
            .fecha(LocalDateTime.now().minusDays(5)).build());

        store.put("pay-003", Payment.builder()
            .id("pay-003").entityId("ord-002").entityType("ORDER")
            .monto(75000.0).metodo("CONSIGNATION").estado("PENDIENTE_BANCO")
            .numeroAprobacion("REC-I9J0K1L2")
            .fecha(LocalDateTime.now().minusDays(1)).build());
    }

    public Payment save(Payment p) {
        if (p.getId() == null) p.setId("pay-" + UUID.randomUUID().toString().substring(0, 8));
        store.put(p.getId(), p);
        return p;
    }

    public Optional<Payment> findById(String id) { return Optional.ofNullable(store.get(id)); }

    public List<Payment> findByEntityId(String entityId) {
        return store.values().stream()
            .filter(p -> p.getEntityId().equals(entityId))
            .collect(Collectors.toList());
    }

    public List<Payment> findAll() { return new ArrayList<>(store.values()); }
}
