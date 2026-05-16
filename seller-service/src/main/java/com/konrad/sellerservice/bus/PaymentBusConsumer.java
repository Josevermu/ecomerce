package com.konrad.sellerservice.bus;

import com.azure.messaging.servicebus.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.konrad.sellerservice.service.SellerApplication;
import com.konrad.sellerservice.model.entity.SellerApplicationRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Consume mensajes del topic "konrad-payment-events", suscripción "seller-sub".
 *
 * Cuando payment-service confirma un pago de suscripción (PAYMENT_CONFIRMED
 * con entityType=SUBSCRIPTION), activa al vendedor cambiando su estado a ACTIVA.
 *
 * Reemplaza el @EventListener de Spring que no cruzaba contenedores.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentBusConsumer {

    private static final String TOPIC        = "konrad-payment-events";
    private static final String SUBSCRIPTION = "seller-sub";

    @Value("${azure.servicebus.connection-string:mock}")
    private String connectionString;

    private final SellerApplicationRepository repo;
    private final ObjectMapper mapper = new ObjectMapper();

    private ServiceBusProcessorClient processor;

    @PostConstruct
    public void start() {
        if (isMock()) {
            log.warn("[SELLER-PAYMENT-CONSUMER] Modo mock — consumer NO arrancará");
            return;
        }
        processor = new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .processor()
                .topicName(TOPIC)
                .subscriptionName(SUBSCRIPTION)
                .processMessage(this::handleMessage)
                .processError(ctx -> log.error("[SELLER-PAYMENT-CONSUMER] Error: {}",
                        ctx.getException().getMessage()))
                .buildProcessorClient();
        processor.start();
        log.info("[SELLER-PAYMENT-CONSUMER] Escuchando '{}/{}'", TOPIC, SUBSCRIPTION);
    }

    @PreDestroy
    public void stop() {
        if (processor != null) processor.close();
    }

    private void handleMessage(ServiceBusReceivedMessageContext ctx) {
        try {
            String body = ctx.getMessage().getBody().toString();
            BusEventDto event = mapper.readValue(body, BusEventDto.class);

            if (!"PAYMENT_CONFIRMED".equals(event.getEventType())) {
                ctx.complete();
                return;
            }

            // Solo actúa si el pago es de una SUSCRIPCIÓN
            if (!"SUBSCRIPTION".equals(event.getEntityType())) {
                ctx.complete();
                return;
            }

            String sellerId = event.getSellerId() != null
                    ? event.getSellerId() : event.getEntityId();

            repo.findById(sellerId).ifPresentOrElse(app -> {
                app.setStatus(SellerApplication.ApplicationStatus.ACTIVA);
                repo.save(app);
                log.info("[SELLER-PAYMENT-CONSUMER] Suscripción ACTIVA — seller: {}, pago: {}",
                        sellerId, event.getPaymentId());
            }, () -> log.warn("[SELLER-PAYMENT-CONSUMER] Seller no encontrado: {}", sellerId));

            ctx.complete();
        } catch (Exception e) {
            log.error("[SELLER-PAYMENT-CONSUMER] Error procesando mensaje: {}", e.getMessage());
            ctx.abandon();
        }
    }

    private boolean isMock() {
        return connectionString == null || connectionString.equals("mock") || connectionString.isBlank();
    }
}