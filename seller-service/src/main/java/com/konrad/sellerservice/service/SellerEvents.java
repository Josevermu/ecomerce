package com.konrad.sellerservice.service;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * PATRÓN OBSERVER — Eventos de dominio del seller-service.
 *
 * Spring publica estos eventos con ApplicationEventPublisher.
 * Cualquier @EventListener en cualquier @Component los recibirá
 * sin que el publicador sepa quién escucha (desacoplamiento total).
 *
 * En producción con múltiples microservicios: RabbitMQ/Kafka.
 * Aquí: Spring Events (mismo proceso, ideal para pruebas/mock).
 */
public class SellerEvents {

    // ─── Solicitud enviada ────────────────────────────────────────────────────
    @Getter
    public static class ApplicationSubmitted extends ApplicationEvent {
        private final String applicationId;
        private final String correo;
        private final String nombreCompleto;

        public ApplicationSubmitted(Object source, String applicationId,
                                    String correo, String nombreCompleto) {
            super(source);
            this.applicationId = applicationId;
            this.correo = correo;
            this.nombreCompleto = nombreCompleto;
        }
    }

    // ─── Solicitud aprobada ───────────────────────────────────────────────────
    @Getter
    public static class ApplicationApproved extends ApplicationEvent {
        private final String applicationId;
        private final String correo;
        private final String nombres;

        public ApplicationApproved(Object source, String applicationId,
                                   String correo, String nombres) {
            super(source);
            this.applicationId = applicationId;
            this.correo = correo;
            this.nombres = nombres;
        }
    }

    // ─── Solicitud rechazada ──────────────────────────────────────────────────
    @Getter
    public static class ApplicationRejected extends ApplicationEvent {
        private final String applicationId;
        private final String correo;
        private final String motivo;

        public ApplicationRejected(Object source, String applicationId,
                                   String correo, String motivo) {
            super(source);
            this.applicationId = applicationId;
            this.correo = correo;
            this.motivo = motivo;
        }
    }

    // ─── Solicitud devuelta ───────────────────────────────────────────────────
    @Getter
    public static class ApplicationReturned extends ApplicationEvent {
        private final String applicationId;
        private final String correo;

        public ApplicationReturned(Object source, String applicationId, String correo) {
            super(source);
            this.applicationId = applicationId;
            this.correo = correo;
        }
    }

    // ─── Suscripción vencida ──────────────────────────────────────────────────
    @Getter
    public static class SubscriptionExpired extends ApplicationEvent {
        private final String sellerId;
        private final String correo;

        public SubscriptionExpired(Object source, String sellerId, String correo) {
            super(source);
            this.sellerId = sellerId;
            this.correo = correo;
        }
    }

    // ─── Vendedor suspendido por calificaciones ───────────────────────────────
    @Getter
    public static class SellerSuspended extends ApplicationEvent {
        private final String sellerId;
        private final String correo;
        private final String motivo;  // "LOW_RATING_COUNT" | "LOW_AVERAGE"

        public SellerSuspended(Object source, String sellerId, String correo, String motivo) {
            super(source);
            this.sellerId = sellerId;
            this.correo = correo;
            this.motivo = motivo;
        }
    }

    // ─── Pago de suscripción confirmado (recibido desde payment-service) ──────
    @Getter
    public static class SubscriptionPaymentConfirmed extends ApplicationEvent {
        private final String sellerId;
        private final String paymentId;
        private final String tipoSuscripcion;

        public SubscriptionPaymentConfirmed(Object source, String sellerId,
                                            String paymentId, String tipoSuscripcion) {
            super(source);
            this.sellerId = sellerId;
            this.paymentId = paymentId;
            this.tipoSuscripcion = tipoSuscripcion;
        }
    }
}
