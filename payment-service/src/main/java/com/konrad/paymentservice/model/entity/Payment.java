package com.konrad.paymentservice.model.entity;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Payment {
    private String id;
    private String entityId;
    private String entityType;     // "ORDER" | "SUBSCRIPTION"
    private Double monto;
    private String metodo;         // "PSE" | "CREDIT_CARD" | "CONSIGNATION"
    private String estado;         // "APROBADO" | "PENDIENTE_BANCO" | "RECHAZADO"
    private String numeroAprobacion;
    private LocalDateTime fecha;
}
