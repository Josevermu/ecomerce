package com.konrad.notificationservice.model.entity;

import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Notification {
    private String id;
    private String destinatario;
    private String asunto;
    private String cuerpo;
    private boolean enviado;
    private LocalDateTime timestamp;
    private String eventoOrigen;  // tipo de evento que disparó la notificación
}
