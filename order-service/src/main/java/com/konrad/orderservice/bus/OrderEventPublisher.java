package com.konrad.orderservice.bus;

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
 * Publica eventos de orden en el topic "konrad-order-events".
 *
 * Consumidores:
 *   - seller-service / seller-sub → updateRating() cuando ORDER_RATED
 *   - bam-service    / bam-sub   → registra auditoría y KPIs
 */
@Slf4j
@Component
public class OrderEventPublisher {

    private static final String TOPIC = "konrad-order-events";

    @Value("${azure.servicebus.connection-string:mock}")
    private String connectionString;

    private ServiceBusSenderAsyncClient sender;
    private final ObjectMapper mapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        if (isMock()) {
            log.warn("[ORDER-BUS] Modo mock — mensajes NO se enviarán al Service Bus");
            return;
        }
        sender = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .sender().topicName(TOPIC).buildAsyncClient();
        log.info("[ORDER-BUS] Publisher conectado al topic '{}'", TOPIC);
    }

    @PreDestroy
    public void close() { if (sender != null) sender.close(); }

    public void publishOrderCreated(String orderId, String sellerId,
                                    String buyerId, Double total) {
        send(BusEventDto.builder()
                .eventType("ORDER_CREATED")
                .orderId(orderId).sellerId(sellerId).buyerId(buyerId).total(total)
                .timestamp(LocalDateTime.now().toString()).build());
    }

    public void publishOrderRated(String orderId, String sellerId,
                                  int calificacion, String comentario) {
        send(BusEventDto.builder()
                .eventType("ORDER_RATED")
                .orderId(orderId).sellerId(sellerId)
                .calificacion(calificacion).comentario(comentario)
                .timestamp(LocalDateTime.now().toString()).build());
    }

    private void send(BusEventDto event) {
        if (isMock()) {
            log.info("[ORDER-BUS][MOCK] Evento: {}", event.getEventType());
            return;
        }
        try {
            sender.sendMessage(new ServiceBusMessage(mapper.writeValueAsString(event))
                            .setContentType("application/json")
                            .setSubject(event.getEventType()))
                    .doOnSuccess(v -> log.info("[ORDER-BUS] Publicado: {}", event.getEventType()))
                    .doOnError(e -> log.error("[ORDER-BUS] Error: {}", e.getMessage()))
                    .subscribe();
        } catch (Exception e) {
            log.error("[ORDER-BUS] Serialización fallida: {}", e.getMessage());
        }
    }

    private boolean isMock() {
        return connectionString == null || connectionString.equals("mock") || connectionString.isBlank();
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BusEventDto {
        private String eventType, timestamp, orderId, sellerId, buyerId, comentario;
        private Integer calificacion;
        private Double total;
    }
}