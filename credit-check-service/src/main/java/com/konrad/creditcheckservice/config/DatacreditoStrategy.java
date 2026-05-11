package com.konrad.creditcheckservice.config;

import com.konrad.creditcheckservice.service.CreditCheckStrategy;
import com.konrad.creditcheckservice.service.CreditScore;
import lombok.extern.slf4j.Slf4j;

/**
 * PATRÓN STRATEGY — Consulta Datacrédito vía REST.
 *
 * En producción: llama al endpoint real de Datacrédito usando WebClient
 * con la URL configurada en konrad.datacredito.api-url.
 *
 * MOCK: simula la respuesta según el último dígito del número de identificación,
 * cubriendo los 3 escenarios posibles (ALTA / ADVERTENCIA / BAJA) para pruebas.
 *
 * Lógica mock:
 *   último dígito 0-3  → BAJA       (reportado en mora)
 *   último dígito 4-6  → ADVERTENCIA (días en mora)
 *   último dígito 7-9  → ALTA       (al día)
 */
@Slf4j
public class DatacreditoStrategy implements CreditCheckStrategy {

    @Override
    public CreditScore check(String identificacion) {
        log.info("[STRATEGY-DC] Consultando Datacrédito (mock) para: {}", identificacion);

        char lastChar = identificacion.charAt(identificacion.length() - 1);
        int lastDigit = Character.getNumericValue(lastChar);

        CreditScore score;
        if (lastDigit <= 3) {
            score = CreditScore.BAJA;
        } else if (lastDigit <= 6) {
            score = CreditScore.ADVERTENCIA;
        } else {
            score = CreditScore.ALTA;
        }

        log.info("[STRATEGY-DC] Resultado Datacrédito para {}: {}", identificacion, score);
        return score;
    }

    @Override
    public String getSourceName() {
        return "DATACREDITO";
    }
}