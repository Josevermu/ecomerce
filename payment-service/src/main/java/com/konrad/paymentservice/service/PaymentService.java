package com.konrad.paymentservice.service;

import com.konrad.paymentservice.dto.PaymentDtos.*;
import com.konrad.paymentservice.model.entity.Payment;
import com.konrad.paymentservice.model.entity.PaymentRepository;
import com.konrad.paymentservice.service.PaymentStrategy;
import com.konrad.paymentservice.service.PaymentStrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PATRÓN STRATEGY — Contexto.
 *
 * PaymentService NO contiene lógica de ningún método de pago.
 * Delega todo a la estrategia concreta que resuelve PaymentStrategyFactory.
 * El mismo método process() sirve para PSE, tarjeta o consignación.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentStrategyFactory strategyFactory;
    private final PaymentRepository paymentRepo;
    private final ApplicationEventPublisher eventPublisher; // OBSERVER: publica pago confirmado

    /**
     * Ejecuta el pago usando la estrategia correcta.
     * El caller solo dice qué método quiere — el contexto se encarga del resto.
     */
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("[PAYMENT] Procesando pago — método: {}, entidad: {}, monto: {}",
            request.getMetodoPago(), request.getEntityId(), request.getMonto());

        // STRATEGY: resolución de la estrategia según el método elegido
        PaymentStrategy strategy = strategyFactory.resolve(request.getMetodoPago());

        // Delegar completamente la lógica de pago a la estrategia
        PaymentResult result = strategy.pay(request);

        // Persistir resultado
        Payment payment = Payment.builder()
            .entityId(request.getEntityId())
            .entityType(request.getEntityType())
            .monto(request.getMonto())
            .metodo(strategy.getMethodName())
            .estado(result.getEstado())
            .numeroAprobacion(result.getNumeroAprobacion())
            .fecha(LocalDateTime.now())
            .build();
        Payment saved = paymentRepo.save(payment);

        // OBSERVER: si el pago fue aprobado, publicar evento para que seller/order-service reaccionen
        if (result.isSuccess() && "APROBADO".equals(result.getEstado())) {
            log.info("[PAYMENT] Pago confirmado — publicando evento para activación");
            eventPublisher.publishEvent(new PaymentConfirmedEvent(
                this, saved.getId(), request.getEntityId(),
                request.getEntityType(), request.getMetodoPago()
            ));
        }

        return toResponse(saved);
    }

    public List<PaymentResponse> findByEntity(String entityId) {
        return paymentRepo.findByEntityId(entityId).stream()
            .map(this::toResponse).collect(Collectors.toList());
    }

    public List<PaymentResponse> findAll() {
        return paymentRepo.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    private PaymentResponse toResponse(Payment p) {
        return PaymentResponse.builder()
            .paymentId(p.getId()).entityId(p.getEntityId()).entityType(p.getEntityType())
            .monto(p.getMonto()).metodo(p.getMetodo()).estado(p.getEstado())
            .numeroAprobacion(p.getNumeroAprobacion())
            .fecha(p.getFecha() != null ? p.getFecha().toString() : "")
            .build();
    }

    // Evento interno para comunicar pago confirmado
    public static class PaymentConfirmedEvent extends org.springframework.context.ApplicationEvent {
        private final String paymentId, entityId, entityType, method;
        public PaymentConfirmedEvent(Object source, String paymentId, String entityId,
                                     String entityType, String method) {
            super(source);
            this.paymentId = paymentId; this.entityId = entityId;
            this.entityType = entityType; this.method = method;
        }
        public String getPaymentId() { return paymentId; }
        public String getEntityId() { return entityId; }
        public String getEntityType() { return entityType; }
        public String getMethod() { return method; }
    }
}
