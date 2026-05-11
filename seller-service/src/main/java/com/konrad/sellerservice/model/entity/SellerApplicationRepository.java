package com.konrad.sellerservice.model.entity;

import com.konrad.sellerservice.service.SellerApplication;
import com.konrad.sellerservice.service.SellerApplication.ApplicationStatus;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Repositorio mock en memoria.
 * En producción: JpaRepository<SellerApplication, String> con PostgreSQL.
 */
@Repository
public class SellerApplicationRepository {

    private final Map<String, SellerApplication> store = new ConcurrentHashMap<>();

    public SellerApplicationRepository() {
        loadMockData();
    }

    // -------------------------------------------------------------------------
    // MOCK DATA
    // -------------------------------------------------------------------------
    private void loadMockData() {
        store.put("app-001", SellerApplication.builder()
            .id("app-001").nombres("Carlos").apellidos("Ramírez")
            .identificacion("12345678").tipoPersona(SellerApplication.TipoPersona.NATURAL)
            .correo("carlos@tienda.com").pais("Colombia").ciudad("Bogotá")
            .telefono("3001234567")
            .documentos(List.of("cedula.pdf", "rut.pdf"))
            .status(ApplicationStatus.ACTIVA)
            .fechaSolicitud(LocalDateTime.now().minusDays(30))
            .totalCalificaciones(15).calificacionesBajas(1).promedioCalificacion(8.2)
            .build());

        store.put("app-002", SellerApplication.builder()
            .id("app-002").nombres("María").apellidos("López")
            .identificacion("87654321").tipoPersona(SellerApplication.TipoPersona.NATURAL)
            .correo("maria@correo.com").pais("Colombia").ciudad("Medellín")
            .telefono("3109876543")
            .documentos(List.of("cedula.pdf", "rut.pdf"))
            .status(ApplicationStatus.PENDIENTE)
            .fechaSolicitud(LocalDateTime.now().minusDays(2))
            .totalCalificaciones(0).calificacionesBajas(0).promedioCalificacion(0)
            .build());

        store.put("app-003", SellerApplication.builder()
            .id("app-003").nombres("Inversiones").apellidos("XYZ SAS")
            .identificacion("900123456").tipoPersona(SellerApplication.TipoPersona.JURIDICA)
            .correo("info@xyz.com").pais("Colombia").ciudad("Cali")
            .telefono("6021234567")
            .documentos(List.of("nit.pdf", "rut.pdf", "camara_comercio.pdf"))
            .status(ApplicationStatus.EN_MORA)
            .fechaSolicitud(LocalDateTime.now().minusDays(60))
            .totalCalificaciones(5).calificacionesBajas(0).promedioCalificacion(7.0)
            .build());

        store.put("app-004", SellerApplication.builder()
            .id("app-004").nombres("Pedro").apellidos("Gómez")
            .identificacion("11223344").tipoPersona(SellerApplication.TipoPersona.NATURAL)
            .correo("pedro@mail.com").pais("Colombia").ciudad("Barranquilla")
            .telefono("3152223344")
            .documentos(List.of("cedula.pdf", "rut.pdf"))
            .status(ApplicationStatus.RECHAZADA)
            .motivoRechazo("Calificación BAJA en Datacrédito")
            .fechaSolicitud(LocalDateTime.now().minusDays(10))
            .fechaDecision(LocalDateTime.now().minusDays(8))
            .totalCalificaciones(0).calificacionesBajas(0).promedioCalificacion(0)
            .build());
    }

    // -------------------------------------------------------------------------
    // Operaciones CRUD
    // -------------------------------------------------------------------------
    public SellerApplication save(SellerApplication app) {
        if (app.getId() == null) {
            app.setId("app-" + UUID.randomUUID().toString().substring(0, 8));
        }
        store.put(app.getId(), app);
        return app;
    }

    public Optional<SellerApplication> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    public Optional<SellerApplication> findByIdentificacion(String identificacion) {
        return store.values().stream()
            .filter(a -> a.getIdentificacion().equals(identificacion))
            .findFirst();
    }

    public List<SellerApplication> findByStatus(ApplicationStatus status) {
        return store.values().stream()
            .filter(a -> a.getStatus() == status)
            .collect(Collectors.toList());
    }

    public List<SellerApplication> findByFilters(String identificacion, String status,
                                                  LocalDateTime desde, LocalDateTime hasta) {
        return store.values().stream()
            .filter(a -> identificacion == null || a.getIdentificacion().contains(identificacion))
            .filter(a -> status == null || a.getStatus().name().equals(status))
            .filter(a -> desde == null || !a.getFechaSolicitud().isBefore(desde))
            .filter(a -> hasta == null || !a.getFechaSolicitud().isAfter(hasta))
            .sorted(Comparator.comparing(SellerApplication::getFechaSolicitud).reversed())
            .collect(Collectors.toList());
    }

    public List<SellerApplication> findAll() {
        return new ArrayList<>(store.values());
    }
}
