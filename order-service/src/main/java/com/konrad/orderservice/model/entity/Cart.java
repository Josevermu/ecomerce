package com.konrad.orderservice.model.entity;

import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Carrito de compras — punto 11 del documento.
 * Al pagar se convierte en una Order (ver Order.java).
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Cart {
    private String id;
    private String buyerId;

    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    private boolean entregaDomicilio;
    private String ciudadEntrega;
    private LocalDateTime creadoEn;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CartItem {
        private String productId;
        private String nombre;
        private Integer cantidad;
        private Double precioUnitario;
        private String categoria;   // Para calcular comisión por categoría (punto 11.1)
        private Double peso;        // Para calcular costo de envío (punto 11.2)
        private boolean aplicaIVA; // Configurable por producto (punto 11.3)
    }
}
