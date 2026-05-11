package com.konrad.creditcheckservice.config;

import com.konrad.creditcheckservice.model.entity.CIFINRepository;
import com.konrad.creditcheckservice.model.entity.CreditRecord;
import com.konrad.creditcheckservice.service.CreditCheckStrategy;
import com.konrad.creditcheckservice.service.CreditScore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PATRÓN STRATEGY — Consulta CIFIN desde la BD local.
 *
 * En producción: un job nocturno (CIFINSyncJob) descarga el archivo plano
 * del FTP mensual de CIFIN y lo carga en la tabla cifin_records.
 * Esta estrategia consulta esa tabla local — nunca hace FTP en tiempo real.
 *
 * MOCK: la BD local ya está precargada en {@link CIFINRepository#loadMockData()}
 * con varios IDs en distintos estados para cubrir todos los escenarios.
 *
 * Si el identificador NO aparece en CIFIN (persona sin historial),
 * se devuelve ALTA como valor por defecto (sin reporte negativo = sin mora).
 */
@Slf4j
@RequiredArgsConstructor
public class CIFINStrategy implements CreditCheckStrategy {

    private final CIFINRepository cifinRepository;

    @Override
    public CreditScore check(String identificacion) {
        log.info("[STRATEGY-CIFIN] Consultando BD local CIFIN para: {}", identificacion);

        CreditScore score = cifinRepository
                .findByIdentificacion(identificacion)
                .map(CreditRecord::getEstado)
                .map(estado -> {
                    try {
                        return CreditScore.valueOf(estado);
                    } catch (IllegalArgumentException e) {
                        log.warn("[STRATEGY-CIFIN] Estado desconocido '{}' para {}, asumiendo ALTA",
                                estado, identificacion);
                        return CreditScore.ALTA;
                    }
                })
                .orElseGet(() -> {
                    // No figura en la base de CIFIN → no tiene reporte negativo
                    log.info("[STRATEGY-CIFIN] {} no encontrado en CIFIN — sin reporte → ALTA",
                            identificacion);
                    return CreditScore.ALTA;
                });

        log.info("[STRATEGY-CIFIN] Resultado CIFIN para {}: {}", identificacion, score);
        return score;
    }

    @Override
    public String getSourceName() {
        return "CIFIN";
    }
}