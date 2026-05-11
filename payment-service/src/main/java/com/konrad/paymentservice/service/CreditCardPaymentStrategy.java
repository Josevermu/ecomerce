package com.konrad.paymentservice.service;

import com.konrad.paymentservice.dto.PaymentDtos;
import com.konrad.paymentservice.dto.PaymentDtos.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * STRATEGY CONCRETA: Tarjeta de crédito.
 * "mostrará los campos para registrar la información de la tarjeta
 * y realizar el pago"
 */
@Slf4j
@Component
class CreditCardPaymentStrategy implements PaymentStrategy {

    @Override
    public PaymentResult pay(PaymentRequest request) {
        log.info("[STRATEGY-CARD] Procesando tarjeta terminada en: {}",
                maskCard(request.getNumeroTarjeta()));

        if (!isValidCard(request)) {
            return PaymentResult.failure("Datos de tarjeta inválidos o incompletos");
        }

        // Mock gateway de pagos: rechaza tarjetas que terminan en 0000
        if (request.getNumeroTarjeta() != null && request.getNumeroTarjeta().endsWith("0000")) {
            return PaymentResult.failure("Tarjeta declinada por el banco emisor");
        }

        String transactionId = "CC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("[STRATEGY-CARD] Transacción aprobada: {}", transactionId);
        return PaymentResult.success(transactionId, "CREDIT_CARD");
    }

    private boolean isValidCard(PaymentRequest r) {
        return r.getNumeroTarjeta() != null && r.getNumeroTarjeta().length() >= 13
                && r.getCvv() != null && r.getFechaVencimientoTarjeta() != null
                && r.getNombreTitularTarjeta() != null;
    }

    private String maskCard(String card) {
        if (card == null || card.length() < 4) return "****";
        return "*".repeat(card.length() - 4) + card.substring(card.length() - 4);
    }

    @Override
    public String getMethodName() {
        return "CREDIT_CARD";
    }
}
