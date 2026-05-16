package com.konrad.notificationservice.bus;

import com.azure.messaging.servicebus.*;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.konrad.notificationservice.service.EmailService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationBusConsumer {

    @Value("${azure.servicebus.connection-string:mock}")
    private String connectionString;

    private final EmailService emailService;

    // ── FAIL_ON_UNKNOWN_PROPERTIES = false ─────────────────────────────────────
    // Each publisher sends its own BusEventDto with different fields.
    // Without this, Jackson throws UnrecognizedPropertyException on fields
    // like 'monto' (payment), 'orderId'/'buyerId'/'total' (order), crashing
    // the consumer and sending every message to the Dead Letter Queue.
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final List<ServiceBusProcessorClient> processors = new ArrayList<>();

    @PostConstruct
    public void start() {
        if (isMock()) {
            log.warn("[NOTIF-BUS] Modo mock — usar POST /notifications/simulate para pruebas");
            return;
        }
        processors.add(buildProcessor("konrad-seller-events",  "notification-sub"));
        processors.add(buildProcessor("konrad-buyer-events",   "notification-sub"));
        processors.add(buildProcessor("konrad-payment-events", "notification-sub"));
        processors.add(buildProcessor("konrad-order-events",   "notification-sub"));
        processors.forEach(ServiceBusProcessorClient::start);
        log.info("[NOTIF-BUS] Escuchando 4 topics");
    }

    @PreDestroy
    public void stop() {
        processors.forEach(ServiceBusProcessorClient::close);
    }

    private void handleMessage(ServiceBusReceivedMessageContext ctx) {
        try {
            BusEventDto event = mapper.readValue(
                    ctx.getMessage().getBody().toString(), BusEventDto.class);
            log.info("[NOTIF-BUS] Recibido: {}", event.getEventType());
            route(event);
            ctx.complete();
        } catch (Exception e) {
            log.error("[NOTIF-BUS] Error procesando mensaje: {}", e.getMessage());
            ctx.abandon();
        }
    }

    private void route(BusEventDto e) {
        switch (e.getEventType()) {

            case "SELLER_SUBMITTED" -> emailService.sendCertifiedEmail(
                    e.getCorreo(),
                    "Solicitud recibida — #" + e.getEntityId(),
                    "Estimado/a " + e.getNombre() + ",\n\n" +
                            "Su solicitud ha sido registrada en estado PENDIENTE.\n" +
                            "Número: " + e.getEntityId() + "\n\nEquipo Konrad");

            case "SELLER_APPROVED" -> {
                String tmp = "Konrad" + (int)(Math.random() * 9000 + 1000) + "!";
                emailService.sendCertifiedEmail(
                        e.getCorreo(),
                        "¡Solicitud aprobada! Credenciales — Konrad",
                        "¡Felicitaciones, " + e.getNombre() + "!\n\n" +
                                "Solicitud #" + e.getEntityId() + " APROBADA.\n" +
                                "Usuario: " + e.getCorreo() + "\nContraseña temporal: " + tmp +
                                "\n\nEquipo Konrad");
            }

            case "SELLER_REJECTED" -> emailService.sendCertifiedEmail(
                    e.getCorreo(),
                    "Resultado de su solicitud — Konrad",
                    "Su solicitud #" + e.getEntityId() + " fue RECHAZADA.\n" +
                            "Motivo: " + humanize(e.getMotivo()) + "\n\nEquipo Konrad");

            case "SELLER_RETURNED" -> emailService.sendCertifiedEmail(
                    e.getCorreo(),
                    "Su solicitud requiere atención — Konrad",
                    "Su solicitud #" + e.getEntityId() + " fue DEVUELTA.\n" +
                            "Puede reactivarla al regularizar su situación crediticia.\n\nEquipo Konrad");

            case "SUBSCRIPTION_EXPIRED" -> emailService.sendCertifiedEmail(
                    e.getCorreo(),
                    "Suscripción vencida — Konrad",
                    "Su suscripción finalizó. Tiene 30 días para renovar.\n\nEquipo Konrad");

            case "SELLER_SUSPENDED" -> emailService.sendCertifiedEmail(
                    e.getCorreo(),
                    "Cuenta suspendida — Konrad",
                    "Su cuenta fue suspendida: " + humanizeSuspension(e.getMotivo()) +
                            ".\n\nEquipo Konrad");

            case "BUYER_REGISTERED" -> {
                String tmp = "Buyer" + (int)(Math.random() * 9000 + 1000) + "!";
                emailService.sendCertifiedEmail(
                        e.getCorreo(),
                        "Bienvenido a Konrad E-Commerce",
                        "Hola " + e.getNombre() + ",\n\nCuenta creada exitosamente.\n" +
                                "Usuario: " + e.getCorreo() + "\nContraseña temporal: " + tmp +
                                "\n\nEquipo Konrad");
            }

            case "PAYMENT_CONFIRMED" -> emailService.sendCertifiedEmail(
                    e.getCorreo() != null ? e.getCorreo() : "soporte@konrad.com",
                    "Pago confirmado — Konrad",
                    "Tu pago #" + e.getPaymentId() + " fue aprobado.\n\nEquipo Konrad");

            case "PAYMENT_REJECTED" -> emailService.sendCertifiedEmail(
                    e.getCorreo() != null ? e.getCorreo() : "soporte@konrad.com",
                    "Pago rechazado — Konrad",
                    "El pago fue rechazado. Motivo: " + humanize(e.getMotivo()) +
                            "\n\nEquipo Konrad");

            // ORDER_CREATED / ORDER_RATED no generan correo, solo log
            case "ORDER_CREATED", "ORDER_RATED" ->
                    log.info("[NOTIF-BUS] Evento {} recibido — sin acción de correo", e.getEventType());

            default -> log.warn("[NOTIF-BUS] EventType desconocido: {}", e.getEventType());
        }
    }

    private ServiceBusProcessorClient buildProcessor(String topic, String subscription) {
        return new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .processor()
                .topicName(topic)
                .subscriptionName(subscription)
                .processMessage(this::handleMessage)
                .processError(ctx -> log.error("[NOTIF-BUS] Error en {}: {}",
                        topic, ctx.getException().getMessage()))
                .buildProcessorClient();
    }

    private boolean isMock() {
        return connectionString == null
                || connectionString.equals("mock")
                || connectionString.isBlank();
    }

    private String humanize(String code) {
        if (code == null) return "Incumplimiento de requisitos";
        return switch (code) {
            case "BAJA_DATACREDITO"   -> "reporte negativo en Datacrédito";
            case "BAJA_CIFIN"         -> "reporte negativo en CIFIN";
            case "REQUERIDO_JUDICIAL" -> "requerimiento judicial activo";
            default -> code;
        };
    }

    private String humanizeSuspension(String code) {
        if (code == null) return "incumplimiento";
        return switch (code) {
            case "LOW_RATING_COUNT" -> "acumuló 10 o más calificaciones por debajo de 3";
            case "LOW_AVERAGE"      -> "calificación promedio bajó de 5";
            default -> code;
        };
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class BusEventDto {
        private String eventType, timestamp, entityId, correo, nombre;
        private String motivo, sellerId, paymentId, entityType;
    }
}