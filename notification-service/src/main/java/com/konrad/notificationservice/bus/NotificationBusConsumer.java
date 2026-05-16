package com.konrad.notificationservice.bus;

import com.azure.messaging.servicebus.*;
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

/**
 * Consumer de Azure Service Bus para notification-service.
 *
 * Escucha 4 topics (suscripción "notification-sub" en cada uno) y
 * delega el envío de correo a EmailService según el eventType.
 *
 * Reemplaza el mecanismo de Spring Events que no cruzaba contenedores.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationBusConsumer {

    @Value("${azure.servicebus.connection-string:mock}")
    private String connectionString;

    private final EmailService emailService;
    private final ObjectMapper mapper = new ObjectMapper();

    private final List<ServiceBusProcessorClient> processors = new ArrayList<>();

    @PostConstruct
    public void start() {
        if (isMock()) {
            log.warn("[NOTIF-BUS] Modo mock — consumers NO arrancarán. Usar POST /notifications/simulate");
            return;
        }
        processors.add(buildProcessor("konrad-seller-events",  "notification-sub"));
        processors.add(buildProcessor("konrad-buyer-events",   "notification-sub"));
        processors.add(buildProcessor("konrad-payment-events", "notification-sub"));
        processors.add(buildProcessor("konrad-order-events",   "notification-sub"));
        processors.forEach(p -> { p.start(); log.info("[NOTIF-BUS] Consumer arrancado: {}", p); });
    }

    @PreDestroy
    public void stop() { processors.forEach(ServiceBusProcessorClient::close); }

    // ── Procesamiento de mensajes ──────────────────────────────────────────────

    private void handleMessage(ServiceBusReceivedMessageContext ctx) {
        try {
            String body = ctx.getMessage().getBody().toString();
            BusEventDto event = mapper.readValue(body, BusEventDto.class);
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
                    "Solicitud de vendedor recibida — #" + e.getEntityId(),
                    "Estimado/a " + e.getNombre() + ",\n\nSu solicitud ha sido registrada en estado PENDIENTE.\n" +
                            "Número de solicitud: " + e.getEntityId() + "\n\nEquipo Konrad");

            case "SELLER_APPROVED" -> {
                String tmp = "Konrad" + (int)(Math.random() * 9000 + 1000) + "!";
                emailService.sendCertifiedEmail(
                        e.getCorreo(),
                        "¡Solicitud aprobada! Credenciales — Konrad E-Commerce",
                        "¡Felicitaciones, " + e.getNombre() + "!\n\nSu solicitud #" + e.getEntityId() +
                                " fue APROBADA.\n\nUsuario: " + e.getCorreo() +
                                "\nContraseña temporal: " + tmp + "\n\nEquipo Konrad");
            }

            case "SELLER_REJECTED" -> emailService.sendCertifiedEmail(
                    e.getCorreo(),
                    "Resultado de su solicitud — Konrad E-Commerce",
                    "Su solicitud #" + e.getEntityId() + " fue RECHAZADA.\nMotivo: " +
                            humanize(e.getMotivo()) + "\n\nEquipo Konrad");

            case "SELLER_RETURNED" -> emailService.sendCertifiedEmail(
                    e.getCorreo(),
                    "Su solicitud requiere atención — Konrad E-Commerce",
                    "Su solicitud #" + e.getEntityId() + " fue DEVUELTA.\n" +
                            "Puede reactivarla una vez regularice su situación crediticia.\n\nEquipo Konrad");

            case "SUBSCRIPTION_EXPIRED" -> emailService.sendCertifiedEmail(
                    e.getCorreo(),
                    "Su suscripción ha vencido — Konrad E-Commerce",
                    "Su período de suscripción finalizó. Tiene 30 días para renovar.\n\nEquipo Konrad");

            case "SELLER_SUSPENDED" -> emailService.sendCertifiedEmail(
                    e.getCorreo(),
                    "Cuenta suspendida — Konrad E-Commerce",
                    "Su cuenta fue suspendida porque " + humanizeSuspension(e.getMotivo()) + ".\n\nEquipo Konrad");

            case "BUYER_REGISTERED" -> {
                String tmp = "Buyer" + (int)(Math.random() * 9000 + 1000) + "!";
                emailService.sendCertifiedEmail(
                        e.getCorreo(),
                        "Bienvenido a Konrad E-Commerce",
                        "Hola " + e.getNombre() + ",\n\nTu cuenta ha sido creada.\n" +
                                "Usuario: " + e.getCorreo() + "\nContraseña temporal: " + tmp + "\n\nEquipo Konrad");
            }

            case "PAYMENT_CONFIRMED" -> emailService.sendCertifiedEmail(
                    e.getCorreo() != null ? e.getCorreo() : "soporte@konrad.com",
                    "Pago confirmado — Konrad E-Commerce",
                    "Tu pago #" + e.getPaymentId() + " fue aprobado.\n\nEquipo Konrad");

            case "PAYMENT_REJECTED" -> emailService.sendCertifiedEmail(
                    e.getCorreo() != null ? e.getCorreo() : "soporte@konrad.com",
                    "Pago rechazado — Konrad E-Commerce",
                    "El pago para #" + e.getEntityId() + " fue rechazado. Motivo: " +
                            humanize(e.getMotivo()) + "\n\nEquipo Konrad");

            default -> log.warn("[NOTIF-BUS] EventType desconocido: {}", e.getEventType());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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
        return connectionString == null || connectionString.equals("mock") || connectionString.isBlank();
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
            case "LOW_AVERAGE"      -> "su calificación promedio bajó de 5";
            default -> code;
        };
    }

    // ── DTO local ─────────────────────────────────────────────────────────────
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class BusEventDto {
        private String eventType, timestamp, entityId, correo, nombre;
        private String motivo, sellerId, paymentId, entityType;
    }
}