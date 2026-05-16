#!/bin/bash

set -e

RESOURCE_GROUP="konrad-rg"
NAMESPACE="konrad-servicebus"

echo "======================================="
echo " FIXING AZURE SERVICE BUS SUBSCRIPTIONS"
echo "======================================="

echo ""
echo "[1/5] Checking namespace..."

az servicebus namespace show \
  --name "$NAMESPACE" \
  --resource-group "$RESOURCE_GROUP" \
  --output none

echo "✅ Namespace exists"

echo ""
echo "[2/5] Creating subscriptions..."

create_sub() {
  SUB=$1
  TOPIC=$2

  echo "Creating $SUB on $TOPIC..."

  az servicebus topic subscription create \
    --name "$SUB" \
    --topic-name "$TOPIC" \
    --namespace-name "$NAMESPACE" \
    --resource-group "$RESOURCE_GROUP" \
    --max-delivery-count 3 \
    --lock-duration PT30S \
    --enable-dead-lettering-on-message-expiration \
    --output none || true

  echo "✅ $SUB → $TOPIC"
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

echo ""
echo "[3/5] Getting Service Bus connection string..."

CONNECTION_STRING=$(az servicebus namespace authorization-rule keys list \
  --name RootManageSharedAccessKey \
  --namespace-name "$NAMESPACE" \
  --resource-group "$RESOURCE_GROUP" \
  --query primaryConnectionString \
  -o tsv)

echo "✅ Connection string obtained"

echo ""
echo "[4/5] Updating Container Apps..."

SERVICES=(
  "seller-service"
  "buyer-service"
  "payment-service"
  "order-service"
  "notification-service"
  "bam-service"
)

for SERVICE in "${SERVICES[@]}"; do
  echo "Updating $SERVICE..."

  az containerapp update \
    --name "$SERVICE" \
    --resource-group "$RESOURCE_GROUP" \
    --set-env-vars \
      "AZURE_SERVICEBUS_CONNECTION_STRING=$CONNECTION_STRING" \
    --output none

  echo "✅ $SERVICE updated"
done

echo ""
echo "[5/5] Verifying subscriptions..."

for topic in \
  konrad-seller-events \
  konrad-buyer-events \
  konrad-payment-events \
  konrad-order-events
do
  echo ""
  echo "===== $topic ====="

  az servicebus topic subscription list \
    --topic-name "$topic" \
    --namespace-name "$NAMESPACE" \
    --resource-group "$RESOURCE_GROUP" \
    --query "[].name" \
    -o table
done

echo ""
echo "======================================="
echo " SERVICE BUS FIX COMPLETE"
echo "======================================="
echo ""
echo "Now test your app again (create order/payment)"
echo "Then check Azure Metrics after 30–60 seconds."