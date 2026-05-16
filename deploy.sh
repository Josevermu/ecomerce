#!/bin/bash
# =============================================================================
# deploy.sh — E-Commerce Konrad
# Despliega los 10 microservicios en Azure Container Apps con UN solo comando.
#
# PREREQUISITOS (instalar una sola vez):
#   1. Azure CLI  →  https://aka.ms/installazurecliwindows  (Windows)
#                    brew install azure-cli                  (Mac)
#   2. Estar autenticado: az login
#
# NO necesitas Docker corriendo — las imágenes se construyen en la nube (az acr build).
#
# USO:
#   chmod +x deploy.sh
#   ./deploy.sh
#
# En Windows usar Git Bash o WSL:
#   bash deploy.sh
# =============================================================================

set -e  # Detiene el script si cualquier comando falla

# =============================================================================
#  ▼▼▼  CONFIGURA ESTAS 3 VARIABLES — ES LO ÚNICO QUE DEBES CAMBIAR  ▼▼▼
# =============================================================================

# Nombre del Resource Group (agrupa todos los recursos, puedes inventar uno)
RESOURCE_GROUP="konrad-rg"

# Nombre del Container Registry — SOLO minúsculas y números, sin guiones ni espacios
# DEBE ser único en todo Azure (si falla, cambia el nombre)
ACR_NAME="konradregistryecoa"

# Secreto JWT — cualquier string largo y seguro. auth-service y gateway lo comparten.
# IMPORTANTE: usar el mismo valor en ambos servicios.
JWT_SECRET="konrad-local-secret-key-2024-ecommerce"

# =============================================================================
#  ▲▲▲  FIN DE CONFIGURACIÓN  ▲▲▲
# =============================================================================

# Variables internas (no tocar)
LOCATION="eastus"
ENVIRONMENT_NAME="konrad-env"
VERSION="1.0.0"
ACR_SERVER="${ACR_NAME}.azurecr.io"

