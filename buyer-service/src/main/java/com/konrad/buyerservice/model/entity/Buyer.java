package com.konrad.buyerservice.model.entity;

import lombok.*;
import java.time.LocalDateTime;

/**
 * Entidad Comprador — punto 7 del documento.
 * Los campos twitter e instagram son opcionales; sirven como
 * señales para el motor de tendencias del punto 14.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Buyer {

    private String id;
    private String nombres;
    private String apellidos;
    private String identificacion;   // Cédula o NIT
    private String correo;
    private String pais;
    private String ciudad;
    private String direccion;
    private String telefono;
    private String twitter;          // opcional — usado para tendencias (punto 14)
    private String instagram;        // opcional — usado para tendencias (punto 14)
    private boolean activo;
    private LocalDateTime creadoEn;
}