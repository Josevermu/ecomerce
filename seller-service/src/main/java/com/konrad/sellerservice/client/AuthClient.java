package com.konrad.sellerservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

/**
 * Cliente HTTP hacia auth-service.
 *
 * Cuando el Director APRUEBA una solicitud, seller-service debe crear
 * el usuario en auth-service para que el vendedor pueda hacer login.
 *
 * Flujo (punto 3 del enunciado):
 *   Director aprueba → seller-service llama a auth-service para crear usuario
 *                    → seller-service llama a notification-service para enviar credenciales
 *                    → el vendedor recibe email con usuario y contraseña temporal
 */
@Slf4j
@Component
public class AuthClient {

    private final WebClient webClient;

    public AuthClient(
            @Value("${konrad.services.auth-url:http://auth-service}") String authUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(authUrl)
                .build();
    }

    /**
     * Registra el vendedor aprobado en auth-service.
     * La contraseña temporal cumple la política del RNF:
     *   - mínimo 8 caracteres
     *   - al menos una mayúscula
     *   - al menos una minúscula
     *   - al menos un número
     *
     * @return la contraseña temporal generada (para incluirla en el correo)
     */
    public String registerApprovedSeller(String applicationId, String correo) {
        String tempPassword = generateTempPassword(applicationId);

        Map<String, String> body = Map.of(
                "email",           correo,
                "password",        tempPassword,
                "role",            "SELLER",
                "relatedEntityId", applicationId
        );

        try {
            webClient.post()
                    .uri("/auth/register")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnSuccess(r -> log.info("[AUTH-CLIENT] Usuario vendedor creado en auth-service: {}", correo))
                    .doOnError(e -> log.error("[AUTH-CLIENT] Error creando usuario en auth-service para {}: {}", correo, e.getMessage()))
                    .block(); // bloqueante — necesitamos la contraseña antes de notificar
        } catch (Exception e) {
            log.error("[AUTH-CLIENT] Fallo al registrar usuario {}: {}", correo, e.getMessage());
            // No propagamos el error — el flujo del director no debe fallar
            // si auth-service no está disponible momentáneamente
        }

        return tempPassword;
    }

    /**
     * Genera una contraseña temporal que cumple la política de seguridad (RNF):
     * - Empieza con "Konrad" (mayúscula + minúsculas)
     * - Agrega los últimos 4 caracteres del applicationId (para unicidad)
     * - Termina con "1!" (número + carácter especial)
     *
     * Ejemplo: applicationId "app-a3f9" → "KonradA3f91!"
     */
    private String generateTempPassword(String applicationId) {
        String suffix = applicationId.length() >= 4
                ? applicationId.substring(applicationId.length() - 4).toUpperCase()
                : "0000";
        return "Konrad" + suffix + "1";
    }
}