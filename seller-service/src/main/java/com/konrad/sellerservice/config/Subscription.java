package com.konrad.sellerservice.config;


import lombok.*;
import java.time.LocalDateTime;

// ─────────────────────────────────────────────────────────────────────────────

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Subscription {
    private String id;
    private String sellerId;
    private TipoSuscripcion tipo;        // MENSUAL | SEMESTRAL | ANUAL
    private double monto;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaVencimiento;
    private String paymentId;

    public enum TipoSuscripcion { MENSUAL, SEMESTRAL, ANUAL }
}