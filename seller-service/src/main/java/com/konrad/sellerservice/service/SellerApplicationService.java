package com.konrad.sellerservice.service;

import com.konrad.sellerservice.bus.SellerEventPublisher;
import com.konrad.sellerservice.dto.SellerDtos.*;
import com.konrad.sellerservice.service.SellerApplication.ApplicationStatus;
import com.konrad.sellerservice.model.entity.SellerApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SellerApplicationService {

    private final SellerApplicationRepository repo;
    private final SellerEventPublisher eventPublisher; // ← Service Bus publisher

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
                .totalCalificaciones(0)
                .calificacionesBajas(0)
                .promedioCalificacion(0)
                .build();

        SellerApplication saved = repo.save(app);
        log.info("[SELLER] Solicitud registrada: {}", saved.getId());

        // Publica en konrad-seller-events → notification-service envía correo
        eventPublisher.publishSubmitted(
                saved.getId(),
                saved.getCorreo(),
                saved.getNombres() + " " + saved.getApellidos()
        );

        return ApplicationSubmittedResponse.builder()
                .applicationId(saved.getId())
                .status("PENDIENTE")
                .mensaje("Solicitud registrada. Número: " + saved.getId())
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

        switch (newStatus) {
            case APROBADA -> {
                log.info("[SELLER] Solicitud APROBADA: {}", applicationId);
                eventPublisher.publishApproved(applicationId, app.getCorreo(), app.getNombres());
            }
            case RECHAZADA -> {
                log.info("[SELLER] Solicitud RECHAZADA: {}", applicationId);
                eventPublisher.publishRejected(applicationId, app.getCorreo(), request.getMotivo());
            }
            case DEVUELTA -> {
                log.info("[SELLER] Solicitud DEVUELTA: {}", applicationId);
                eventPublisher.publishReturned(applicationId, app.getCorreo());
            }
            default -> throw new RuntimeException("Decisión inválida: " + request.getDecision());
        }

        return toDetail(app);
    }

    // ─── Punto 3: Activar suscripción (escucha bus desde PaymentBusConsumer) ──
    @EventListener
    public void activateSubscription(SellerEvents.SubscriptionPaymentConfirmed event) {
        repo.findById(event.getSellerId()).ifPresent(app -> {
            app.setStatus(ApplicationStatus.ACTIVA);
            repo.save(app);
            log.info("[SELLER] Suscripción ACTIVA: {} (pago: {})",
                    event.getSellerId(), event.getPaymentId());
        });
    }

    // ─── Punto 5: Job diario — vencimientos ───────────────────────────────────
    @Scheduled(cron = "0 0 6 * * *")
    public void checkExpiredSubscriptions() {
        List<SellerApplication> activos = repo.findByStatus(ApplicationStatus.ACTIVA);
        LocalDateTime ahora = LocalDateTime.now();

        for (SellerApplication app : activos) {
            long dias = java.time.Duration.between(app.getFechaSolicitud(), ahora).toDays();

            if (dias > 60 && dias <= 90) {
                app.setStatus(ApplicationStatus.EN_MORA);
                repo.save(app);
                eventPublisher.publishSubscriptionExpired(app.getId(), app.getCorreo());
                log.info("[SELLER] EN MORA: {}", app.getId());

            } else if (dias > 90) {
                app.setStatus(ApplicationStatus.CANCELADA);
                repo.save(app);
                log.info("[SELLER] CANCELADA por mora: {}", app.getId());
            }
        }
    }

    // ─── Punto 6: Actualizar calificación ────────────────────────────────────
    public void updateRating(String sellerId, int newRating) {
        SellerApplication app = repo.findById(sellerId)
                .orElseThrow(() -> new RuntimeException("Vendedor no encontrado: " + sellerId));

        int total    = app.getTotalCalificaciones() + 1;
        int bajas    = app.getCalificacionesBajas() + (newRating < 3 ? 1 : 0);
        double prom  = ((app.getPromedioCalificacion() * app.getTotalCalificaciones()) + newRating) / total;

        app.setTotalCalificaciones(total);
        app.setCalificacionesBajas(bajas);
        app.setPromedioCalificacion(prom);
        repo.save(app);

        boolean porBajas   = bajas >= 10;
        boolean porPromedio = prom < 5.0 && total >= 5;

        if ((porBajas || porPromedio) && app.getStatus() == ApplicationStatus.ACTIVA) {
            app.setStatus(ApplicationStatus.CANCELADA);
            repo.save(app);
            String motivo = porBajas ? "LOW_RATING_COUNT" : "LOW_AVERAGE";
            eventPublisher.publishSellerSuspended(sellerId, app.getCorreo(), motivo);
            log.info("[SELLER] CANCELADA por calificaciones: {} ({})", sellerId, motivo);
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
                .fechaDecision(a.getFechaDecision()  != null ? a.getFechaDecision().toString()  : "")
                .build();
    }

    private void validateDocuments(SellerRegistrationRequest req) {
        if (req.getDocumentos() == null || req.getDocumentos().isEmpty())
            throw new RuntimeException("Debe adjuntar los documentos requeridos");
        if ("JURIDICA".equals(req.getTipoPersona()) && req.getDocumentos().size() < 3)
            throw new RuntimeException("Persona jurídica: cédula/NIT, RUT y cámara de comercio");
    }
}