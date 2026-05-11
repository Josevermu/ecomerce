package com.konrad.buyerservice.dto;

import lombok.*;

public class BuyerDtos {

    /** Registro de nuevo comprador — punto 7 */
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class BuyerRegistrationRequest {
        private String nombres;
        private String apellidos;
        private String identificacion;
        private String correo;
        private String pais;
        private String ciudad;
        private String direccion;
        private String telefono;
        private String twitter;    // opcional
        private String instagram;  // opcional
    }

    /** Respuesta tras el registro exitoso */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BuyerRegistrationResponse {
        private String buyerId;
        private String correo;
        private String mensaje;
    }

    /** Vista del perfil del comprador */
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BuyerProfileResponse {
        private String id;
        private String nombres;
        private String apellidos;
        private String identificacion;
        private String correo;
        private String pais;
        private String ciudad;
        private String direccion;
        private String telefono;
        private String twitter;
        private String instagram;
        private String creadoEn;
    }

    /** Actualización parcial del perfil */
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class BuyerUpdateRequest {
        private String ciudad;
        private String direccion;
        private String telefono;
        private String twitter;
        private String instagram;
    }
}