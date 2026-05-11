package com.konrad.paymentservice.service;


import com.konrad.paymentservice.dto.PaymentDtos.*;
import com.konrad.paymentservice.service.PaymentStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

// =============================================================================
// PATRÓN STRATEGY — Familia de estrategias de pago
//
// Interfaz + 3 implementaciones concretas.
// El PaymentService (contexto) las usa indistintamente vía la interfaz.
// Agregar un nuevo método = nueva clase, cero cambios al contexto.
// =============================================================================


// ─── Estrategia 1: PSE ────────────────────────────────────────────────────────

/**
 * STRATEGY CONCRETA: Pago en línea PSE.
 * "consumirá un servicio de un intermediario que permitirá ingresar a la
 * página del banco realizar el pago y retornar el número de aprobación"
 */
@Slf4j
@Component
class PSEPaymentStrategy implements PaymentStrategy {

    @Override
    public PaymentResult pay(PaymentRequest request) {
        log.info("[STRATEGY-PSE] Iniciando pago PSE — banco: {}, monto: {}",
                request.getEntidadBancaria(), request.getMonto());

        // MOCK: simula la redirección al banco y el retorno del número de aprobación
        if (request.getEntidadBancaria() == null || request.getEntidadBancaria().isBlank()) {
            return PaymentResult.failure("Entidad bancaria requerida para PSE");
        }

        // Mock intermediario bancario: siempre aprueba con un número aleatorio
        String approvalNumber = "PSE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("[STRATEGY-PSE] Aprobado: {}", approvalNumber);
        return PaymentResult.success(approvalNumber, "PSE");
    }

    @Override
    public String getMethodName() { return "PSE"; }
}


// ─── Estrategia 2: Tarjeta de crédito ────────────────────────────────────────


// ─── Estrategia 3: Consignación bancaria ─────────────────────────────────────

