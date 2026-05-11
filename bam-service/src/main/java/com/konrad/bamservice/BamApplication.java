package com.konrad.bamservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * BAM Service — Business Activity Monitoring.
 * Provee KPIs, tendencias y auditoría al Director Comercial (punto 13–15).
 *
 * @EnableScheduling habilita el job de detección automática de tendencias (punto 14).
 */
@SpringBootApplication
@EnableScheduling
public class BamApplication {
    public static void main(String[] args) {
        SpringApplication.run(BamApplication.class, args);
    }
}