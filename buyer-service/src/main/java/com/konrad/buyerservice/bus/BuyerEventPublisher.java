package com.konrad.buyerservice.bus;

import com.azure.messaging.servicebus.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class BuyerEventPublisher {

    private static final String TOPIC = "konrad-buyer-events";

    @Value("${azure.servicebus.connection-string:mock}")
    private String connectionString;

    private ServiceBusSenderAsyncClient sender;
    private final ObjectMapper mapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        if (isMock()) {
            log.warn("[BUYER-BUS] Modo mock — mensajes NO se enviarán al Service Bus");
            return;
        }
        sender = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .sender().topicName(TOPIC).buildAsyncClient();
        log.info("[BUYER-BUS] Publisher conectado al topic '{}'", TOPIC);
    }

    @PreDestroy
    public void close() { if (sender != null) sender.close(); }

    public void publishRegistered(String buyerId, String correo, String nombre) {
        send(BusEventDto.builder()
                .eventType("BUYER_REGISTERED")
                .entityId(buyerId).correo(correo).nombre(nombre)
                .timestamp(LocalDateTime.now().toString()).build());
    }

    private void send(BusEventDto event) {
        if (isMock()) {
            log.info("[BUYER-BUS][MOCK] Evento: {}", event.getEventType());
            return;
        }
        try {
            sender.sendMessage(new ServiceBusMessage(mapper.writeValueAsString(event))
                            .setContentType("application/json")
                            .setSubject(event.getEventType()))
                    .doOnSuccess(v -> log.info("[BUYER-BUS] Publicado: {}", event.getEventType()))
                    .doOnError(e -> log.error("[BUYER-BUS] Error: {}", e.getMessage()))
                    .subscribe();
        } catch (Exception e) {
            log.error("[BUYER-BUS] Serialización fallida: {}", e.getMessage());
        }
    }

    private boolean isMock() {
        return connectionString == null || connectionString.equals("mock") || connectionString.isBlank();
    }

    // ── DTO local (igual en todos los servicios) ──────────────────────────────
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BusEventDto {
        private String eventType, timestamp, entityId, correo, nombre;
    }
}