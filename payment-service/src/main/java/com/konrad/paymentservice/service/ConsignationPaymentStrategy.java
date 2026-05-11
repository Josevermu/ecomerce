package com.konrad.paymentservice.service;

import com.konrad.paymentservice.dto.PaymentDtos;
import com.konrad.paymentservice.dto.PaymentDtos.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * STRATEGY CONCRETA: Consignación bancaria.
 * "el banco deja un archivo al final del día en un FileSystem de esta entidad
 * con permisos de acceso con el listado de las personas que han consignado"
 * <p>
 * Fase 1: Genera recibo para que el usuario lo imprima.
 * Fase 2: ConsignationSyncJob (job nocturno) lee el archivo plano y confirma.
 */
@Slf4j
@Component
public class ConsignationPaymentStrategy implements PaymentStrategy {

    @Override
    public PaymentResult pay(PaymentRequest request) {
        log.info("[STRATEGY-CONSIGN] Generando recibo de consignación para: {}",
                request.getPagadorIdentificacion());

        // MOCK: generar número de recibo para impresión
        String receiptNumber = "REC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        log.info("[STRATEGY-CONSIGN] Recibo generado: {}. Pendiente confirmación bancaria.", receiptNumber);

        // Retorna éxito con estado "PENDIENTE_BANCO" — no es pago confirmado todavía
        // El estado real se actualiza cuando ConsignationSyncJob lee el archivo plano
        return PaymentDtos.PaymentResult.builder()
                .success(true)
                .numeroAprobacion(receiptNumber)
                .metodo("CONSIGNATION")
                .estado("PENDIENTE_BANCO")
                .mensaje("Recibo generado. El pago se confirma cuando el banco procese la consignación.")
                .build();
    }

    @Override
    public String getMethodName() {
        return "CONSIGNATION";
    }
}
