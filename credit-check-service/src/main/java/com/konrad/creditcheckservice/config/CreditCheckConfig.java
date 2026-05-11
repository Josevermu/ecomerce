package com.konrad.creditcheckservice.config;

import com.konrad.creditcheckservice.model.entity.CIFINRepository;
import com.konrad.creditcheckservice.service.CreditCheckStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de beans — decide qué implementación de CreditCheckStrategy
 * se inyecta en cada punto.
 *
 * El Facade recibe las dos estrategias por nombre via @Qualifier en producción.
 * Aquí se exponen como beans nombrados para mayor claridad.
 */
@Configuration
public class CreditCheckConfig {

    @Bean("datacreditoStrategy")
    public CreditCheckStrategy datacreditoStrategy() {
        return new DatacreditoStrategy();
    }

    @Bean("cifinStrategy")
    public CreditCheckStrategy cifinStrategy(CIFINRepository repo) {
        return new CIFINStrategy(repo);
    }
}
