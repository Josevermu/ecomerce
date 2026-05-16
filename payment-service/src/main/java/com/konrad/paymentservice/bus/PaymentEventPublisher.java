package com.konrad.paymentservice.bus;

import com.azure.messaging.servicebus.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Publica eventos de pago en el topic "konrad-payment-events".
 *
 * Consumidores:
 *   - seller-service / seller-sub  → activa suscripción en PAYMENT_CONFIRMED (SUBSCRIPTION)
 *   - notification-service / notification-sub → envía correo de confirmación
 */
@Slf4j
@Component
public class PaymentEventPublisher {

    private static final String TOPIC = "konrad-payment-events";

    @Value("${azure.servicebus.connection-string:mock}")
    private String connectionString;

    private ServiceBusSenderAsyncClient sender;
    private final ObjectMapper mapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        if (isMock()) {
            log.warn("[PAYMENT-BUS] Modo mock — mensajes NO se enviarán al Service Bus");
            return;
        }
        sender = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .sender().topicName(TOPIC).buildAsyncClient();
        log.info("[PAYMENT-BUS] Publisher conectado al topic '{}'", TOPIC);
    }

    @PreDestroy
    public void close() { if (sender != null) sender.close(); }

    /**
     * @param paymentId    ID del pago generado
     * @param entityId     ID de la orden o suscripción
     * @param entityType   "ORDER" | "SUBSCRIPTION"
     * @param sellerId     ID del vendedor (solo cuando entityType=SUBSCRIPTION)
     * @param correo       correo del pagador para notificación
     * @param monto        monto aprobado
     */
    public void publishConfirmed(String paymentId, String entityId, String entityType,
                                 String sellerId, String correo, Double monto) {
        send(BusEventDto.builder()
                .eventType("PAYMENT_CONFIRMED")
                .paymentId(paymentId)
                .entityId(entityId)
                .entityType(entityType)
                .sellerId(sellerId)
                .correo(correo)
                .monto(monto)
                .timestamp(LocalDateTime.now().toString())
                .build());
    }

    public void publishRejected(String entityId, String correo, String motivo) {
        send(BusEventDto.builder()
                .eventType("PAYMENT_REJECTED")
                .entityId(entityId).correo(correo).motivo(motivo)
                .timestamp(LocalDateTime.now().toString())
                .build());
    }

    private void send(BusEventDto event) {
        if (isMock()) {
            log.info("[PAYMENT-BUS][MOCK] Evento: {}", event.getEventType());
            return;
        }
        try {
            sender.sendMessage(new ServiceBusMessage(mapper.writeValueAsString(event))
                            .setContentType("application/json")
                            .setSubject(event.getEventType()))
                    .doOnSuccess(v -> log.info("[PAYMENT-BUS] Publicado: {}", event.getEventType()))
                    .doOnError(e -> log.error("[PAYMENT-BUS] Error: {}", e.getMessage()))
                    .subscribe();
        } catch (Exception e) {
            log.error("[PAYMENT-BUS] Serialización fallida: {}", e.getMessage());
        }
    }

    private boolean isMock() {
        return connectionString == null || connectionString.equals("mock") || connectionString.isBlank();
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BusEventDto {
        private String eventType, timestamp, paymentId, entityId, entityType;
        private String sellerId, correo, motivo;
        private Double monto;
    }
}