package com.konrad.sellerservice.bus;

import com.azure.messaging.servicebus.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Publica eventos de dominio del seller-service en el topic
 * "konrad-seller-events" de Azure Service Bus.
 *
 * Todos los cambios de estado del ciclo de vida del vendedor se publican aquí.
 * notification-service consume el topic para enviar correos certificados.
 *
 * Si la connection string es "mock" o está vacía, el publisher
 * no envía nada (modo desarrollo local sin Azure).
 */
@Slf4j
@Component
public class SellerEventPublisher {

    private static final String TOPIC = "konrad-seller-events";

    @Value("${azure.servicebus.connection-string:mock}")
    private String connectionString;

    private ServiceBusSenderAsyncClient sender;
    private final ObjectMapper mapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        if (isMock()) {
            log.warn("[SELLER-BUS] connection-string=mock — mensajes NO se enviarán al Service Bus");
            return;
        }
        sender = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .sender()
                .topicName(TOPIC)
                .buildAsyncClient();
        log.info("[SELLER-BUS] Publisher conectado al topic '{}'", TOPIC);
    }

    @PreDestroy
    public void close() {
        if (sender != null) sender.close();
    }

    // ── Métodos de publicación ────────────────────────────────────────────────

    public void publishSubmitted(String applicationId, String correo, String nombre) {
        send(BusEventDto.builder()
                .eventType("SELLER_SUBMITTED")
                .entityId(applicationId).correo(correo).nombre(nombre)
                .timestamp(now()).build());
    }

    public void publishApproved(String applicationId, String correo, String nombre) {
        send(BusEventDto.builder()
                .eventType("SELLER_APPROVED")
                .entityId(applicationId).correo(correo).nombre(nombre)
                .timestamp(now()).build());
    }

    public void publishRejected(String applicationId, String correo, String motivo) {
        send(BusEventDto.builder()
                .eventType("SELLER_REJECTED")
                .entityId(applicationId).correo(correo).motivo(motivo)
                .timestamp(now()).build());
    }

    public void publishReturned(String applicationId, String correo) {
        send(BusEventDto.builder()
                .eventType("SELLER_RETURNED")
                .entityId(applicationId).correo(correo)
                .timestamp(now()).build());
    }

    public void publishSubscriptionExpired(String sellerId, String correo) {
        send(BusEventDto.builder()
                .eventType("SUBSCRIPTION_EXPIRED")
                .sellerId(sellerId).correo(correo)
                .timestamp(now()).build());
    }

    public void publishSellerSuspended(String sellerId, String correo, String motivo) {
        send(BusEventDto.builder()
                .eventType("SELLER_SUSPENDED")
                .sellerId(sellerId).correo(correo).motivo(motivo)
                .timestamp(now()).build());
    }

    // ── Interno ───────────────────────────────────────────────────────────────

    private void send(BusEventDto event) {
        if (isMock()) {
            log.info("[SELLER-BUS][MOCK] Evento que se enviaría: {}", event.getEventType());
            return;
        }
        try {
            String json = mapper.writeValueAsString(event);
            sender.sendMessage(new ServiceBusMessage(json)
                            .setContentType("application/json")
                            .setSubject(event.getEventType()))
                    .doOnSuccess(v -> log.info("[SELLER-BUS] Publicado: {}", event.getEventType()))
                    .doOnError(e -> log.error("[SELLER-BUS] Error publicando {}: {}", event.getEventType(), e.getMessage()))
                    .subscribe();
        } catch (Exception e) {
            log.error("[SELLER-BUS] Serialización fallida: {}", e.getMessage());
        }
    }

    private boolean isMock() {
        return connectionString == null || connectionString.equals("mock") || connectionString.isBlank();
    }

    private String now() { return LocalDateTime.now().toString(); }
}