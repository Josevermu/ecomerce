package com.konrad.notificationservice.service;

import com.konrad.notificationservice.model.entity.Notification;
import com.konrad.notificationservice.model.entity.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Servicio mock de correo electrónico.
 * En producción: JavaMailSender con SMTP, o AWS SES, o SendGrid.
 * RNF Seguridad: "cualquier correo debe ser certificado y con estampado cronológico".
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final NotificationRepository notificationRepo;

    public void sendCertifiedEmail(String to, String subject, String body) {
        // MOCK: imprime el correo en logs y lo persiste
        log.info("╔══════════════════════════════════════════════════════╗");
        log.info("║  [EMAIL MOCK — Certificado con estampado cronológico] ║");
        log.info("║  Para:    {}",  to);
        log.info("║  Asunto:  {}",  subject);
        log.info("║  Cuerpo:  {}",  body.replace("\n", " | "));
        log.info("║  Timestamp: {}", LocalDateTime.now());
        log.info("╚══════════════════════════════════════════════════════╝");

        // Persistir registro de la notificación enviada
        notificationRepo.save(Notification.builder()
            .destinatario(to)
            .asunto(subject)
            .cuerpo(body)
            .enviado(true)
            .timestamp(LocalDateTime.now())
            .build());
    }
}
