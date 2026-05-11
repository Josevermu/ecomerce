package com.konrad.buyerservice.service;

import com.konrad.buyerservice.client.BuyerNotificationClient;
import com.konrad.buyerservice.dto.BuyerDtos.*;
import com.konrad.buyerservice.model.entity.Buyer;
import com.konrad.buyerservice.model.entity.BuyerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BuyerService {

    private final BuyerRepository repo;
    private final BuyerNotificationClient notificationClient;

    // ─── Punto 7: Registro de comprador ──────────────────────────────────────

    public BuyerRegistrationResponse register(BuyerRegistrationRequest request) {
        if (repo.existsByEmail(request.getCorreo())) {
            throw new RuntimeException("El correo ya está registrado: " + request.getCorreo());
        }

        Buyer buyer = Buyer.builder()
                .nombres(request.getNombres())
                .apellidos(request.getApellidos())
                .identificacion(request.getIdentificacion())
                .correo(request.getCorreo())
                .pais(request.getPais())
                .ciudad(request.getCiudad())
                .direccion(request.getDireccion())
                .telefono(request.getTelefono())
                .twitter(request.getTwitter())
                .instagram(request.getInstagram())
                .activo(true)
                .creadoEn(LocalDateTime.now())
                .build();

        Buyer saved = repo.save(buyer);
        log.info("[BUYER] Comprador registrado: {} — {}", saved.getId(), saved.getCorreo());

        // Generar contraseña temporal (RNF: mínimo 8 chars, mayúscula, minúscula y número)
        String tempPassword = "Buyer" + (int)(Math.random() * 9000 + 1000) + "!";

        // Notificar al notification-service para envío de credenciales por correo
        notificationClient.notifyBuyerRegistered(
                saved.getId(), saved.getCorreo(),
                saved.getNombres() + " " + saved.getApellidos(),
                tempPassword
        );

        return BuyerRegistrationResponse.builder()
                .buyerId(saved.getId())
                .correo(saved.getCorreo())
                .mensaje("Registro exitoso. Revisa tu correo para obtener tus credenciales de acceso.")
                .build();
    }

    // ─── Consultas y actualización de perfil ──────────────────────────────────

    public BuyerProfileResponse findById(String id) {
        return toProfile(repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Comprador no encontrado: " + id)));
    }

    public BuyerProfileResponse findByEmail(String email) {
        return toProfile(repo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Comprador no encontrado: " + email)));
    }

    public List<BuyerProfileResponse> findAll() {
        return repo.findAll().stream().map(this::toProfile).collect(Collectors.toList());
    }

    public BuyerProfileResponse update(String id, BuyerUpdateRequest request) {
        Buyer buyer = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Comprador no encontrado: " + id));

        if (request.getCiudad()    != null) buyer.setCiudad(request.getCiudad());
        if (request.getDireccion() != null) buyer.setDireccion(request.getDireccion());
        if (request.getTelefono()  != null) buyer.setTelefono(request.getTelefono());
        if (request.getTwitter()   != null) buyer.setTwitter(request.getTwitter());
        if (request.getInstagram() != null) buyer.setInstagram(request.getInstagram());

        return toProfile(repo.save(buyer));
    }

    public void deactivate(String id) {
        Buyer buyer = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Comprador no encontrado: " + id));
        buyer.setActivo(false);
        repo.save(buyer);
        log.info("[BUYER] Comprador desactivado: {}", id);
    }

    // ─── Mapper ──────────────────────────────────────────────────────────────

    private BuyerProfileResponse toProfile(Buyer b) {
        return BuyerProfileResponse.builder()
                .id(b.getId())
                .nombres(b.getNombres()).apellidos(b.getApellidos())
                .identificacion(b.getIdentificacion())
                .correo(b.getCorreo())
                .pais(b.getPais()).ciudad(b.getCiudad())
                .direccion(b.getDireccion()).telefono(b.getTelefono())
                .twitter(b.getTwitter()).instagram(b.getInstagram())
                .creadoEn(b.getCreadoEn() != null ? b.getCreadoEn().toString() : "")
                .build();
    }
}