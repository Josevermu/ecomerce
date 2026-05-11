package com.konrad.sellerservice.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entidad de dominio — Solicitud de vendedor.
 *
 * CORRECCIÓN: el campo se llama "documentos" (consistente con el builder
 * usado en SellerApplicationRepository y los DTOs de SellerDtos).
 * El nombre anterior "documentosAdjuntos" causaba NullPointerException
 * en el mapper toDetail() al llamar a.getDocumentos().
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerApplication {

    private String id;
    private String nombres;
    private String apellidos;
    private String identificacion;          // Cédula o NIT
    private TipoPersona tipoPersona;        // NATURAL | JURIDICA
    private String correo;
    private String pais;
    private String ciudad;
    private String telefono;
    private List<String> documentos;        // rutas de archivos mock (era documentosAdjuntos)
    private ApplicationStatus status;
    private String motivoRechazo;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaDecision;
    private String decididoPor;             // userId del Director Comercial

    // Estadísticas de calificación (punto 6)
    private int totalCalificaciones;
    private int calificacionesBajas;        // calificaciones < 3
    private double promedioCalificacion;

    public enum TipoPersona { NATURAL, JURIDICA }

    public enum ApplicationStatus {
        PENDIENTE, APROBADA, RECHAZADA, DEVUELTA, ACTIVA, EN_MORA, CANCELADA
    }
}