package com.konrad.creditcheckservice.controller;

import com.konrad.creditcheckservice.dto.CreditCheckDtos.*;
import com.konrad.creditcheckservice.model.entity.CIFINRepository;
import com.konrad.creditcheckservice.service.VendorVerificationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/credit-check")
@RequiredArgsConstructor
public class CreditCheckController {

    private final VendorVerificationFacade facade;
    private final CIFINRepository cifinRepo;

    /**
     * GET /credit-check/verify/{identificacion}
     *
     * FACADE: Una sola llamada → resultado de Datacrédito + CIFIN + Policía.
     * El Director solo necesita este endpoint — no conoce los 3 subsistemas.
     * Requerimiento punto 2 del documento.
     */
    @GetMapping("/verify/{identificacion}")
    public ResponseEntity<VerificationResult> verify(@PathVariable String identificacion) {
        return ResponseEntity.ok(facade.verify(identificacion));
    }

    /** GET /credit-check/cifin — consultar BD local de CIFIN (solo ADMIN) */
    @GetMapping("/cifin")
    public ResponseEntity<List<?>> cifinData() {
        return ResponseEntity.ok(cifinRepo.findAll());
    }
}
