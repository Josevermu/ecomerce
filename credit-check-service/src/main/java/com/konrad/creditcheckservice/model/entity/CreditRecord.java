package com.konrad.creditcheckservice.model.entity;

import lombok.*;

/** Registro cargado del archivo plano mensual de CIFIN */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreditRecord {
    private String identificacion;
    private String estado;   // "ALTA" | "ADVERTENCIA" | "BAJA"
    private String fuente;   // "CIFIN"
}
