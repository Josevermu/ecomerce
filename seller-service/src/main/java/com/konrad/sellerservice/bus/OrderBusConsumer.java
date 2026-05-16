package com.konrad.sellerservice.bus;

import com.azure.messaging.servicebus.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.konrad.sellerservice.service.SellerApplicationService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Consume mensajes del topic "konrad-order-events", suscripción "seller-sub".
 *
 * Cuando order-service registra una calificación (ORDER_RATED), actualiza
 * las estadísticas del vendedor y evalúa si debe ser suspendido (punto 6).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderBusConsumer {

    private static final String TOPIC        = "konrad-order-events";
    private static final String SUBSCRIPTION = "seller-sub";

    @Value("${azure.servicebus.connection-string:mock}")
    private String connectionString;

    private final SellerApplicationService sellerService;
    private final ObjectMapper mapper = new ObjectMapper();

    private ServiceBusProcessorClient processor;

    @PostConstruct
    public void start() {
        if (isMock()) {
            log.warn("[SELLER-ORDER-CONSUMER] Modo mock — consumer NO arrancará");
            return;
        }
        processor = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .processor()
                .topicName(TOPIC)
                .subscriptionName(SUBSCRIPTION)
                .processMessage(this::handleMessage)
                .processError(ctx -> log.error("[SELLER-ORDER-CONSUMER] Error: {}",
                        ctx.getException().getMessage()))
                .buildProcessorClient();
        processor.start();
        log.info("[SELLER-ORDER-CONSUMER] Escuchando '{}/{}'", TOPIC, SUBSCRIPTION);
    }

    @PreDestroy
    public void stop() {
        if (processor != null) processor.close();
    }

    private void handleMessage(ServiceBusReceivedMessageContext ctx) {
        try {
            BusEventDto event = mapper.readValue(
                    ctx.getMessage().getBody().toString(), BusEventDto.class);

            if (!"ORDER_RATED".equals(event.getEventType())) {
                ctx.complete();
                return;
            }

            String sellerId     = event.getSellerId();
            Integer calificacion = event.getCalificacion();

            if (sellerId == null || calificacion == null) {
                log.warn("[SELLER-ORDER-CONSUMER] Mensaje ORDER_RATED incompleto");
                ctx.complete();
                return;
            }

            sellerService.updateRating(sellerId, calificacion);
            log.info("[SELLER-ORDER-CONSUMER] Rating {} aplicado al seller {}", calificacion, sellerId);
            ctx.complete();
        } catch (Exception e) {
            log.error("[SELLER-ORDER-CONSUMER] Error: {}", e.getMessage());
            ctx.abandon();
        }
    }

    private boolean isMock() {
        return connectionString == null || connectionString.equals("mock") || connectionString.isBlank();
    }
}