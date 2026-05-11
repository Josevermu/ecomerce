package com.konrad.notificationservice.controller;

import com.konrad.notificationservice.model.entity.Notification;
import com.konrad.notificationservice.model.entity.NotificationRepository;
import com.konrad.notificationservice.service.NotificationListeners.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository repo;
    private final ApplicationEventPublisher eventPublisher;

    /** GET /notifications — historial de notificaciones (ADMIN/DIRECTOR) */
    @GetMapping
    public ResponseEntity<List<Notification>> all() {
        return ResponseEntity.ok(repo.findAll());
    }

    /** GET /notifications?email=... — notificaciones de un destinatario */
    @GetMapping("/by-email")
    public ResponseEntity<List<Notification>> byEmail(@RequestParam String email) {
        return ResponseEntity.ok(repo.findByDestinatario(email));
    }

    /**
     * POST /notifications/simulate — dispara un evento para probar los listeners.
     * Body: { "type": "APPROVED", "applicationId": "app-001", "correo": "...", ... }
     * Solo disponible en entorno de pruebas/mock.
     */
    @PostMapping("/simulate")
    public ResponseEntity<Map<String, String>> simulate(@RequestBody Map<String, String> body) {
        String type = body.getOrDefault("type", "");
        String appId = body.getOrDefault("applicationId", "app-test");
        String correo = body.getOrDefault("correo", "test@test.com");

        switch (type.toUpperCase()) {
            case "SUBMITTED" -> eventPublisher.publishEvent(
                new ApplicationSubmittedEvent(appId, correo, body.getOrDefault("nombre", "Usuario")));
            case "APPROVED" -> eventPublisher.publishEvent(
                new ApplicationApprovedEvent(appId, correo, body.getOrDefault("nombre", "Usuario")));
            case "REJECTED" -> eventPublisher.publishEvent(
                new ApplicationRejectedEvent(appId, correo, body.getOrDefault("motivo", "BAJA_DATACREDITO")));
            case "RETURNED" -> eventPublisher.publishEvent(
                new ApplicationReturnedEvent(appId, correo));
            case "SUBSCRIPTION_EXPIRED" -> eventPublisher.publishEvent(
                new SubscriptionExpiredEvent(body.getOrDefault("sellerId", "seller-001"), correo));
            case "SELLER_SUSPENDED" -> eventPublisher.publishEvent(
                new SellerSuspendedEvent(body.getOrDefault("sellerId", "seller-001"),
                    correo, body.getOrDefault("motivo", "LOW_AVERAGE")));
            default -> { return ResponseEntity.badRequest()
                .body(Map.of("error", "Tipo de evento desconocido: " + type)); }
        }

        return ResponseEntity.ok(Map.of("message", "Evento '" + type + "' disparado correctamente"));
    }
}
