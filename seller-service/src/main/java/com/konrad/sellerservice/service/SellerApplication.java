package com.konrad.sellerservice.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerApplication {
    private String id;
    private String nombres;
    private String apellidos;
    private String identificacion;       // Cédula o NIT
    private SellerApplication.TipoPersona tipoPersona;     // NATURAL | JURIDICA
    private String correo;
    private String pais;
    private String ciudad;
    private String telefono;
    private List<String> documentosAdjuntos; // rutas de archivos mock
    private SellerApplication.ApplicationStatus status;
    private String motivoRechazo;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaDecision;
    private String decididoPor;          // userId del Director Comercial
    // Estadísticas de calificación (punto 6)
    private int totalCalificaciones;
    private int calificacionesBajas;     // calificaciones < 3
    private double promedioCalificacion;

    public enum TipoPersona {NATURAL, JURIDICA}

    public enum ApplicationStatus {
        PENDIENTE, APROBADA, RECHAZADA, DEVUELTA, ACTIVA, EN_MORA, CANCELADA
    }
}
