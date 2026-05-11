package com.konrad.bamservice.model;

import com.konrad.bamservice.dto.BamDtos.AuditEntry;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registro de auditoría en memoria.
 * En producción: tabla audit_log en PostgreSQL con INSERT-only (sin UPDATE/DELETE).
 * Cada microservicio debería publicar eventos de auditoría hacia este servicio.
 *
 * MOCK: cargado con datos representativos de todas las acciones del sistema.
 * RNF Mantenimiento: cada CRUD registra acción, usuario, fecha y hora (punto 15.1).
 */
@Repository
public class AuditRepository {

    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter FMT_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Map<String, AuditEntry> store = new ConcurrentHashMap<>();

    public AuditRepository() { loadMockData(); }

    private void loadMockData() {
        save("LOGIN",   "director@konrad.com", "USER",    "u-002", "Login exitoso");
        save("CREATE",  "vendedor@tienda.com", "PRODUCT", "prod-001", "Publicó 'Camiseta Polo Classic'");
        save("CREATE",  "comprador@gmail.com", "ORDER",   "ord-001", "Compra por $703.000");
        save("UPDATE",  "vendedor@tienda.com", "SELLER",  "app-001", "Solicitud APROBADA por director@konrad.com");
        save("PAYMENT", "comprador@gmail.com", "PAYMENT", "pay-002", "Pago CREDIT_CARD aprobado: CC-E5F6G7H8");
        save("DELETE",  "vendedor@tienda.com", "PRODUCT", "prod-999", "Producto desactivado");
        save("CREATE",  "admin@konrad.com",    "USER",    "u-003",   "Vendedor registrado tras aprobación");
        save("UPDATE",  "comprador@gmail.com", "ORDER",   "ord-001", "Calificación 9/10 registrada");
    }

    public AuditEntry save(String accion, String usuario, String entidad,
                           String entidadId, String detalle) {
        LocalDateTime now = LocalDateTime.now();
        AuditEntry entry = AuditEntry.builder()
                .id("aud-" + UUID.randomUUID().toString().substring(0, 8))
                .accion(accion).usuario(usuario)
                .entidad(entidad).entidadId(entidadId)
                .fecha(now.format(FMT_DATE))
                .hora(now.format(FMT_TIME))
                .detalle(detalle)
                .build();
        store.put(entry.getId(), entry);
        return entry;
    }

    public List<AuditEntry> findAll() {
        return store.values().stream()
                .sorted(Comparator.comparing(AuditEntry::getFecha)
                        .thenComparing(AuditEntry::getHora).reversed())
                .collect(Collectors.toList());
    }

    public List<AuditEntry> findByUsuario(String usuario) {
        return store.values().stream()
                .filter(e -> e.getUsuario().equalsIgnoreCase(usuario))
                .collect(Collectors.toList());
    }

    public List<AuditEntry> findByEntidad(String entidad) {
        return store.values().stream()
                .filter(e -> e.getEntidad().equalsIgnoreCase(entidad))
                .collect(Collectors.toList());
    }
}