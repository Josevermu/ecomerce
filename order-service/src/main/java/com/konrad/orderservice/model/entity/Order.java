package com.konrad.orderservice.model.entity;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Orden de compra — generada al hacer checkout del carrito (punto 11).
 * Un Cart se convierte en Order al confirmar el pago.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Order {

    private String id;
    private String buyerId;
    private String sellerId;

    /** Copia de los ítems del carrito al momento de la compra */
    private List<Cart.CartItem> items;

    // ── Desglose financiero (punto 11) ─────────────────────────────────────────
    private Double subtotal;       // suma de precioUnitario * cantidad
    private Double comision;       // porcentaje por categoría (configurable)
    private Double costoEnvio;     // ciudad + peso (solo si domicilio=true)
    private Double iva;            // 19% solo a productos que apliquen IVA
    private Double total;          // subtotal + comision + costoEnvio + iva

    private OrderStatus status;
    private String paymentId;      // referencia al pago confirmado

    private boolean entregaDomicilio;
    private String ciudadEntrega;

    private LocalDateTime creadoEn;

    // ── Calificación post-compra (punto 12) ────────────────────────────────────
    private Integer calificacion;  // 1–10
    private String comentario;

    public enum OrderStatus {
        PAGADO,
        EN_PREPARACION,
        ENVIADO,
        ENTREGADO,
        CANCELADO
    }
}