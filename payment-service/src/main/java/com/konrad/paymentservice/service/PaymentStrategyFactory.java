package com.konrad.paymentservice.service;

import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * STRATEGY FACTORY — resuelve la estrategia concreta según el método de pago.
 *
 * Spring inyecta automáticamente todas las implementaciones de PaymentStrategy
 * en un Map<nombre, instancia> gracias a la autowiring por tipo.
 * Agregar nueva estrategia = nueva @Component, sin tocar este factory.
 */
@Component
public class PaymentStrategyFactory {

    // Spring inyecta: { "PSEPaymentStrategy" → bean, "CreditCardPaymentStrategy" → bean, ... }
    private final Map<String, PaymentStrategy> strategies;

    public PaymentStrategyFactory(Map<String, PaymentStrategy> strategies) {
        this.strategies = strategies;
    }

    public PaymentStrategy resolve(String method) {
        return switch (method.toUpperCase()) {
            case "PSE"          -> getStrategy("PSEPaymentStrategy");
            case "CREDIT_CARD"  -> getStrategy("creditCardPaymentStrategy");
            case "CONSIGNATION" -> getStrategy("consignationPaymentStrategy");
            default -> throw new IllegalArgumentException("Método de pago no soportado: " + method);
        };
    }

    private PaymentStrategy getStrategy(String beanName) {
        PaymentStrategy s = strategies.get(beanName);
        if (s == null) throw new IllegalStateException("Estrategia no encontrada: " + beanName);
        return s;
    }
}
