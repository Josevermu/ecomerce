package com.konrad.paymentservice.service;

import com.konrad.paymentservice.bus.PaymentEventPublisher;
import com.konrad.paymentservice.dto.PaymentDtos.*;
import com.konrad.paymentservice.model.entity.Payment;
import com.konrad.paymentservice.model.entity.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentStrategyFactory strategyFactory;
    private final PaymentRepository paymentRepo;
    private final PaymentEventPublisher eventPublisher; // ← Service Bus publisher

    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("[PAYMENT] Procesando — método: {}, entidad: {}, monto: {}",
                request.getMetodoPago(), request.getEntityId(), request.getMonto());

        PaymentStrategy strategy = strategyFactory.resolve(request.getMetodoPago());
        PaymentResult result     = strategy.pay(request);

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

        // ── Publicar en Service Bus ───────────────────────────────────────────
        if (result.isSuccess() && "APROBADO".equals(result.getEstado())) {
            log.info("[PAYMENT] Pago aprobado — publicando en bus");
            eventPublisher.publishConfirmed(
                    saved.getId(),
                    request.getEntityId(),
                    request.getEntityType(),
                    request.getEntityId(),            // sellerId cuando SUBSCRIPTION
                    request.getPagadorIdentificacion(),
                    request.getMonto()
            );
        } else if (!result.isSuccess()) {
            eventPublisher.publishRejected(
                    request.getEntityId(),
                    request.getPagadorIdentificacion(),
                    result.getMensaje()
            );
        }
        // PENDIENTE_BANCO (consignación) no se publica hasta que el job nocturno confirme

        return toResponse(saved);
    }

    public List<PaymentResponse> findByEntity(String entityId) {
        return paymentRepo.findByEntityId(entityId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    public List<PaymentResponse> findAll() {
        return paymentRepo.findAll().stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    private PaymentResponse toResponse(Payment p) {
        return PaymentResponse.builder()
                .paymentId(p.getId()).entityId(p.getEntityId()).entityType(p.getEntityType())
                .monto(p.getMonto()).metodo(p.getMetodo()).estado(p.getEstado())
                .numeroAprobacion(p.getNumeroAprobacion())
                .fecha(p.getFecha() != null ? p.getFecha().toString() : "")
                .build();
    }
}