package com.konrad.paymentservice.dto;

import lombok.*;

public class PaymentDtos {

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class PaymentRequest {
        private String entityId;               // orderId o subscriptionId
        private String entityType;             // "ORDER" | "SUBSCRIPTION"
        private Double monto;
        private String metodoPago;             // "PSE" | "CREDIT_CARD" | "CONSIGNATION"
        // PSE
        private String pagadorIdentificacion;
        private String pagadorTipo;            // "NATURAL" | "JURIDICA"
        private String entidadBancaria;
        // Tarjeta
        private String numeroTarjeta;
        private String nombreTitularTarjeta;
        private String fechaVencimientoTarjeta;
        private String cvv;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PaymentResult {
        private boolean success;
        private String numeroAprobacion;
        private String metodo;
        private String estado;                 // "APROBADO" | "PENDIENTE_BANCO" | "RECHAZADO"
        private String mensaje;

        public static PaymentResult success(String numero, String metodo) {
            return PaymentResult.builder()
                .success(true).numeroAprobacion(numero)
                .metodo(metodo).estado("APROBADO")
                .mensaje("Pago procesado correctamente").build();
        }

        public static PaymentResult failure(String mensaje) {
            return PaymentResult.builder()
                .success(false).estado("RECHAZADO").mensaje(mensaje).build();
        }
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PaymentResponse {
        private String paymentId;
        private String entityId;
        private String entityType;
        private Double monto;
        private String metodo;
        private String estado;
        private String numeroAprobacion;
        private String fecha;
    }
}
