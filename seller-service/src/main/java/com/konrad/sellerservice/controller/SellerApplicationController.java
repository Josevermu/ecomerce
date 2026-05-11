package com.konrad.sellerservice.controller;

import com.konrad.sellerservice.dto.SellerDtos.*;
import com.konrad.sellerservice.service.SellerApplicationService;
import com.konrad.sellerservice.service.SellerEvents;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SellerApplicationController {

    private final SellerApplicationService service;

    // ─── Rutas públicas ───────────────────────────────────────────────────────

    /** POST /sellers/apply — cualquier persona puede enviar una solicitud (punto 1) */
    @PostMapping("/sellers/apply")
    public ResponseEntity<ApplicationSubmittedResponse> apply(
            @RequestBody SellerRegistrationRequest request) {
        return ResponseEntity.ok(service.submitApplication(request));
    }

    /** GET /sellers/status?q=... — consulta pública por número o identificación (punto 3) */
    @GetMapping("/sellers/status")
    public ResponseEntity<ApplicationDetailResponse> queryStatus(@RequestParam String q) {
        return ResponseEntity.ok(service.queryPublic(q));
    }

    // ─── Rutas del Director Comercial (requieren rol DIRECTOR via gateway) ────

    /** GET /sellers/applications — listado con filtros (punto 2) */
    @GetMapping("/sellers/applications")
    public ResponseEntity<List<ApplicationSummaryResponse>> list(
            @RequestParam(required = false) String identificacion,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta) {
        return ResponseEntity.ok(service.searchApplications(identificacion, status, desde, hasta));
    }

    /** GET /sellers/applications/{id} — detalle de una solicitud (punto 2) */
    @GetMapping("/sellers/applications/{id}")
    public ResponseEntity<ApplicationDetailResponse> detail(@PathVariable String id) {
        return ResponseEntity.ok(service.findById(id));
    }

    /** POST /sellers/applications/{id}/decision — Director registra APROBADA/RECHAZADA/DEVUELTA */
    @PostMapping("/sellers/applications/{id}/decision")
    public ResponseEntity<ApplicationDetailResponse> decide(
            @PathVariable String id,
            @RequestBody ApplicationDecisionRequest request) {
        return ResponseEntity.ok(service.registerDecision(id, request));
    }

    // ─── Ruta de vendedor ─────────────────────────────────────────────────────

    /** POST /sellers/{id}/rating — comprador califica al vendedor (punto 12) */
    @PostMapping("/sellers/{id}/rating")
    public ResponseEntity<Void> rate(@PathVariable String id, @RequestParam int rating) {
        service.updateRating(id, rating);
        return ResponseEntity.ok().build();
    }

    /** POST /sellers/{id}/activate — activar suscripción tras pago */
    @PostMapping("/sellers/{id}/activate")
    public ResponseEntity<Void> activate(@PathVariable String id,
                                          @RequestBody SubscriptionRequest request) {
        service.activateSubscription(new SellerEvents
            .SubscriptionPaymentConfirmed(this, id, request.getPaymentId(), request.getTipo()));
        return ResponseEntity.ok().build();
    }
}
