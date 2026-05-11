package com.konrad.bamservice.controller;

import com.konrad.bamservice.dto.BamDtos.*;
import com.konrad.bamservice.service.BamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * BAM Controller — expone el tablero de control al Director Comercial (punto 13).
 * Todos los endpoints requieren rol DIRECTOR o ADMIN (validado por el gateway).
 */
@RestController
@RequestMapping("/bam")
@RequiredArgsConstructor
public class BamController {

    private final BamService bamService;

    // ─── Tablero completo ─────────────────────────────────────────────────────

    /**
     * GET /bam/dashboard — tablero con los 3 KPIs + resumen general.
     * Endpoint principal para la pantalla del Director Comercial (punto 13).
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> dashboard() {
        return ResponseEntity.ok(bamService.getDashboard());
    }

    // ─── KPIs individuales ────────────────────────────────────────────────────

    /** GET /bam/kpi/top-product — producto con mayor venta en el último mes (punto 13.1) */
    @GetMapping("/kpi/top-product")
    public ResponseEntity<TopProductResponse> topProduct() {
        return ResponseEntity.ok(bamService.getTopProduct());
    }

    /** GET /bam/kpi/top-category — categoría con mayores consultas en la última semana (punto 13.2) */
    @GetMapping("/kpi/top-category")
    public ResponseEntity<TopCategoryResponse> topCategory() {
        return ResponseEntity.ok(bamService.getTopCategory());
    }

    /** GET /bam/kpi/subscriptions — comportamiento de suscripciones por semestre (punto 13.3) */
    @GetMapping("/kpi/subscriptions")
    public ResponseEntity<List<SubscriptionTrendResponse>> subscriptionTrend() {
        return ResponseEntity.ok(bamService.getSubscriptionTrend());
    }

    // ─── Auditoría (punto 15.2) ────────────────────────────────────────────────

    /**
     * GET /bam/audit — log completo de auditoría (solo ADMIN).
     * Muestra: acción, usuario, entidad, fecha, hora (RNF Mantenimiento 1).
     */
    @GetMapping("/audit")
    public ResponseEntity<List<AuditEntry>> auditAll() {
        return ResponseEntity.ok(bamService.getAuditLog());
    }

    /** GET /bam/audit?usuario=... — auditoría filtrada por usuario */
    @GetMapping("/audit/by-user")
    public ResponseEntity<List<AuditEntry>> auditByUser(@RequestParam String usuario) {
        return ResponseEntity.ok(bamService.getAuditByUsuario(usuario));
    }

    /** GET /bam/audit/by-entity?entidad=ORDER — auditoría filtrada por entidad */
    @GetMapping("/audit/by-entity")
    public ResponseEntity<List<AuditEntry>> auditByEntity(@RequestParam String entidad) {
        return ResponseEntity.ok(bamService.getAuditByEntidad(entidad));
    }

    /**
     * POST /bam/audit — registrar entrada de auditoría desde otro microservicio.
     * Body: { "accion": "CREATE", "usuario": "...", "entidad": "ORDER",
     *         "entidadId": "ord-001", "detalle": "..." }
     */
    @PostMapping("/audit")
    public ResponseEntity<AuditEntry> registerAudit(@RequestBody Map<String, String> body) {
        AuditEntry entry = bamService.registerAudit(
                body.getOrDefault("accion",    "UNKNOWN"),
                body.getOrDefault("usuario",   "system"),
                body.getOrDefault("entidad",   "UNKNOWN"),
                body.getOrDefault("entidadId", ""),
                body.getOrDefault("detalle",   "")
        );
        return ResponseEntity.ok(entry);
    }
}