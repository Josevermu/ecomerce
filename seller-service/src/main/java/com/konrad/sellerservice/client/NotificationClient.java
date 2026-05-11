package com.konrad.sellerservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Cliente HTTP hacia notification-service.
 *
 * Los eventos de Spring (ApplicationEventPublisher) solo viajan dentro
 * del mismo proceso JVM. Como seller-service y notification-service corren
 * en contenedores separados, este cliente llama al endpoint REST
 * POST /notifications/simulate del notification-service para disparar
 * el envío de correo certificado desde allí.
 *
 * En producción real se reemplazaría por RabbitMQ / Azure Service Bus.
 */
@Slf4j
@Component
public class NotificationClient {

    private final WebClient webClient;

    public NotificationClient(
            @Value("${konrad.services.notification-url:http://notification-service}") String notificationUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(notificationUrl)
                .build();
    }

    /** Solicitud enviada → correo con número de solicitud */
    public void notifySubmitted(String applicationId, String correo, String nombre) {
        send(Map.of(
                "type", "SUBMITTED",
                "applicationId", applicationId,
                "correo", correo,
                "nombre", nombre
        ));
    }

    /** Solicitud aprobada → correo con credenciales */
    public void notifyApproved(String applicationId, String correo, String nombre) {
        send(Map.of(
                "type", "APPROVED",
                "applicationId", applicationId,
                "correo", correo,
                "nombre", nombre
        ));
    }

    /** Solicitud rechazada → correo con motivo */
    public void notifyRejected(String applicationId, String correo, String motivo) {
        send(Map.of(
                "type", "REJECTED",
                "applicationId", applicationId,
                "correo", correo,
                "motivo", motivo
        ));
    }

    /** Solicitud devuelta → correo explicando cómo reactivar */
    public void notifyReturned(String applicationId, String correo) {
        send(Map.of(
                "type", "RETURNED",
                "applicationId", applicationId,
                "correo", correo
        ));
    }

    /** Suscripción vencida → correo de aviso de mora */
    public void notifySubscriptionExpired(String sellerId, String correo) {
        send(Map.of(
                "type", "SUBSCRIPTION_EXPIRED",
                "sellerId", sellerId,
                "correo", correo
        ));
    }

    /** Vendedor suspendido → correo con motivo de suspensión */
    public void notifySellerSuspended(String sellerId, String correo, String motivo) {
        send(Map.of(
                "type", "SELLER_SUSPENDED",
                "sellerId", sellerId,
                "correo", correo,
                "motivo", motivo
        ));
    }

    // ─── Interno ──────────────────────────────────────────────────────────────

    private void send(Map<String, String> payload) {
        try {
            webClient.post()
                    .uri("/notifications/simulate")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnSuccess(r -> log.info("[NOTIF-CLIENT] Evento enviado a notification-service: {}", payload.get("type")))
                    .doOnError(e -> log.error("[NOTIF-CLIENT] Error llamando notification-service: {}", e.getMessage()))
                    .subscribe(); // fire-and-forget — no bloquea el hilo del seller
        } catch (Exception e) {
            // Si notification-service no está disponible el flujo del seller no falla
            log.error("[NOTIF-CLIENT] Fallo al contactar notification-service: {}", e.getMessage());
        }
    }
}