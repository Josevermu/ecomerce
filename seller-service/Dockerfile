# =============================================================================
# Dockerfile estandar — Microservicios E-Commerce Konrad
# Valido para los 9 servicios: auth, seller, product, order,
# payment, notification, credit-check, buyer, bam
#
# Uso:
#   docker build -t konrad/nombre-servicio:1.0.0 .
#   docker run -p 8080:8080 konrad/nombre-servicio:1.0.0
#
# NOTA: Todos los datos son mock (ConcurrentHashMap).
# Las variables de entorno de servicios externos (Policia, Datacredito,
# CIFIN, bancos) tienen valores dummy por defecto y el codigo los ignora
# cuando konrad.mock.enabled=true esta activo.
# =============================================================================


# -----------------------------------------------------------------------------
# STAGE 1 — Build
# Usa la imagen oficial de Maven con JDK 17 Alpine (imagen ligera)
# Se descarta completamente despues del build; no llega al runtime
# -----------------------------------------------------------------------------
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# 1. Copia solo el pom.xml primero.
#    Truco de cache: si el pom.xml no cambio, Docker reutiliza la capa
#    de dependencias descargadas y no vuelve a bajarlas de internet.
COPY pom.xml .

# 2. Pre-descarga todas las dependencias en modo offline.
#    Solo se re-ejecuta cuando pom.xml cambia.
RUN mvn dependency:go-offline --no-transfer-progress -q

# 3. Copia el codigo fuente (se hace DESPUES del paso de dependencias
#    para maximizar el uso de cache cuando solo cambio el codigo).
COPY src ./src

# 4. Compila y empaqueta el JAR.
#    -DskipTests : los tests se corren en el pipeline de CI, no aqui.
#    -q          : modo silencioso, logs mas limpios.
RUN mvn package -DskipTests --no-transfer-progress -q


# -----------------------------------------------------------------------------
# STAGE 2 — Runtime
# Solo el JRE (no el JDK completo), Alpine para imagen minima.
# Resultado tipico: ~180 MB vs ~600 MB con la imagen full.
# -----------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-alpine AS runtime

# Metadatos de la imagen
LABEL org.opencontainers.image.title="E-Commerce Konrad Microservicio"
LABEL org.opencontainers.image.vendor="Fundacion Universitaria Konrad Lorenz"
LABEL org.opencontainers.image.version="1.0.0"

WORKDIR /app

# -- Seguridad: usuario no-root -----------------------------------------------
# Los containers que corren como root son un riesgo de seguridad.
# Azure Container Apps tambien recomienda esto como buena practica.
RUN addgroup -S konrad && adduser -S konrad -G konrad

# -- Copia el JAR desde el stage de build -------------------------------------
# El wildcard *.jar funciona independientemente del nombre del artefacto,
# por eso este Dockerfile es igual para todos los servicios.
COPY --from=builder /build/target/*.jar app.jar

# Asigna el JAR al usuario no-root
RUN chown konrad:konrad app.jar

# Cambia al usuario no-root antes de ejecutar
USER konrad

# -- Puerto -------------------------------------------------------------------
# Azure Container Apps usa 8080 como target port por defecto.
# La variable ${PORT} permite que Azure la sobreescriba si es necesario.
EXPOSE 8080

# =============================================================================
# Variables de entorno
#
# Todas tienen valores "dummy" seguros por defecto.
# El codigo los ignora porque konrad.mock.enabled=true hace que
# cada servicio use sus implementaciones mock internas (ConcurrentHashMap).
#
# En Azure Container Apps: configura las que necesites reales desde
# el portal o con: az containerapp update --set-env-vars
# =============================================================================
ENV PORT=8080

# Activa el perfil mock de Spring Boot
ENV SPRING_PROFILES_ACTIVE=mock

# -- JVM: optimizacion para correr dentro de un container --------------------
# +UseContainerSupport  : detecta los limites de memoria del container
#                         (activo por defecto en Java 17, pero se pone explicito)
# MaxRAMPercentage=75.0 : usa maximo 75% de la RAM asignada al container,
#                         deja margen para el OS y otros procesos
# urandom               : generador de numeros aleatorios mas rapido,
#                         evita bloqueos en el arranque del servicio
ENV JAVA_OPTS="\
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -Djava.security.egd=file:/dev/./urandom \
  -Dspring.profiles.active=mock"

# -- Variables dummy para servicios externos (nunca se usan en modo mock) ----
ENV JWT_SECRET=konrad-mock-secret-key-solo-desarrollo

# URLs internas dentro del Azure Container Apps Environment
# En Azure se resuelven por nombre de app (DNS interno del environment)
ENV NOTIFICATION_SERVICE_URL=http://notification-service
ENV AUTH_SERVICE_URL=http://auth-service
ENV SELLER_SERVICE_URL=http://seller-service
ENV PRODUCT_SERVICE_URL=http://product-service
ENV ORDER_SERVICE_URL=http://order-service
ENV PAYMENT_SERVICE_URL=http://payment-service
ENV CREDIT_CHECK_SERVICE_URL=http://credit-check-service
ENV BUYER_SERVICE_URL=http://buyer-service
ENV BAM_SERVICE_URL=http://bam-service

# Integraciones externas reales — IGNORADAS en modo mock
# La Policia, Datacredito y CIFIN apuntan a URLs dummy porque
# el codigo usa datos simulados (ConcurrentHashMap) en su lugar
ENV DATACREDITO_URL=http://mock-datacredito-no-disponible
ENV DATACREDITO_KEY=mock-key
ENV CIFIN_FTP_HOST=mock-cifin-no-disponible
ENV CIFIN_FTP_USER=mock-user
ENV CIFIN_FTP_PASS=mock-pass
ENV POLICIA_URL=http://mock-policia-no-disponible
ENV PSE_INTERMEDIARY_URL=http://mock-banco-no-disponible
ENV PAYMENT_GATEWAY_KEY=mock-gateway-key
ENV BANK_FTP_HOST=mock-bank-ftp-no-disponible
ENV SMTP_HOST=mock-smtp-no-disponible
ENV SMTP_USER=mock@mock.com
ENV SMTP_PASS=mock-pass

# -- Health check -------------------------------------------------------------
# Azure Container Apps sondea este endpoint para saber si el servicio
# esta listo para recibir trafico (liveness + readiness probe).
# Requiere spring-boot-starter-actuator en el pom.xml (ya incluido).
# --start-period: espera 30s antes del primer intento (tiempo de arranque)
# --interval    : verifica cada 30s
# --timeout     : falla si no responde en 5s
# --retries     : 3 fallos consecutivos = container unhealthy
HEALTHCHECK \
  --start-period=30s \
  --interval=30s \
  --timeout=5s \
  --retries=3 \
  CMD wget -qO- http://localhost:${PORT}/actuator/health | grep -q '"status":"UP"' || exit 1

# -- Punto de entrada ---------------------------------------------------------
# Se usa "exec" form con sh -c para que JAVA_OPTS se expanda correctamente
# y para que las senales del OS (SIGTERM) lleguen al proceso Java,
# permitiendo un shutdown limpio (graceful shutdown).
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
