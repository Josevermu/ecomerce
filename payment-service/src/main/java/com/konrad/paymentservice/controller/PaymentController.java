package com.konrad.paymentservice.controller;

import com.konrad.paymentservice.dto.PaymentDtos.*;
import com.konrad.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * POST /payments/process
     * Body incluye metodoPago: "PSE" | "CREDIT_CARD" | "CONSIGNATION"
     * El servicio selecciona automáticamente la estrategia correcta.
     */
    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> process(@RequestBody PaymentRequest request) {
        return ResponseEntity.ok(paymentService.processPayment(request));
    }

    /** GET /payments/entity/{entityId} — historial de pagos de una entidad */
    @GetMapping("/entity/{entityId}")
    public ResponseEntity<List<PaymentResponse>> byEntity(@PathVariable String entityId) {
        return ResponseEntity.ok(paymentService.findByEntity(entityId));
    }

    /** GET /payments — todos los pagos (solo ADMIN/DIRECTOR) */
    @GetMapping
    public ResponseEntity<List<PaymentResponse>> all() {
        return ResponseEntity.ok(paymentService.findAll());
    }
}
