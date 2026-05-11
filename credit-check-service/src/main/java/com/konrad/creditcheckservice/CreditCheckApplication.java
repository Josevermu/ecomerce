package com.konrad.creditcheckservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @EnableScheduling habilita el CIFINSyncJob mensual que descarga
 * el archivo plano del FTP de CIFIN y actualiza la BD local (punto 2).
 */
@SpringBootApplication
@EnableScheduling
public class CreditCheckApplication {
    public static void main(String[] args) {
        SpringApplication.run(CreditCheckApplication.class, args);
    }
}