# Colores para output legible
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log()  { echo -e "${BLUE}[$(date +%H:%M:%S)]${NC} $1"; }
ok()   { echo -e "${GREEN}[OK]${NC} $1"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
fail() { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

echo ""
echo "╔══════════════════════════════════════════════════╗"
echo "║   E-Commerce Konrad — Deploy a Azure             ║"
echo "║   Resource Group : $RESOURCE_GROUP               "
echo "║   ACR            : $ACR_SERVER                   "
echo "╚══════════════════════════════════════════════════╝"
echo ""

# ─── PASO 0: Verificar que Azure CLI está instalado y autenticado ─────────────
log "Verificando Azure CLI..."
az account show > /dev/null 2>&1 || fail "No estás autenticado. Ejecuta: az login"
SUBSCRIPTION=$(az account show --query name -o tsv)
ok "Autenticado en suscripción: $SUBSCRIPTION"

# ─── PASO 1: Resource Group ───────────────────────────────────────────────────
log "Creando Resource Group '$RESOURCE_GROUP' en '$LOCATION'..."
az group create \
  --name "$RESOURCE_GROUP" \
  --location "$LOCATION" \
  --output none
ok "Resource Group listo"

# ─── PASO 2: Azure Container Registry ────────────────────────────────────────
log "Creando Azure Container Registry '$ACR_NAME'..."
az acr create \
  --resource-group "$RESOURCE_GROUP" \
  --name "$ACR_NAME" \
  --sku Basic \
  --admin-enabled true \
  --output none
ok "ACR listo: $ACR_SERVER"

# ─── PASO 3: Container Apps Environment (red privada) ────────────────────────
log "Creando Container Apps Environment '$ENVIRONMENT_NAME'..."
az containerapp env create \
  --name "$ENVIRONMENT_NAME" \
  --resource-group "$RESOURCE_GROUP" \
  --location "$LOCATION" \
  --output none
ok "Environment (red privada) listo"

# ─── PASO 4: Build de imágenes en la nube (az acr build) ─────────────────────
# az acr build envía el código fuente a Azure y construye la imagen allá.
# No necesitas Docker corriendo localmente.
# Cada servicio usa su propia carpeta como contexto de build.

SERVICES=(
  "auth-service"
  "seller-service"
  "product-service"
  "order-service"
  "payment-service"
  "notification-service"
  "credit-check-service"
  "buyer-service"
  "bam-service"
  "gateway-service"
)

log "Construyendo y subiendo imágenes al ACR..."
echo "  (esto tarda ~15-20 min la primera vez, las siguientes son más rápidas)"
echo ""

for SERVICE in "${SERVICES[@]}"; do

  log "  Building local image: $SERVICE..."

  docker build \
    -t "$ACR_SERVER/$SERVICE:$VERSION" \
    -f "./$SERVICE/Dockerfile" \
    "./$SERVICE"

  log "  Pushing image: $SERVICE..."

  docker push "$ACR_SERVER/$SERVICE:$VERSION"

  ok "  $SERVICE → $ACR_SERVER/$SERVICE:$VERSION"

done

echo ""
ok "Todas las imágenes construidas y subidas al ACR"
echo ""

# ─── PASO 5: Desplegar servicios INTERNOS (ingress: internal) ─────────────────
# Estos servicios NO son accesibles desde internet.
# Solo se comunican entre sí dentro del Environment por nombre de app.
# URL interna en Azure: http://{nombre-app}  (sin puerto, Azure lo maneja)

log "Desplegando servicios internos..."

# ── auth-service ──────────────────────────────────────────────────────────────
log "  Desplegando auth-service..."
az containerapp create \
  --name auth-service \
  --resource-group "$RESOURCE_GROUP" \
  --environment "$ENVIRONMENT_NAME" \
  --image "$ACR_SERVER/auth-service:$VERSION" \
  --registry-server "$ACR_SERVER" \
  --ingress internal \
  --target-port 8080 \
  --min-replicas 1 --max-replicas 3 \
  --env-vars \
    PORT=8080 \
    SPRING_PROFILES_ACTIVE=mock \
    "JWT_SECRET=$JWT_SECRET" \
  --output none
ok "  auth-service desplegado"

# ── notification-service ──────────────────────────────────────────────────────
# Se despliega antes que seller y buyer porque dependen de él
log "  Desplegando notification-service..."
az containerapp create \
  --name notification-service \
  --resource-group "$RESOURCE_GROUP" \
  --environment "$ENVIRONMENT_NAME" \
  --image "$ACR_SERVER/notification-service:$VERSION" \
  --registry-server "$ACR_SERVER" \
  --ingress internal \
  --target-port 8080 \
  --min-replicas 1 --max-replicas 2 \
  --env-vars \
    PORT=8080 \
    SPRING_PROFILES_ACTIVE=mock \
    SMTP_HOST=mock-smtp-no-disponible \
  --output none
ok "  notification-service desplegado"

# ── credit-check-service ──────────────────────────────────────────────────────
# Se despliega antes que seller porque seller lo llama
log "  Desplegando credit-check-service..."
az containerapp create \
  --name credit-check-service \
  --resource-group "$RESOURCE_GROUP" \
  --environment "$ENVIRONMENT_NAME" \
  --image "$ACR_SERVER/credit-check-service:$VERSION" \
  --registry-server "$ACR_SERVER" \
  --ingress internal \
  --target-port 8080 \
  --min-replicas 1 --max-replicas 2 \
  --env-vars \
    PORT=8080 \
    SPRING_PROFILES_ACTIVE=mock \
    DATACREDITO_URL=http://mock-datacredito-no-disponible \
    CIFIN_FTP_HOST=mock-cifin-no-disponible \
    POLICIA_URL=http://mock-policia-no-disponible \
  --output none
ok "  credit-check-service desplegado"

# ── product-service ───────────────────────────────────────────────────────────
log "  Desplegando product-service..."
az containerapp create \
  --name product-service \
  --resource-group "$RESOURCE_GROUP" \
  --environment "$ENVIRONMENT_NAME" \
  --image "$ACR_SERVER/product-service:$VERSION" \
  --registry-server "$ACR_SERVER" \
  --ingress internal \
  --target-port 8080 \
  --min-replicas 1 --max-replicas 5 \
  --env-vars \
    PORT=8080 \
    SPRING_PROFILES_ACTIVE=mock \
  --output none
ok "  product-service desplegado"

# ── payment-service ───────────────────────────────────────────────────────────
log "  Desplegando payment-service..."
az containerapp create \
  --name payment-service \
  --resource-group "$RESOURCE_GROUP" \
  --environment "$ENVIRONMENT_NAME" \
  --image "$ACR_SERVER/payment-service:$VERSION" \
  --registry-server "$ACR_SERVER" \
  --ingress internal \
  --target-port 8080 \
  --min-replicas 1 --max-replicas 3 \
  --env-vars \
    PORT=8080 \
    SPRING_PROFILES_ACTIVE=mock \
    PSE_INTERMEDIARY_URL=http://mock-banco-no-disponible \
    PAYMENT_GATEWAY_KEY=mock-gateway-key \
    BANK_FTP_HOST=mock-bank-ftp-no-disponible \
  --output none
ok "  payment-service desplegado"

# ── seller-service ────────────────────────────────────────────────────────────
log "  Desplegando seller-service..."
az containerapp create \
  --name seller-service \
  --resource-group "$RESOURCE_GROUP" \
  --environment "$ENVIRONMENT_NAME" \
  --image "$ACR_SERVER/seller-service:$VERSION" \
  --registry-server "$ACR_SERVER" \
  --ingress internal \
  --target-port 8080 \
  --min-replicas 1 --max-replicas 3 \
  --env-vars \
    PORT=8080 \
    SPRING_PROFILES_ACTIVE=mock \
    AUTH_SERVICE_URL=http://auth-service \
    NOTIFICATION_SERVICE_URL=http://notification-service \
    CREDIT_CHECK_SERVICE_URL=http://credit-check-service \
  --output none
ok "  seller-service desplegado"

# ── buyer-service ─────────────────────────────────────────────────────────────
log "  Desplegando buyer-service..."
az containerapp create \
  --name buyer-service \
  --resource-group "$RESOURCE_GROUP" \
  --environment "$ENVIRONMENT_NAME" \
  --image "$ACR_SERVER/buyer-service:$VERSION" \
  --registry-server "$ACR_SERVER" \
  --ingress internal \
  --target-port 8080 \
  --min-replicas 1 --max-replicas 3 \
  --env-vars \
    PORT=8080 \
    SPRING_PROFILES_ACTIVE=mock \
    AUTH_SERVICE_URL=http://auth-service \
    NOTIFICATION_SERVICE_URL=http://notification-service \
  --output none
ok "  buyer-service desplegado"

# ── order-service ─────────────────────────────────────────────────────────────
log "  Desplegando order-service..."
az containerapp create \
  --name order-service \
  --resource-group "$RESOURCE_GROUP" \
  --environment "$ENVIRONMENT_NAME" \
  --image "$ACR_SERVER/order-service:$VERSION" \
  --registry-server "$ACR_SERVER" \
  --ingress internal \
  --target-port 8080 \
  --min-replicas 1 --max-replicas 5 \
  --env-vars \
    PORT=8080 \
    SPRING_PROFILES_ACTIVE=mock \
    PRODUCT_SERVICE_URL=http://product-service \
    PAYMENT_SERVICE_URL=http://payment-service \
    NOTIFICATION_SERVICE_URL=http://notification-service \
    SELLER_SERVICE_URL=http://seller-service \
  --output none
ok "  order-service desplegado"

# ── bam-service ───────────────────────────────────────────────────────────────
log "  Desplegando bam-service..."
az containerapp create \
  --name bam-service \
  --resource-group "$RESOURCE_GROUP" \
  --environment "$ENVIRONMENT_NAME" \
  --image "$ACR_SERVER/bam-service:$VERSION" \
  --registry-server "$ACR_SERVER" \
  --ingress internal \
  --target-port 8080 \
  --min-replicas 1 --max-replicas 2 \
  --env-vars \
    PORT=8080 \
    SPRING_PROFILES_ACTIVE=mock \
    ORDER_SERVICE_URL=http://order-service \
    SELLER_SERVICE_URL=http://seller-service \
    PRODUCT_SERVICE_URL=http://product-service \
  --output none
ok "  bam-service desplegado"

# ─── PASO 6: GATEWAY — único con ingress EXTERNO ─────────────────────────────
echo ""
log "Desplegando gateway-service (PÚBLICO — el único expuesto a internet)..."
az containerapp create \
  --name gateway-service \
  --resource-group "$RESOURCE_GROUP" \
  --environment "$ENVIRONMENT_NAME" \
  --image "$ACR_SERVER/gateway-service:$VERSION" \
  --registry-server "$ACR_SERVER" \
  --ingress external \
  --target-port 8080 \
  --min-replicas 1 --max-replicas 5 \
  --env-vars \
    PORT=8080 \
    SPRING_PROFILES_ACTIVE=mock \
    "JWT_SECRET=$JWT_SECRET" \
    AUTH_SERVICE_URL=http://auth-service \
    SELLER_SERVICE_URL=http://seller-service \
    PRODUCT_SERVICE_URL=http://product-service \
    ORDER_SERVICE_URL=http://order-service \
    PAYMENT_SERVICE_URL=http://payment-service \
    BUYER_SERVICE_URL=http://buyer-service \
    BAM_SERVICE_URL=http://bam-service \
  --output none
ok "gateway-service desplegado"

# ─── PASO 7: Obtener URL pública del gateway ──────────────────────────────────
GATEWAY_FQDN=$(az containerapp show \
  --name gateway-service \
  --resource-group "$RESOURCE_GROUP" \
  --query "properties.configuration.ingress.fqdn" \
  -o tsv)

GATEWAY_URL="https://$GATEWAY_FQDN"

echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║                  DESPLIEGUE COMPLETADO                      ║"
echo "╠══════════════════════════════════════════════════════════════╣"
echo "║                                                              ║"
echo "║  URL PÚBLICA DEL GATEWAY:                                    ║"
echo "║  $GATEWAY_URL"
echo "║                                                              ║"
echo "║  Esta es la ÚNICA URL que usas desde Postman y el frontend.  ║"
echo "║  Los otros 9 servicios son invisibles desde internet.        ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
echo "  Verifica que todo está corriendo:"
echo "  curl $GATEWAY_URL/actuator/health"
echo ""
echo "  Primer login (Director):"
echo "  curl -X POST $GATEWAY_URL/auth/login \\"
echo "    -H 'Content-Type: application/json' \\"
echo "    -d '{\"email\":\"director@konrad.com\",\"password\":\"Director123\"}'"
echo ""
echo "  En Postman, cambia BASE_URL a:"
echo "  $GATEWAY_URL"
echo ""