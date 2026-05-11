package com.konrad.paymentservice.service;

import com.konrad.paymentservice.dto.PaymentDtos;

/**
 * STRATEGY: Contrato común para todos los métodos de pago.
 * Requerimiento puntos 3 (suscripción) y 11 (compra de productos).
 */
public interface PaymentStrategy {
    PaymentDtos.PaymentResult pay(PaymentDtos.PaymentRequest request);

    String getMethodName();
}
