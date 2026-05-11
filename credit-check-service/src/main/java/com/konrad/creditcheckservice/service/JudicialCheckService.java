package com.konrad.creditcheckservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.konrad.creditcheckservice.service.VendorVerificationFacade.JudicialStatus;

/**
 * Consulta antecedentes judiciales en la página de la Policía Nacional.
 * "el Director debe ingresar a la página de la policía y con el número de
 * identificación consultar los antecedentes judiciales"
 *
 * MOCK: simula el scraping de la página web de la Policía.
 * En producción: HttpClient / Selenium / Puppeteer contra
 * https://antecedentes.policia.gov.co
 */
@Slf4j
@Service
public class JudicialCheckService {

    public JudicialStatus check(String identificacion) {
        log.info("[JUDICIAL] Consultando antecedentes en página de la Policía: {}", identificacion);

        // MOCK: IDs que terminan en número impar → NO_REQUERIDO
        //       IDs que terminan en número par y > 5 → REQUERIDO (para pruebas)
        int lastDigit = Character.getNumericValue(identificacion.charAt(identificacion.length() - 1));
        JudicialStatus result = (lastDigit % 2 == 0 && lastDigit > 5)
            ? JudicialStatus.REQUERIDO
            : JudicialStatus.NO_REQUERIDO;

        log.info("[JUDICIAL] Resultado para {}: {}", identificacion, result);
        return result;
    }
}
