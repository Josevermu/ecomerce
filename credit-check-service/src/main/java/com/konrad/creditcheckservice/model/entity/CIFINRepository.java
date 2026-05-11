package com.konrad.creditcheckservice.model.entity;

import com.konrad.creditcheckservice.model.entity.CreditRecord;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BD local con datos cargados del archivo plano mensual de CIFIN.
 * En producción: JpaRepository + CIFINSyncJob que carga el archivo FTP mensualmente.
 */
@Repository
public class CIFINRepository {

    private final Map<String, CreditRecord> store = new ConcurrentHashMap<>();

    public CIFINRepository() { loadMockData(); }

    /** Simula los datos que estarían en la BD local después de procesar el archivo plano */
    private void loadMockData() {
        // Personas con distintos estados para probar todos los escenarios
        put("12345678", "ALTA");
        put("87654321", "ADVERTENCIA");
        put("11223344", "BAJA");
        put("99887766", "ALTA");
        put("55443322", "ADVERTENCIA");
        put("900123456", "ALTA");
    }

    private void put(String id, String estado) {
        store.put(id, CreditRecord.builder()
            .identificacion(id).estado(estado).fuente("CIFIN").build());
    }

    public Optional<CreditRecord> findByIdentificacion(String identificacion) {
        return Optional.ofNullable(store.get(identificacion));
    }

    public void replaceAll(List<CreditRecord> records) {
        store.clear();
        records.forEach(r -> store.put(r.getIdentificacion(), r));
    }

    public List<CreditRecord> findAll() { return new ArrayList<>(store.values()); }
}
