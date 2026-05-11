package com.konrad.creditcheckservice.service;

import com.konrad.creditcheckservice.dto.CreditCheckDtos.*;
import com.konrad.creditcheckservice.service.CreditCheckStrategy;
import com.konrad.creditcheckservice.service.CreditScore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * PATRÓN FACADE — VendorVerificationFacade.
 *
 * El Director Comercial necesita hacer 3 consultas a sistemas distintos:
 *   1. Datacrédito  → REST API (CreditCheckStrategy #1)
 *   2. CIFIN        → BD local de archivo plano (CreditCheckStrategy #2)
 *   3. Policía Nac. → Página web / scraping (JudicialCheckService)
 *
 * Sin Facade: el controller mezclaría 3 APIs distintas + lógica de decisión.
 * Con Facade: una sola llamada verify() → resultado consolidado con decisión.
 *
 * Requerimiento punto 2 del documento.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VendorVerificationFacade {

    private final CreditCheckStrategy datacreditoStrategy; // STRATEGY #1
    private final CreditCheckStrategy cifinStrategy;       // STRATEGY #2
    private final JudicialCheckService judicialCheckService;

    /**
     * FACADE: Una sola llamada consolida las 3 verificaciones.
     * El Director llama verify() — no necesita saber nada de las 3 fuentes.
     */
    public VerificationResult verify(String identificacion) {
        log.info("[FACADE] Iniciando verificación completa para: {}", identificacion);

        // 1. Verificar con Datacrédito (STRATEGY — REST)
        CreditScore datacreditoScore = datacreditoStrategy.check(identificacion);

        // 2. Verificar con CIFIN (STRATEGY — BD local)
        CreditScore cifinScore = cifinStrategy.check(identificacion);

        // 3. Verificar antecedentes judiciales (Policía Nacional)
        JudicialStatus judicialStatus = judicialCheckService.check(identificacion);

        // 4. FACADE aplica las reglas de negocio del documento para la decisión
        ApplicationDecision decision = determineDecision(datacreditoScore, cifinScore, judicialStatus);
        List<String> motivos = buildMotivos(datacreditoScore, cifinScore, judicialStatus);

        log.info("[FACADE] Verificación completa — Decisión: {} | DC: {} | CIFIN: {} | Judicial: {}",
            decision, datacreditoScore, cifinScore, judicialStatus);

        return VerificationResult.builder()
            .identificacion(identificacion)
            .datacreditoScore(datacreditoScore.name())
            .cifinScore(cifinScore.name())
            .judicialStatus(judicialStatus.name())
            .decision(decision.name())
            .motivos(motivos)
            .recomendacion(buildRecomendacion(decision))
            .build();
    }

    /**
     * Reglas de negocio del documento (punto 2):
     *  RECHAZADA: crediticia BAJA en alguna entidad O requerido judicialmente
     *  DEVUELTA:  crediticia en ADVERTENCIA (en cualquiera de las dos)
     *  APROBADA:  crediticia ALTA en ambas entidades Y no requerido
     */
    private ApplicationDecision determineDecision(CreditScore dc, CreditScore cifin,
                                                   JudicialStatus judicial) {
        if (judicial == JudicialStatus.REQUERIDO) return ApplicationDecision.RECHAZADA;
        if (dc == CreditScore.BAJA || cifin == CreditScore.BAJA) return ApplicationDecision.RECHAZADA;
        if (dc == CreditScore.ADVERTENCIA || cifin == CreditScore.ADVERTENCIA) return ApplicationDecision.DEVUELTA;
        return ApplicationDecision.APROBADA;
    }

    private List<String> buildMotivos(CreditScore dc, CreditScore cifin, JudicialStatus j) {
        List<String> motivos = new ArrayList<>();
        if (dc == CreditScore.BAJA) motivos.add("BAJA_DATACREDITO");
        if (dc == CreditScore.ADVERTENCIA) motivos.add("ADVERTENCIA_DATACREDITO");
        if (cifin == CreditScore.BAJA) motivos.add("BAJA_CIFIN");
        if (cifin == CreditScore.ADVERTENCIA) motivos.add("ADVERTENCIA_CIFIN");
        if (j == JudicialStatus.REQUERIDO) motivos.add("REQUERIDO_JUDICIAL");
        return motivos;
    }

    private String buildRecomendacion(ApplicationDecision decision) {
        return switch (decision) {
            case APROBADA  -> "Proceder con la aprobación y envío de credenciales";
            case DEVUELTA  -> "Notificar al solicitante que puede reactivar al regularizar crédito";
            case RECHAZADA -> "Notificar rechazo con motivos específicos";
        };
    }

    public enum ApplicationDecision { APROBADA, RECHAZADA, DEVUELTA }
    public enum JudicialStatus { REQUERIDO, NO_REQUERIDO }
}
