package com.konrad.productservice.controller;

import com.konrad.productservice.model.entity.Product;
import com.konrad.productservice.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /** GET /products/search?nombre=&categoria=&subcategoria=&precioMin=&precioMax=&palabra=
     * Punto 8: búsqueda pública de productos */
    @GetMapping("/products/search")
    public ResponseEntity<List<Product>> search(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String subcategoria,
            @RequestParam(required = false) Double precioMin,
            @RequestParam(required = false) Double precioMax,
            @RequestParam(required = false) String palabra) {
        return ResponseEntity.ok(productService.search(nombre, categoria, subcategoria,
            precioMin, precioMax, palabra));
    }

    /** GET /products/{id} — detalle de producto (punto 9) */
    @GetMapping("/products/{id}")
    public ResponseEntity<Product> detail(@PathVariable String id) {
        return ResponseEntity.ok(productService.findById(id));
    }

    /** POST /products — publicar producto (SELLER, punto 4) */
    @PostMapping("/products")
    public ResponseEntity<Product> create(@RequestBody Product product) {
        return ResponseEntity.ok(productService.create(product));
    }

    /** PUT /products/{id} — actualizar producto (SELLER) */
    @PutMapping("/products/{id}")
    public ResponseEntity<Product> update(@PathVariable String id, @RequestBody Product product) {
        return ResponseEntity.ok(productService.update(id, product));
    }

    /** DELETE /products/{id} — desactivar producto (SELLER) */
    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** GET /products/seller/{sellerId} — productos de un vendedor */
    @GetMapping("/products/seller/{sellerId}")
    public ResponseEntity<List<Product>> bySeller(@PathVariable String sellerId) {
        return ResponseEntity.ok(productService.findBySeller(sellerId));
    }
}
