package com.konrad.sellerservice.dto;

import lombok.*;
import java.util.List;

public class SellerDtos {

    /** Solicitud de registro de nuevo vendedor (punto 1 del documento) */
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class SellerRegistrationRequest {
        private String nombres;
        private String apellidos;
        private String identificacion;
        private String tipoPersona;      // "NATURAL" | "JURIDICA"
        private String correo;
        private String pais;
        private String ciudad;
        private String telefono;
        // Documentos adjuntos — mock: solo nombres de archivo
        private List<String> documentos; // fotocopyCedula, rut, camaraComercio, etc.
    }

    /** Respuesta después de enviar la solicitud */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ApplicationSubmittedResponse {
        private String applicationId;
        private String status;
        private String mensaje;
    }

    /** Resultado que registra el Director Comercial (punto 2) */
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ApplicationDecisionRequest {
        private String decision;         // "APROBADA" | "RECHAZADA" | "DEVUELTA"
        private String motivo;           // requerido si RECHAZADA o DEVUELTA
        private String directorId;
    }

    /** Vista de solicitud para listado del Director */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ApplicationSummaryResponse {
        private String id;
        private String identificacion;
        private String apellidos;
        private String nombres;
        private String correo;
        private String status;
        private String fechaSolicitud;
    }

    /** Detalle completo de una solicitud */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ApplicationDetailResponse {
        private String id;
        private String nombres;
        private String apellidos;
        private String identificacion;
        private String tipoPersona;
        private String correo;
        private String pais;
        private String ciudad;
        private String telefono;
        private List<String> documentos;
        private String status;
        private String motivoRechazo;
        private String fechaSolicitud;
        private String fechaDecision;
    }

    /** Solicitud de activación de suscripción (punto 3) */
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class SubscriptionRequest {
        private String tipo;             // "MENSUAL" | "SEMESTRAL" | "ANUAL"
        private String paymentId;        // ID del pago confirmado
    }
}
