// ─── DTOs ─────────────────────────────────────────────────────────────────────
package com.konrad.creditcheckservice.dto;

import lombok.*;
import java.util.List;

public class CreditCheckDtos {

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class VerificationResult {
        private String identificacion;
        private String datacreditoScore;   // "ALTA" | "ADVERTENCIA" | "BAJA"
        private String cifinScore;
        private String judicialStatus;     // "REQUERIDO" | "NO_REQUERIDO"
        private String decision;           // "APROBADA" | "RECHAZADA" | "DEVUELTA"
        private List<String> motivos;
        private String recomendacion;
    }
}
