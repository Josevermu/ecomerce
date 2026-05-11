package com.konrad.sellerservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling   // Habilita @Scheduled para el job de vencimientos (punto 5)
public class SellerApplication {
    public static void main(String[] args) {
        SpringApplication.run(SellerApplication.class, args);
    }
}