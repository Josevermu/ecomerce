package com.konrad.sellerservice.bus;

import com.azure.messaging.servicebus.*;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.konrad.sellerservice.service.SellerApplicationService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderBusConsumer {

    private static final String TOPIC        = "konrad-order-events";
    private static final String SUBSCRIPTION = "seller-sub";

    @Value("${azure.servicebus.connection-string:mock}")
    private String connectionString;

    private final SellerApplicationService sellerService;

    // FAIL_ON_UNKNOWN_PROPERTIES = false — OrderEventPublisher sends
    // 'orderId', 'buyerId', 'total' which are not in BusEventDto.
    // Without this every ORDER message crashes the consumer.
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

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

            if (event.getSellerId() == null || event.getCalificacion() == null) {
                log.warn("[SELLER-ORDER-CONSUMER] Mensaje ORDER_RATED incompleto");
                ctx.complete();
                return;
            }

            sellerService.updateRating(event.getSellerId(), event.getCalificacion());
            log.info("[SELLER-ORDER-CONSUMER] Rating {} → seller {}",
                    event.getCalificacion(), event.getSellerId());
            ctx.complete();
        } catch (Exception e) {
            log.error("[SELLER-ORDER-CONSUMER] Error: {}", e.getMessage());
            ctx.abandon();
        }
    }

    private boolean isMock() {
        return connectionString == null
                || connectionString.equals("mock")
                || connectionString.isBlank();
    }
}