package com.konrad.sellerservice.service;

import com.konrad.sellerservice.client.AuthClient;
import com.konrad.sellerservice.client.NotificationClient;
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
    private final NotificationClient notificationClient;
    private final AuthClient authClient; // CORRECCIÓN: agregado para crear usuario al aprobar

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

        notificationClient.notifySubmitted(
                saved.getId(),
                saved.getCorreo(),
                saved.getNombres() + " " + saved.getApellidos()
        );

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

        switch (newStatus) {
            case APROBADA -> {
                log.info("[SELLER] Solicitud APROBADA: {}", applicationId);

                // CORRECCIÓN: crear el usuario en auth-service ANTES de notificar
                // para que el vendedor pueda hacer login con las credenciales del correo.
                // authClient.registerApprovedSeller() retorna la contraseña temporal
                // generada, que el notification-service incluirá en el correo.
                String tempPassword = authClient.registerApprovedSeller(
                        applicationId, app.getCorreo());

                log.info("[SELLER] Usuario creado en auth-service para: {} (pass: {})",
                        app.getCorreo(), tempPassword);

                notificationClient.notifyApproved(applicationId, app.getCorreo(), app.getNombres());
            }
            case RECHAZADA -> {
                log.info("[SELLER] Solicitud RECHAZADA: {} — {}", applicationId, request.getMotivo());
                notificationClient.notifyRejected(applicationId, app.getCorreo(), request.getMotivo());
            }
            case DEVUELTA -> {
                log.info("[SELLER] Solicitud DEVUELTA: {}", applicationId);
                notificationClient.notifyReturned(applicationId, app.getCorreo());
            }
            default -> throw new RuntimeException("Decisión inválida: " + request.getDecision());
        }

        return toDetail(app);
    }

    // ─── Punto 3: Activar suscripción tras pago ──────────────────────────────
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
    @Scheduled(cron = "0 0 6 * * *")
    public void checkExpiredSubscriptions() {
        List<SellerApplication> activos = repo.findByStatus(ApplicationStatus.ACTIVA);
        LocalDateTime ahora = LocalDateTime.now();

        for (SellerApplication app : activos) {
            long diasDesde = java.time.Duration.between(app.getFechaSolicitud(), ahora).toDays();

            if (diasDesde > 60 && diasDesde <= 90) {
                app.setStatus(ApplicationStatus.EN_MORA);
                repo.save(app);
                log.info("[SELLER] EN MORA: {}", app.getId());
                notificationClient.notifySubscriptionExpired(app.getId(), app.getCorreo());
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

        int total    = app.getTotalCalificaciones() + 1;
        int bajas    = app.getCalificacionesBajas() + (newRating < 3 ? 1 : 0);
        double promedio = ((app.getPromedioCalificacion() * app.getTotalCalificaciones()) + newRating) / total;

        app.setTotalCalificaciones(total);
        app.setCalificacionesBajas(bajas);
        app.setPromedioCalificacion(promedio);
        repo.save(app);

        boolean suspenderPorBajas    = bajas >= 10;
        boolean suspenderPorPromedio = promedio < 5.0 && total >= 5;

        if ((suspenderPorBajas || suspenderPorPromedio) && app.getStatus() == ApplicationStatus.ACTIVA) {
            app.setStatus(ApplicationStatus.CANCELADA);
            repo.save(app);
            String motivo = suspenderPorBajas ? "LOW_RATING_COUNT" : "LOW_AVERAGE";
            notificationClient.notifySellerSuspended(app.getId(), app.getCorreo(), motivo);
            log.info("[SELLER] Vendedor CANCELADO por calificaciones: {} ({})", sellerId, motivo);
        }
    }

    // ─── Consultas públicas y del Director ────────────────────────────────────
    public ApplicationDetailResponse queryPublic(String q) {
        SellerApplication app = repo.findById(q)
                .or(() -> repo.findByIdentificacion(q))
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada: " + q));
        return toDetail(app);
    }

    public List<ApplicationSummaryResponse> searchApplications(String identificacion,
                                                                String status,
                                                                String desde, String hasta) {
        LocalDateTime desdeDT = desde != null ? LocalDateTime.parse(desde + "T00:00:00") : null;
        LocalDateTime hastaDT = hasta != null ? LocalDateTime.parse(hasta + "T23:59:59") : null;

        return repo.findByFilters(identificacion, status, desdeDT, hastaDT)
                .stream().map(this::toSummary).collect(Collectors.toList());
    }

    public ApplicationDetailResponse findById(String id) {
        return toDetail(repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada: " + id)));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────
    private void validateDocuments(SellerRegistrationRequest request) {
        if (request.getDocumentos() == null || request.getDocumentos().size() < 4) {
            throw new RuntimeException("Se requieren al menos 4 documentos adjuntos");
        }
    }

    private ApplicationSummaryResponse toSummary(SellerApplication a) {
        return ApplicationSummaryResponse.builder()
                .applicationId(a.getId())
                .identificacion(a.getIdentificacion())
                .apellidos(a.getApellidos())
                .nombres(a.getNombres())
                .correo(a.getCorreo())
                .status(a.getStatus().name())
                .build();
    }

    private ApplicationDetailResponse toDetail(SellerApplication a) {
        return ApplicationDetailResponse.builder()
                .applicationId(a.getId())
                .nombres(a.getNombres())
                .apellidos(a.getApellidos())
                .identificacion(a.getIdentificacion())
                .tipoPersona(a.getTipoPersona().name())
                .correo(a.getCorreo())
                .pais(a.getPais())
                .ciudad(a.getCiudad())
                .telefono(a.getTelefono())
                .documentos(a.getDocumentos())
                .status(a.getStatus().name())
                .motivoRechazo(a.getMotivoRechazo())
                .fechaSolicitud(a.getFechaSolicitud() != null ? a.getFechaSolicitud().toString() : null)
                .fechaDecision(a.getFechaDecision() != null ? a.getFechaDecision().toString() : null)
                .sellerId(a.getId())
                .build();
    }
}