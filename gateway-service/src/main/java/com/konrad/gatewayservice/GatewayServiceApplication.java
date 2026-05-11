package com.konrad.gatewayservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del API Gateway.
 *
 * IMPORTANTE: Spring Cloud Gateway es reactivo (usa Netty, NO Tomcat).
 * Por eso el pom.xml NO incluye spring-boot-starter-web.
 * Si lo agregas, Spring intenta levantar dos servidores y falla al arrancar.
 *
 * El gateway se configura completamente en application.yml:
 *   - Rutas hacia cada microservicio (spring.cloud.gateway.routes)
 *   - Filtro JWT (JwtAuthFilter) aplicado a rutas protegidas
 *   - CORS global para el frontend
 */
@SpringBootApplication
public class GatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }
}