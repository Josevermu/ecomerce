package com.konrad.creditcheckservice.service;

// =============================================================================
// PATRÓN STRATEGY — Verificación crediticia (clases públicas para inyección)
// =============================================================================

/** Contrato común para ambas fuentes crediticias. */
public interface CreditCheckStrategy {
    CreditScore check(String identificacion);
    String getSourceName();
}
