package com.konrad.productservice.service;

import com.konrad.productservice.model.entity.Product;
import com.konrad.productservice.model.entity.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository repo;

    /** Punto 4: Publicar producto */
    public Product create(Product product) {
        product.setCreadoEn(LocalDateTime.now());
        product.setActivo(true);
        Product saved = repo.save(product);
        log.info("[PRODUCT] Producto publicado: {} por vendedor: {}", saved.getId(), saved.getSellerId());
        return saved;
    }

    /** Punto 8: Búsqueda de productos */
    public List<Product> search(String nombre, String categoria, String subcategoria,
                                 Double precioMin, Double precioMax, String palabra) {
        return repo.search(nombre, categoria, subcategoria, precioMin, precioMax, palabra);
    }

    /** Punto 9: Detalle de producto */
    public Product findById(String id) {
        return repo.findById(id)
            .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + id));
    }

    public List<Product> findBySeller(String sellerId) {
        return repo.findBySellerId(sellerId);
    }

    public Product update(String id, Product updated) {
        Product existing = findById(id);
        updated.setId(existing.getId());
        updated.setSellerId(existing.getSellerId());
        updated.setCreadoEn(existing.getCreadoEn());
        return repo.save(updated);
    }

    public void delete(String id) {
        repo.delete(id);
        log.info("[PRODUCT] Producto desactivado: {}", id);
    }
}
