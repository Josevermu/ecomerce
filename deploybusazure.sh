#!/bin/bash
# =============================================================================
# servicebus-setup.sh — E-Commerce Konrad
# Configura Azure Service Bus para los microservicios.
#
# PREREQUISITO: haber ejecutado deploy.sh primero.
#
# USO:
#   chmod +x servicebus-setup.sh
#   ./servicebus-setup.sh
# =============================================================================

set -e

# =============================================================================
# VARIABLES — deben coincidir exactamente con deploy.sh
# =============================================================================
RESOURCE_GROUP="konrad-rg"
LOCATION="eastus"
NAMESPACE="konrad-servicebus"

# Colores
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

log() { echo -e "${BLUE}[$(date +%H:%M:%S)]${NC} $1"; }
ok()  { echo -e "${GREEN}[OK]${NC} $1"; }

echo ""
echo "╔══════════════════════════════════════════════════╗"
echo "║   E-Commerce Konrad — Service Bus Setup          ║"
echo "║   Namespace : $NAMESPACE                         "
echo "║   Group     : $RESOURCE_GROUP                    "
echo "╚══════════════════════════════════════════════════╝"
echo ""

# =============================================================================
# PASO 1 — Crear el namespace
# =============================================================================
log "Creando namespace '$NAMESPACE'..."
az servicebus namespace create \
  --name "$NAMESPACE" \
  --resource-group "$RESOURCE_GROUP" \
  --location "$LOCATION" \
  --sku Standard \
  --output none
ok "Namespace listo"

# =============================================================================
# PASO 2 — Crear los 4 topics
# =============================================================================
log "Creando topics..."

for TOPIC in konrad-seller-events konrad-buyer-events konrad-payment-events konrad-order-events; do
  az servicebus topic create \
    --name "$TOPIC" \
    --namespace-name "$NAMESPACE" \
    --resource-group "$RESOURCE_GROUP" \
    --default-message-time-to-live P7D \
    --output none
done

ok "4 topics creados"

# =============================================================================
# PASO 3 — Crear las suscripciones
# =============================================================================
log "Creando suscripciones..."

create_sub() {
  SUB=$1
  TOPIC=$2

  az servicebus topic subscription create \
    --name "$SUB" \
    --topic-name "$TOPIC" \
    --namespace-name "$NAMESPACE" \
    --resource-group "$RESOURCE_GROUP" \
    --max-delivery-count 3 \
    --lock-duration PT30S \
    --enable-dead-lettering-on-message-expiration \
    --output none

  ok "  $SUB → $TOPIC"
}

# seller-events → notification
create_sub notification-sub konrad-seller-events

# buyer-events → notification
create_sub notification-sub konrad-buyer-events

# payment-events → seller + notification
create_sub seller-sub konrad-payment-events
create_sub notification-sub konrad-payment-events

# order-events → seller + bam
create_sub seller-sub konrad-order-events
create_sub bam-sub konrad-order-events

ok "Suscripciones creadas"

# =============================================================================
# PASO 4 — Obtener connection string e inyectarla en cada Container App
# =============================================================================
log "Obteniendo connection string..."

CONNECTION_STRING=$(az servicebus namespace authorization-rule keys list \
  --name RootManageSharedAccessKey \
  --namespace-name "$NAMESPACE" \
  --resource-group "$RESOURCE_GROUP" \
  --query primaryConnectionString \
  --output tsv)

ok "Connection string obtenida"

log "Configurando variable en los Container Apps..."

for SERVICE in seller-service buyer-service payment-service order-service notification-service; do
  az containerapp update \
    --name "$SERVICE" \
    --resource-group "$RESOURCE_GROUP" \
    --set-env-vars "AZURE_SERVICEBUS_CONNECTION_STRING=$CONNECTION_STRING" \
    --output none
  ok "  $SERVICE configurado"
done

# =============================================================================
# PASO 5 — Verificar
# =============================================================================
echo ""
log "Verificando topics..."
az servicebus topic list \
  --namespace-name "$NAMESPACE" \
  --resource-group "$RESOURCE_GROUP" \
  --query "[].name" \
  --output table

log "Verificando suscripciones de konrad-payment-events..."
az servicebus topic subscription list \
  --topic-name konrad-payment-events \
  --namespace-name "$NAMESPACE" \
  --resource-group "$RESOURCE_GROUP" \
  --query "[].name" \
  --output table

log "Verificando suscripciones de konrad-order-events..."
az servicebus topic subscription list \
  --topic-name konrad-order-events \
  --namespace-name "$NAMESPACE" \
  --resource-group "$RESOURCE_GROUP" \
  --query "[].name" \
  --output table

log "Verificando variable en notification-service..."
az containerapp show \
  --name notification-service \
  --resource-group "$RESOURCE_GROUP" \
  --query "properties.template.containers[0].env[?name=='AZURE_SERVICEBUS_CONNECTION_STRING'].value" \
  --output tsv

echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║              SERVICE BUS CONFIGURADO                        ║"
echo "╠══════════════════════════════════════════════════════════════╣"
echo "║  Flujo de mensajes:                                          ║"
echo "║  seller  ──► konrad-seller-events  ──► notification-service  ║"
echo "║  buyer   ──► konrad-buyer-events   ──► notification-service  ║"
echo "║  payment ──► konrad-payment-events ──► seller + notification ║"
echo "║  order   ──► konrad-order-events   ──► seller + bam          ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
