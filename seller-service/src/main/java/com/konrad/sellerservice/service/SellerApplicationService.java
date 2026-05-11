package com.konrad.sellerservice.service;

import com.konrad.sellerservice.dto.SellerDtos.*;
import com.konrad.sellerservice.service.SellerApplication.ApplicationStatus;
import com.konrad.sellerservice.model.entity.SellerApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PATRÓN OBSERVER — SellerApplicationService actúa en dos roles:
 *
 *  Como PUBLICADOR (Subject):
 *    Usa ApplicationEventPublisher para emitir eventos cuando cambian estados.
 *    No sabe quién escucha — el NotificationService reacciona automáticamente.
 *
 *  Como SUSCRIPTOR (Observer):
 *    @EventListener en activateSubscription() escucha SubscriptionPaymentConfirmed
 *    para activar al vendedor cuando payment-service confirma el cobro.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SellerApplicationService {

    private final SellerApplicationRepository repo;
    private final ApplicationEventPublisher eventPublisher; // OBSERVER: publicador de Spring

    // ─── Punto 1: Registro de solicitud ──────────────────────────────────────
    public ApplicationSubmittedResponse submitApplication(SellerRegistrationRequest request) {
        validateDocuments(request);

        SellerApplication app = SellerApplication.builder()
            .nombres(request.getNombres())
            .apellidos(request.getApellidos())
            .identificacion(request.getIdentificacion())
            .tipoPersona(SellerApplication.TipoPersona.valueOf(request.getTipoPersona()))
            .correo(request.getCorreo())
            .pais(request.getPais())
            .ciudad(request.getCiudad())
            .telefono(request.getTelefono())
            .documentos(request.getDocumentos())
            .status(ApplicationStatus.PENDIENTE)
            .fechaSolicitud(LocalDateTime.now())
            .totalCalificaciones(0).calificacionesBajas(0).promedioCalificacion(0)
            .build();

        SellerApplication saved = repo.save(app);
        log.info("[SELLER] Solicitud registrada: {}", saved.getId());

        // OBSERVER: publica evento → NotificationService envía correo con el número
        eventPublisher.publishEvent(new SellerEvents.ApplicationSubmitted(
            this, saved.getId(), saved.getCorreo(),
            saved.getNombres() + " " + saved.getApellidos()
        ));

        return ApplicationSubmittedResponse.builder()
            .applicationId(saved.getId())
            .status("PENDIENTE")
            .mensaje("Solicitud registrada. Número de solicitud: " + saved.getId())
            .build();
    }

    // ─── Punto 2: Director registra decisión ─────────────────────────────────
    public ApplicationDetailResponse registerDecision(String applicationId,
                                                       ApplicationDecisionRequest request) {
        SellerApplication app = repo.findById(applicationId)
            .orElseThrow(() -> new RuntimeException("Solicitud no encontrada: " + applicationId));

        if (app.getStatus() != ApplicationStatus.PENDIENTE) {
            throw new RuntimeException("Solo se pueden decidir solicitudes PENDIENTES");
        }

        ApplicationStatus newStatus = ApplicationStatus.valueOf(request.getDecision());
        app.setStatus(newStatus);
        app.setFechaDecision(LocalDateTime.now());
        app.setDecididoPor(request.getDirectorId());
        app.setMotivoRechazo(request.getMotivo());
        repo.save(app);

        // OBSERVER: publica el evento apropiado según la decisión
        switch (newStatus) {
            case APROBADA -> {
                log.info("[SELLER] Solicitud APROBADA: {}", applicationId);
                // → NotificationService.onApplicationApproved() envía credenciales
                eventPublisher.publishEvent(new SellerEvents.ApplicationApproved(
                    this, applicationId, app.getCorreo(), app.getNombres()));
            }
            case RECHAZADA -> {
                log.info("[SELLER] Solicitud RECHAZADA: {} — {}", applicationId, request.getMotivo());
                // → NotificationService.onApplicationRejected() envía correo con motivo
                eventPublisher.publishEvent(new SellerEvents.ApplicationRejected(
                    this, applicationId, app.getCorreo(), request.getMotivo()));
            }
            case DEVUELTA -> {
                log.info("[SELLER] Solicitud DEVUELTA: {}", applicationId);
                // → NotificationService.onApplicationReturned() explica cómo reactivar
                eventPublisher.publishEvent(new SellerEvents.ApplicationReturned(
                    this, applicationId, app.getCorreo()));
            }
            default -> throw new RuntimeException("Decisión inválida: " + request.getDecision());
        }

        return toDetail(app);
    }

    // ─── Punto 3: Activar suscripción al confirmar pago ──────────────────────
    // OBSERVER: este método ESCUCHA el evento de pago confirmado
    @EventListener
    public void activateSubscription(SellerEvents.SubscriptionPaymentConfirmed event) {
        repo.findById(event.getSellerId()).ifPresent(app -> {
            app.setStatus(ApplicationStatus.ACTIVA);
            repo.save(app);
            log.info("[SELLER] Suscripción ACTIVA para: {} (pago: {})",
                event.getSellerId(), event.getPaymentId());
        });
    }

    // ─── Punto 5: Job diario — verificar vencimientos ────────────────────────
    @Scheduled(cron = "0 0 6 * * *")   // Todos los días a las 6 AM
    public void checkExpiredSubscriptions() {
        List<SellerApplication> activos = repo.findByStatus(ApplicationStatus.ACTIVA);
        LocalDateTime ahora = LocalDateTime.now();

        for (SellerApplication app : activos) {
            // Mock: simular vencimiento según fecha de solicitud (60+ días = vencido)
            long diasDesde = java.time.Duration.between(app.getFechaSolicitud(), ahora).toDays();

            if (diasDesde > 60 && diasDesde <= 90) {
                app.setStatus(ApplicationStatus.EN_MORA);
                repo.save(app);
                log.info("[SELLER] EN MORA: {}", app.getId());
                // OBSERVER: NotificationService avisa que debe renovar
                eventPublisher.publishEvent(
                    new SellerEvents.SubscriptionExpired(this, app.getId(), app.getCorreo()));

            } else if (diasDesde > 90) {
                app.setStatus(ApplicationStatus.CANCELADA);
                repo.save(app);
                log.info("[SELLER] CANCELADA por mora: {}", app.getId());
            }
        }
    }

    // ─── Punto 6: Suspensión por calificaciones ───────────────────────────────
    public void updateRating(String sellerId, int newRating) {
        SellerApplication app = repo.findById(sellerId)
            .orElseThrow(() -> new RuntimeException("Vendedor no encontrado: " + sellerId));

        // Recalcular estadísticas
        int total = app.getTotalCalificaciones() + 1;
        int bajas = app.getCalificacionesBajas() + (newRating < 3 ? 1 : 0);
        double promedio = ((app.getPromedioCalificacion() * app.getTotalCalificaciones()) + newRating) / total;

        app.setTotalCalificaciones(total);
        app.setCalificacionesBajas(bajas);
        app.setPromedioCalificacion(promedio);
        repo.save(app);

        // Reglas de suspensión del documento
        boolean suspenderPorBajas = bajas >= 10;
        boolean suspenderPorPromedio = promedio < 5.0 && total >= 5;

        if ((suspenderPorBajas || suspenderPorPromedio) && app.getStatus() == ApplicationStatus.ACTIVA) {
            app.setStatus(ApplicationStatus.CANCELADA);
            repo.save(app);
            String motivo = suspenderPorBajas ? "LOW_RATING_COUNT" : "LOW_AVERAGE";
            log.info("[SELLER] CANCELADA por calificaciones: {} ({})", sellerId, motivo);
            // OBSERVER: NotificationService notifica al vendedor
            eventPublisher.publishEvent(
                new SellerEvents.SellerSuspended(this, sellerId, app.getCorreo(), motivo));
        }
    }

    // ─── Consultas ────────────────────────────────────────────────────────────
    public List<ApplicationSummaryResponse> searchApplications(String identificacion,
                                                                String status,
                                                                String desde, String hasta) {
        LocalDateTime desdeDt = desde != null ? LocalDateTime.parse(desde + "T00:00:00") : null;
        LocalDateTime hastaDt = hasta != null ? LocalDateTime.parse(hasta + "T23:59:59") : null;
        return repo.findByFilters(identificacion, status, desdeDt, hastaDt)
            .stream().map(this::toSummary).collect(Collectors.toList());
    }

    public ApplicationDetailResponse findById(String id) {
        return toDetail(repo.findById(id)
            .orElseThrow(() -> new RuntimeException("Solicitud no encontrada: " + id)));
    }

    // Consulta pública por número o identificación (punto 3)
    public ApplicationDetailResponse queryPublic(String query) {
        return repo.findById(query)
            .or(() -> repo.findByIdentificacion(query))
            .map(this::toDetail)
            .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));
    }

    // ─── Mappers ──────────────────────────────────────────────────────────────
    private ApplicationSummaryResponse toSummary(SellerApplication a) {
        return ApplicationSummaryResponse.builder()
            .id(a.getId()).identificacion(a.getIdentificacion())
            .apellidos(a.getApellidos()).nombres(a.getNombres())
            .correo(a.getCorreo()).status(a.getStatus().name())
            .fechaSolicitud(a.getFechaSolicitud() != null ? a.getFechaSolicitud().toString() : "")
            .build();
    }

    private ApplicationDetailResponse toDetail(SellerApplication a) {
        return ApplicationDetailResponse.builder()
            .id(a.getId()).nombres(a.getNombres()).apellidos(a.getApellidos())
            .identificacion(a.getIdentificacion())
            .tipoPersona(a.getTipoPersona() != null ? a.getTipoPersona().name() : "")
            .correo(a.getCorreo()).pais(a.getPais()).ciudad(a.getCiudad())
            .telefono(a.getTelefono()).documentos(a.getDocumentos())
            .status(a.getStatus().name()).motivoRechazo(a.getMotivoRechazo())
            .fechaSolicitud(a.getFechaSolicitud() != null ? a.getFechaSolicitud().toString() : "")
            .fechaDecision(a.getFechaDecision() != null ? a.getFechaDecision().toString() : "")
            .build();
    }

    private void validateDocuments(SellerRegistrationRequest req) {
        boolean esJuridica = "JURIDICA".equals(req.getTipoPersona());
        if (req.getDocumentos() == null || req.getDocumentos().isEmpty()) {
            throw new RuntimeException("Debe adjuntar los documentos requeridos");
        }
        if (esJuridica && req.getDocumentos().size() < 3) {
            throw new RuntimeException("Persona jurídica debe adjuntar: cédula, RUT y cámara de comercio");
        }
    }
}
