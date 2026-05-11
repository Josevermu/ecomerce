package com.konrad.bamservice.service;

import com.konrad.bamservice.dto.BamDtos.*;
import com.konrad.bamservice.model.AuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Servicio BAM — Business Activity Monitoring.
 *
 * En producción: consultaría order-service, product-service y seller-service
 * vía WebClient para obtener datos en tiempo real.
 *
 * MOCK: calcula KPIs sobre datos simulados representativos de los 3 KPIs
 * del enunciado (punto 13) y del job de tendencias automáticas (punto 14).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BamService {

    private final AuditRepository auditRepo;

    // ─── KPI 1: Producto con mayor venta en el último mes (punto 13.1) ─────────

    public TopProductResponse getTopProduct() {
        log.info("[BAM] Calculando producto con mayor venta del último mes");

        // Mock: simula el resultado que vendría del order-service agrupado por productId
        return TopProductResponse.builder()
                .productId("prod-001")
                .nombre("Camiseta Polo Classic")
                .categoria("Ropa")
                .unidadesVendidas(47)
                .ingresoTotal(8_460_000.0)
                .periodo("ULTIMO_MES")
                .build();
    }

    // ─── KPI 2: Categoría con mayores consultas en la última semana (punto 13.2) ─

    public TopCategoryResponse getTopCategory() {
        log.info("[BAM] Calculando categoría con mayores consultas de la última semana");

        // Mock: simula conteo de hits en el endpoint GET /products/search?categoria=...
        return TopCategoryResponse.builder()
                .categoria("Electrónica")
                .totalConsultas(1_340)
                .periodo("ULTIMA_SEMANA")
                .productosDestacados(List.of(
                        "Audífonos Bluetooth Pro",
                        "Smartwatch FitPro X2",
                        "Parlante Portátil JBL"))
                .build();
    }

    // ─── KPI 3: Comportamiento suscripciones por semestres (punto 13.3) ─────────

    public List<SubscriptionTrendResponse> getSubscriptionTrend() {
        log.info("[BAM] Calculando tendencia de suscripciones por semestre");

        // Mock: 4 semestres de datos históricos simulados
        return List.of(
                buildSemestre("2023-S1", 12, 2, 1, 9),
                buildSemestre("2023-S2", 18, 3, 2, 13),
                buildSemestre("2024-S1", 31, 4, 3, 24),
                buildSemestre("2024-S2", 45, 5, 4, 36)
        );
    }

    private SubscriptionTrendResponse buildSemestre(String semestre,
                                                    int nuevas, int canceladas,
                                                    int mora, int activas) {
        double tasa = nuevas > 0 ? Math.round((activas * 100.0 / nuevas) * 10) / 10.0 : 0;
        return SubscriptionTrendResponse.builder()
                .semestre(semestre)
                .nuevasSuscripciones(nuevas)
                .cancelaciones(canceladas)
                .enMora(mora)
                .activas(activas)
                .tasaRetencion(tasa)
                .build();
    }

    // ─── Tablero completo ─────────────────────────────────────────────────────

    public DashboardResponse getDashboard() {
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("totalVendedoresActivos",  24);
        resumen.put("totalCompradoresRegistrados", 312);
        resumen.put("ordenesUltimoMes",        189);
        resumen.put("ingresoUltimoMesCOP",     34_580_000);
        resumen.put("tasaConversionCarrito",   "63%");
        resumen.put("calificacionPromedioPlataforma", 7.8);

        return DashboardResponse.builder()
                .productoMasVendido(getTopProduct())
                .categoriaMasConsultada(getTopCategory())
                .tendenciaSuscripciones(getSubscriptionTrend())
                .resumenGeneral(resumen)
                .generadoEn(LocalDateTime.now().toString())
                .build();
    }

    // ─── Tendencias automáticas (punto 14) — job cada 24 horas ─────────────────

    /**
     * Job diario: detecta productos más consultados y los registra como tendencia.
     * En producción: consulta métricas reales y envía correos promocionales
     * a compradores registrados cuyo perfil de redes sociales coincide con
     * las categorías tendencia (instagram/twitter del buyer — punto 14.1).
     */
    @Scheduled(cron = "0 0 8 * * *")  // todos los días a las 8 AM
    public void detectTrends() {
        log.info("[BAM] Ejecutando detección automática de tendencias");

        List<TrendAlert> alertas = List.of(
                TrendAlert.builder()
                        .tipo("PRODUCTO_POPULAR")
                        .descripcion("Audífonos Bluetooth Pro aumentó consultas 340% esta semana")
                        .productosRecomendados(List.of("prod-003", "prod-007", "prod-011"))
                        .compradorsBeneficiados(312)
                        .build(),
                TrendAlert.builder()
                        .tipo("CATEGORIA_TENDENCIA")
                        .descripcion("Electrónica es la categoría más buscada 3 semanas consecutivas")
                        .productosRecomendados(List.of("prod-003", "prod-005"))
                        .compradorsBeneficiados(180)
                        .build()
        );

        // Mock: log de las alertas detectadas
        alertas.forEach(a ->
                log.info("[BAM-TREND] {} — {} compradores por notificar",
                        a.getTipo(), a.getCompradorsBeneficiados()));

        // En producción: llamar a notification-service para enviar correos masivos
    }

    // ─── Auditoría (punto 15.2) ────────────────────────────────────────────────

    public List<AuditEntry> getAuditLog() {
        return auditRepo.findAll();
    }

    public List<AuditEntry> getAuditByUsuario(String usuario) {
        return auditRepo.findByUsuario(usuario);
    }

    public List<AuditEntry> getAuditByEntidad(String entidad) {
        return auditRepo.findByEntidad(entidad);
    }

    public AuditEntry registerAudit(String accion, String usuario,
                                    String entidad, String entidadId, String detalle) {
        return auditRepo.save(accion, usuario, entidad, entidadId, detalle);
    }
}