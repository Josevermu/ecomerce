package com.konrad.creditcheckservice.service;

/**
 * ENUM — Representa el nivel de score crediticio
 * usado por Datacrédito y CIFIN.
 *
 * Reglas:
 * - ALTA         → Cliente confiable
 * - ADVERTENCIA  → Cliente con alertas o riesgo medio
 * - BAJA         → Cliente con alto riesgo financiero
 */
public enum CreditScore {

    /**
     * Riesgo bajo.
     * Cliente apto para aprobación.
     */
    ALTA,

    /**
     * Riesgo medio.
     * Requiere revisión o regularización.
     */
    ADVERTENCIA,

    /**
     * Riesgo alto.
     * Solicitud debe ser rechazada.
     */
    BAJA
}