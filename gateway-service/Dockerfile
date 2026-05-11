# =============================================================================
# Dockerfile — gateway-service E-Commerce Konrad
#
# NOTA IMPORTANTE: Spring Cloud Gateway es reactivo (Netty, no Tomcat).
# El proceso de build es identico al de los otros servicios (Maven → JAR).
# La diferencia esta en el pom.xml: usa spring-cloud-starter-gateway
# en lugar de spring-boot-starter-web.
#
# Uso
#   docker build -t konrad/gateway-service:1.0.0 .
#   docker run -p 8080:8080 konrad/gateway-service:1.0.0
#
# En Azure: este es el UNICO servicio con ingress EXTERNO.
#   az containerapp create \
#     --name gateway-service \
#     --ingress external \          <-- publico
#     --target-port 8080 \
#     --env-vars JWT_SECRET=<valor-real>
#
# Todos los demas servicios se crean con --ingress internal.
# =============================================================================


# -----------------------------------------------------------------------------
# STAGE 1 — Build
# -----------------------------------------------------------------------------
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# Cache de dependencias: si pom.xml no cambia, Docker reutiliza la capa
COPY pom.xml .
RUN mvn dependency:go-offline --no-transfer-progress -q

COPY src ./src
RUN mvn package -DskipTests --no-transfer-progress -q


# -----------------------------------------------------------------------------
# STAGE 2 — Runtime
# Solo JRE Alpine — imagen minima (~180 MB)
# -----------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine AS runtime

LABEL org.opencontainers.image.title="E-Commerce Konrad — Gateway"
LABEL org.opencontainers.image.vendor="Fundacion Universitaria Konrad Lorenz"
LABEL org.opencontainers.image.version="1.0.0"

WORKDIR /app

# Seguridad: usuario no-root (requerimiento de buenas practicas de Azure)
RUN addgroup -S konrad && adduser -S konrad -G konrad

COPY --from=builder /build/target/*.jar app.jar
RUN chown konrad:konrad app.jar
USER konrad

EXPOSE 8080

# =============================================================================
# Variables de entorno — valores por defecto (Azure format: sin :puerto)
#
# El docker-compose.yml sobreescribe estas con http://servicio:8080
# Azure Container Apps las usa tal cual (el DNS interno resuelve sin puerto)
# =============================================================================
ENV PORT=8080
ENV SPRING_PROFILES_ACTIVE=mock

ENV JAVA_OPTS="\
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -Djava.security.egd=file:/dev/./urandom \
  -Dspring.profiles.active=mock"

# JWT — DEBE sobreescribirse con la misma clave que usa auth-service en prod
ENV JWT_SECRET=konrad-mock-secret-key-solo-desarrollo

# URLs internas — formato Azure (sin puerto)
# docker-compose las sobreescribe con :8080
ENV AUTH_SERVICE_URL=http://auth-service
ENV SELLER_SERVICE_URL=http://seller-service
ENV PRODUCT_SERVICE_URL=http://product-service
ENV ORDER_SERVICE_URL=http://order-service
ENV PAYMENT_SERVICE_URL=http://payment-service
ENV BUYER_SERVICE_URL=http://buyer-service
ENV BAM_SERVICE_URL=http://bam-service
# credit-check y notification NO se exponen por el gateway (internos puros)

# Health check — el gateway tambien expone /actuator/health
HEALTHCHECK \
  --start-period=40s \
  --interval=30s \
  --timeout=5s \
  --retries=3 \
  CMD wget -qO- http://localhost:${PORT}/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]