package com.konrad.sellerservice.bus;

import lombok.*;

/**
 * Sobre común para todos los mensajes del Service Bus.
 * Se replica en cada servicio porque no hay módulo shared.
 * El campo eventType determina cómo procesar el payload.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BusEventDto {

    // ── Identificación del evento ──────────────────────────────────────────────
    private String eventType;        // SELLER_SUBMITTED | SELLER_APPROVED | SELLER_REJECTED |
    // SELLER_RETURNED | SUBSCRIPTION_EXPIRED | SELLER_SUSPENDED |
    // BUYER_REGISTERED |
    // PAYMENT_CONFIRMED | PAYMENT_REJECTED |
    // ORDER_RATED
    private String timestamp;        // ISO-8601

    // ── Campos comunes ─────────────────────────────────────────────────────────
    private String entityId;         // applicationId | buyerId | paymentId | orderId
    private String correo;
    private String nombre;

    // ── Campos específicos por evento ──────────────────────────────────────────
    private String motivo;           // rechazo / suspensión
    private String sellerId;
    private String paymentId;
    private String orderId;
    private String tipoSuscripcion;  // MENSUAL | SEMESTRAL | ANUAL
    private String entityType;       // ORDER | SUBSCRIPTION
    private Integer calificacion;    // 1-10 (ORDER_RATED)
    private String comentario;       // (ORDER_RATED)
}