package com.konrad.buyerservice.controller;

import com.konrad.buyerservice.dto.BuyerDtos.*;
import com.konrad.buyerservice.service.BuyerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/buyers")
@RequiredArgsConstructor
public class BuyerController {

    private final BuyerService buyerService;

    /**
     * POST /buyers/register — registro público de comprador (punto 7).
     * No requiere autenticación previa.
     * El sistema envía las credenciales por correo electrónico.
     */
    @PostMapping("/register")
    public ResponseEntity<BuyerRegistrationResponse> register(
            @RequestBody BuyerRegistrationRequest request) {
        return ResponseEntity.ok(buyerService.register(request));
    }

    /** GET /buyers/{id} — perfil del comprador (propio o ADMIN) */
    @GetMapping("/{id}")
    public ResponseEntity<BuyerProfileResponse> getById(@PathVariable String id) {
        return ResponseEntity.ok(buyerService.findById(id));
    }

    /** GET /buyers/by-email?email=... — buscar por correo (ADMIN) */
    @GetMapping("/by-email")
    public ResponseEntity<BuyerProfileResponse> getByEmail(@RequestParam String email) {
        return ResponseEntity.ok(buyerService.findByEmail(email));
    }

    /** GET /buyers — todos los compradores (solo ADMIN) */
    @GetMapping
    public ResponseEntity<List<BuyerProfileResponse>> getAll() {
        return ResponseEntity.ok(buyerService.findAll());
    }

    /**
     * PATCH /buyers/{id} — actualizar datos modificables del perfil.
     * El comprador solo puede cambiar: ciudad, dirección, teléfono, redes sociales.
     * El correo y la identificación no son modificables.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<BuyerProfileResponse> update(
            @PathVariable String id,
            @RequestBody BuyerUpdateRequest request) {
        return ResponseEntity.ok(buyerService.update(id, request));
    }

    /** DELETE /buyers/{id} — desactivar cuenta (ADMIN o el propio comprador) */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable String id) {
        buyerService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}