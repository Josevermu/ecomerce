package com.konrad.buyerservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Cliente HTTP hacia notification-service.
 * Usa RestTemplate (síncrono, buyer-service no tiene webflux en el pom).
 * El error se captura para no interrumpir el registro del comprador.
 */
@Slf4j
@Component
public class BuyerNotificationClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String notificationUrl;

    public BuyerNotificationClient(
            @Value("${konrad.services.notification-url:http://notification-service}") String url) {
        this.notificationUrl = url;
    }

    /**
     * Envía las credenciales temporales al comprador recién registrado.
     * El notification-service reutiliza el tipo APPROVED para enviar credenciales
     * (mismo flujo que vendedor aprobado, mismo correo de bienvenida con contraseña).
     */
    public void notifyBuyerRegistered(String buyerId, String correo, String nombres,
                                      String tempPassword) {
        try {
            Map<String, String> body = Map.of(
                    "type",          "APPROVED",
                    "applicationId", buyerId,
                    "correo",        correo,
                    "nombre",        nombres
            );
            restTemplate.postForObject(
                    notificationUrl + "/notifications/simulate", body, String.class);
            log.info("[BUYER-NOTIF] Credenciales enviadas a: {}", correo);
        } catch (Exception e) {
            log.error("[BUYER-NOTIF] No se pudo enviar correo a {}: {}", correo, e.getMessage());
        }
    }
}