package com.konrad.productservice.model.entity;

import com.konrad.productservice.model.entity.Product;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class ProductRepository {

    private final Map<String, Product> store = new ConcurrentHashMap<>();

    public ProductRepository() { loadMockData(); }

    private void loadMockData() {
        save(Product.builder().sellerId("seller-001").nombre("Camiseta Polo Classic")
            .categoria("Ropa").subcategoria("Camisetas").marca("Lacoste")
            .original(true).color("Blanco").tamano("M").peso(0.3).talla("M")
            .nuevo(true).cantidad(50).valor(180000.0)
            .imagenes(List.of("/imgs/polo1.jpg", "/imgs/polo2.jpg"))
            .activo(true).creadoEn(LocalDateTime.now().minusDays(20)).build());

        save(Product.builder().sellerId("seller-001").nombre("Zapatos Deportivos Air")
            .categoria("Calzado").subcategoria("Deportivo").marca("Nike")
            .original(true).color("Negro").tamano("42").peso(0.8).talla("42")
            .nuevo(true).cantidad(30).valor(320000.0)
            .imagenes(List.of("/imgs/nike1.jpg"))
            .activo(true).creadoEn(LocalDateTime.now().minusDays(15)).build());

        save(Product.builder().sellerId("seller-001").nombre("Audífonos Bluetooth Pro")
            .categoria("Electrónica").subcategoria("Audio").marca("Sony")
            .original(true).color("Plateado").tamano("Único").peso(0.25).talla("N/A")
            .nuevo(false).cantidad(10).valor(250000.0)
            .imagenes(List.of("/imgs/sony1.jpg", "/imgs/sony2.jpg"))
            .activo(true).creadoEn(LocalDateTime.now().minusDays(5)).build());

        save(Product.builder().sellerId("seller-002").nombre("Maletín Ejecutivo Cuero")
            .categoria("Accesorios").subcategoria("Bolsos").marca("Totto")
            .original(true).color("Café").tamano("Grande").peso(1.2).talla("N/A")
            .nuevo(true).cantidad(15).valor(150000.0)
            .imagenes(List.of("/imgs/maletin1.jpg"))
            .activo(true).creadoEn(LocalDateTime.now().minusDays(3)).build());
    }

    public Product save(Product p) {
        if (p.getId() == null) p.setId("prod-" + UUID.randomUUID().toString().substring(0, 8));
        store.put(p.getId(), p);
        return p;
    }

    public Optional<Product> findById(String id) { return Optional.ofNullable(store.get(id)); }

    public List<Product> findAll() { return new ArrayList<>(store.values()); }

    /** Búsqueda por múltiples criterios — punto 8 del documento */
    public List<Product> search(String nombre, String categoria, String subcategoria,
                                 Double precioMin, Double precioMax, String palabra) {
        return store.values().stream()
            .filter(p -> p.isActivo())
            .filter(p -> nombre == null || p.getNombre().toLowerCase().contains(nombre.toLowerCase()))
            .filter(p -> categoria == null || p.getCategoria().equalsIgnoreCase(categoria))
            .filter(p -> subcategoria == null || p.getSubcategoria().equalsIgnoreCase(subcategoria))
            .filter(p -> precioMin == null || p.getValor() >= precioMin)
            .filter(p -> precioMax == null || p.getValor() <= precioMax)
            .filter(p -> palabra == null ||
                p.getNombre().toLowerCase().contains(palabra.toLowerCase()) ||
                p.getMarca().toLowerCase().contains(palabra.toLowerCase()) ||
                p.getColor().toLowerCase().contains(palabra.toLowerCase()))
            .collect(Collectors.toList());
    }

    public List<Product> findBySellerId(String sellerId) {
        return store.values().stream()
            .filter(p -> p.getSellerId().equals(sellerId))
            .collect(Collectors.toList());
    }

    public void delete(String id) {
        store.computeIfPresent(id, (k, p) -> { p.setActivo(false); return p; });
    }
}
