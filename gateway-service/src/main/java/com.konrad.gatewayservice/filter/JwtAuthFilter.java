package com.konrad.gatewayservice.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Filtro JWT del API Gateway.
 *
 * Se aplica a todas las rutas que tienen:
 *   filters:
 *     - name: JwtAuthFilter
 *
 * El filtro:
 *  1. Extrae el header Authorization: Bearer <token>
 *  2. Valida la firma del token con la misma clave que usa auth-service
 *  3. Si es valido, extrae userId y role y los propaga como headers
 *     internos (X-User-Id, X-User-Role) para que los microservicios
 *     sepan quien hace la peticion sin tener que volver a llamar a auth.
 *  4. Si no hay token o es invalido, responde 401 Unauthorized.
 *
 * SEGURIDAD: Los microservicios NO tienen que revalidar el JWT —
 * confian en que el gateway ya lo valido. La red interna del Azure
 * Container Apps Environment no es accesible desde internet, por lo
 * que no hay riesgo de inyectar X-User-Id desde afuera.
 */
@Slf4j
@Component
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {

    @Value("${konrad.jwt.secret}")
    private String jwtSecret;

    public JwtAuthFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest()
                    .getHeaders()
                    .getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("[GATEWAY] Peticion sin token JWT a: {}",
                        exchange.getRequest().getPath());
                return unauthorized(exchange);
            }

            String token = authHeader.substring(7);

            try {
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(Keys.hmacShaKeyFor(
                                jwtSecret.getBytes(StandardCharsets.UTF_8)))
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                String userId = claims.getSubject();
                String role   = claims.get("role", String.class);

                log.debug("[GATEWAY] JWT valido — userId={} role={} path={}",
                        userId, role, exchange.getRequest().getPath());

                // Propaga identidad al microservicio destino como headers internos
                ServerWebExchange mutated = exchange.mutate()
                        .request(r -> r
                                .header("X-User-Id",   userId)
                                .header("X-User-Role", role != null ? role : ""))
                        .build();

                return chain.filter(mutated);

            } catch (Exception e) {
                log.warn("[GATEWAY] JWT invalido: {}", e.getMessage());
                return unauthorized(exchange);
            }
        };
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    public static class Config {
        // Sin configuracion adicional por ahora
    }
}