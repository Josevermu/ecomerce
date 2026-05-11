package com.konrad.bamservice.dto;

import lombok.*;
import java.util.List;
import java.util.Map;

public class BamDtos {

    // ── KPI 1: Producto con mayor venta (punto 13.1) ──────────────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TopProductResponse {
        private String productId;
        private String nombre;
        private String categoria;
        private int unidadesVendidas;
        private double ingresoTotal;
        private String periodo;      // "ULTIMO_MES"
    }

    // ── KPI 2: Categoría con mayores consultas (punto 13.2) ───────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TopCategoryResponse {
        private String categoria;
        private int totalConsultas;
        private String periodo;      // "ULTIMA_SEMANA"
        private List<String> productosDestacados;
    }

    // ── KPI 3: Suscripciones por semestre (punto 13.3) ────────────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SubscriptionTrendResponse {
        private String semestre;          // "2024-S1", "2024-S2", ...
        private int nuevasSuscripciones;
        private int cancelaciones;
        private int enMora;
        private int activas;
        private double tasaRetencion;     // activas / nuevas * 100
    }

    // ── Tablero completo ──────────────────────────────────────────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DashboardResponse {
        private TopProductResponse productoMasVendido;
        private TopCategoryResponse categoriaMasConsultada;
        private List<SubscriptionTrendResponse> tendenciaSuscripciones;
        private Map<String, Object> resumenGeneral;
        private String generadoEn;
    }

    // ── Tendencias para correos automáticos (punto 14) ────────────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TrendAlert {
        private String tipo;              // "PRODUCTO_POPULAR" | "CATEGORIA_TENDENCIA"
        private String descripcion;
        private List<String> productosRecomendados;
        private int compradorsBeneficiados;
    }

    // ── Auditoría (punto 15.2) ────────────────────────────────────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AuditEntry {
        private String id;
        private String accion;     // CREATE | UPDATE | DELETE | LOGIN | PAYMENT
        private String usuario;
        private String entidad;    // ORDER | SELLER | PRODUCT | BUYER
        private String entidadId;
        private String fecha;
        private String hora;
        private String detalle;
    }
}