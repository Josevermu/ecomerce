package com.konrad.notificationservice.service;

import com.konrad.notificationservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * PATRÓN OBSERVER — Todos los listeners del notification-service.
 *
 * Este servicio es un Observer PURO: no tiene lógica de negocio propia,
 * solo escucha eventos y envía correos.
 *
 * Cada @EventListener reacciona a un tipo de evento diferente.
 * Los publicadores (seller-service, payment-service, order-service) no
 * conocen esta clase — desacoplamiento total.
 *
 * Para que funcione entre microservicios reales se usaría RabbitMQ/Kafka.
 * En este mock: Spring Events (mismo proceso).
 *
 * Para simular eventos de otros servicios, se expone un endpoint REST
 * /notifications/simulate que dispara cualquier tipo de evento.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListeners {

    private final EmailService emailService;

    // ─── Eventos de la aplicación del vendedor ────────────────────────────────

    /**
     * OBSERVER: Escucha ApplicationSubmitted.
     * Requerimiento punto 1: "arrojando el número de esta [la solicitud]"
     */
    @EventListener
    public void onApplicationSubmitted(ApplicationSubmittedEvent event) {
        log.info("[NOTIF-OBSERVER] Escuchando ApplicationSubmitted: {}", event.getApplicationId());
        emailService.sendCertifiedEmail(
            event.getCorreo(),
            "Solicitud de vendedor recibida - #" + event.getApplicationId(),
            String.format("""
                Estimado/a %s,

                Su solicitud para ser vendedor en Comercial Konrad ha sido
                recibida exitosamente.

                Número de solicitud: %s
                Estado actual: PENDIENTE

                Puede consultar el estado ingresando su número de solicitud
                o número de identificación en: https://ecommerce.konrad.com/status

                Atentamente,
                Equipo Comercial Konrad
                """, event.getNombreCompleto(), event.getApplicationId())
        );
    }

    /**
     * OBSERVER: Escucha ApplicationApproved.
     * Requerimiento punto 3: "el sistema enviará por correo electrónico las credenciales"
     */
    @EventListener
    public void onApplicationApproved(ApplicationApprovedEvent event) {
        log.info("[NOTIF-OBSERVER] Escuchando ApplicationApproved: {}", event.getApplicationId());
        // Mock: genera contraseña temporal (RNF: mínimo 8 chars, mayús, minús, número)
        String tempPassword = "Konrad" + (int)(Math.random() * 9000 + 1000) + "!";

        emailService.sendCertifiedEmail(
            event.getCorreo(),
            "¡Solicitud aprobada! Credenciales de acceso - Konrad E-Commerce",
            String.format("""
                ¡Felicitaciones, %s!

                Su solicitud #%s ha sido APROBADA.

                Sus credenciales de acceso temporales:
                  Usuario:    %s
                  Contraseña: %s

                Deberá cambiar su contraseña en el primer ingreso.

                El siguiente paso es activar su suscripción para poder publicar productos.
                Acceda a: https://ecommerce.konrad.com/login

                Atentamente,
                Equipo Comercial Konrad
                """, event.getNombres(), event.getApplicationId(),
                event.getCorreo(), tempPassword)
        );
    }

    /**
     * OBSERVER: Escucha ApplicationRejected.
     * Requerimiento punto 2: "le debe llegar un correo comunicándole el porqué"
     */
    @EventListener
    public void onApplicationRejected(ApplicationRejectedEvent event) {
        log.info("[NOTIF-OBSERVER] Escuchando ApplicationRejected: {}", event.getApplicationId());
        String motivoLegible = humanizeReason(event.getMotivo());

        emailService.sendCertifiedEmail(
            event.getCorreo(),
            "Resultado de su solicitud - Konrad E-Commerce",
            String.format("""
                Estimado solicitante,

                Lamentamos informarle que su solicitud #%s ha sido RECHAZADA.

                Motivo: %s

                Si considera que existe un error, puede contactar a nuestro
                equipo de soporte: soporte@konrad.com

                Atentamente,
                Equipo Comercial Konrad
                """, event.getApplicationId(), motivoLegible)
        );
    }

    /**
     * OBSERVER: Escucha ApplicationReturned.
     * Requerimiento punto 2: estado DEVUELTA — puede reactivar cuando tenga calificación ALTA
     */
    @EventListener
    public void onApplicationReturned(ApplicationReturnedEvent event) {
        log.info("[NOTIF-OBSERVER] Escuchando ApplicationReturned: {}", event.getApplicationId());
        emailService.sendCertifiedEmail(
            event.getCorreo(),
            "Su solicitud requiere atención - Konrad E-Commerce",
            String.format("""
                Estimado solicitante,

                Su solicitud #%s ha sido DEVUELTA.

                Su historial crediticio presenta calificación en ADVERTENCIA
                en alguna de las centrales de riesgo consultadas.

                Una vez regularice su situación y obtenga calificación ALTA,
                podrá reactivar su solicitud desde nuestra plataforma.

                Acceda a: https://ecommerce.konrad.com/status

                Atentamente,
                Equipo Comercial Konrad
                """, event.getApplicationId())
        );
    }

    /**
     * OBSERVER: Escucha SubscriptionExpired.
     * Requerimiento punto 5: avisarle al vendedor que debe cancelar nueva suscripción
     */
    @EventListener
    public void onSubscriptionExpired(SubscriptionExpiredEvent event) {
        log.info("[NOTIF-OBSERVER] Escuchando SubscriptionExpired: {}", event.getSellerId());
        emailService.sendCertifiedEmail(
            event.getCorreo(),
            "Su suscripción ha vencido - Konrad E-Commerce",
            String.format("""
                Estimado vendedor,

                Su período de suscripción ha finalizado. Su cuenta está EN MORA.

                Tiene 30 días para renovar su suscripción antes de que
                su cuenta quede CANCELADA y sus productos sean removidos.

                Renueve aquí: https://ecommerce.konrad.com/subscription

                Atentamente,
                Equipo Comercial Konrad
                """)
        );
    }

    /**
     * OBSERVER: Escucha SellerSuspended.
     * Requerimiento punto 6: suspensión por calificaciones bajas
     */
    @EventListener
    public void onSellerSuspended(SellerSuspendedEvent event) {
        log.info("[NOTIF-OBSERVER] Escuchando SellerSuspended: {}", event.getSellerId());
        String motivo = "LOW_RATING_COUNT".equals(event.getMotivo())
            ? "acumuló 10 o más calificaciones por debajo de 3"
            : "su calificación promedio bajó de 5";

        emailService.sendCertifiedEmail(
            event.getCorreo(),
            "Cuenta suspendida - Konrad E-Commerce",
            String.format("Su cuenta ha sido suspendida porque %s.", motivo)
        );
    }

    // ─── Helper ───────────────────────────────────────────────────────────────
    private String humanizeReason(String code) {
        if (code == null) return "Incumplimiento de requisitos";
        return switch (code) {
            case "BAJA_DATACREDITO"   -> "presenta reporte negativo en Datacrédito";
            case "BAJA_CIFIN"         -> "presenta reporte negativo en CIFIN";
            case "REQUERIDO_JUDICIAL" -> "tiene requerimiento judicial activo";
            default -> code;
        };
    }

    // ─── Eventos (clases internas — en producción son DTOs compartidos vía jar) ─

    public static class ApplicationSubmittedEvent { /* simulado en controller */ 
        private final String applicationId, correo, nombreCompleto;
        public ApplicationSubmittedEvent(String applicationId, String correo, String nombreCompleto) {
            this.applicationId = applicationId; this.correo = correo; this.nombreCompleto = nombreCompleto;
        }
        public String getApplicationId() { return applicationId; }
        public String getCorreo() { return correo; }
        public String getNombreCompleto() { return nombreCompleto; }
    }

    public static class ApplicationApprovedEvent {
        private final String applicationId, correo, nombres;
        public ApplicationApprovedEvent(String applicationId, String correo, String nombres) {
            this.applicationId = applicationId; this.correo = correo; this.nombres = nombres;
        }
        public String getApplicationId() { return applicationId; }
        public String getCorreo() { return correo; }
        public String getNombres() { return nombres; }
    }

    public static class ApplicationRejectedEvent {
        private final String applicationId, correo, motivo;
        public ApplicationRejectedEvent(String applicationId, String correo, String motivo) {
            this.applicationId = applicationId; this.correo = correo; this.motivo = motivo;
        }
        public String getApplicationId() { return applicationId; }
        public String getCorreo() { return correo; }
        public String getMotivo() { return motivo; }
    }

    public static class ApplicationReturnedEvent {
        private final String applicationId, correo;
        public ApplicationReturnedEvent(String applicationId, String correo) {
            this.applicationId = applicationId; this.correo = correo;
        }
        public String getApplicationId() { return applicationId; }
        public String getCorreo() { return correo; }
    }

    public static class SubscriptionExpiredEvent {
        private final String sellerId, correo;
        public SubscriptionExpiredEvent(String sellerId, String correo) {
            this.sellerId = sellerId; this.correo = correo;
        }
        public String getSellerId() { return sellerId; }
        public String getCorreo() { return correo; }
    }

    public static class SellerSuspendedEvent {
        private final String sellerId, correo, motivo;
        public SellerSuspendedEvent(String sellerId, String correo, String motivo) {
            this.sellerId = sellerId; this.correo = correo; this.motivo = motivo;
        }
        public String getSellerId() { return sellerId; }
        public String getCorreo() { return correo; }
        public String getMotivo() { return motivo; }
    }
}
