package com.konrad.productservice.model.entity;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

/** Entidad Producto — punto 4 del documento */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Product {
    private String id;
    private String sellerId;
    private String nombre;
    private String categoria;
    private String subcategoria;
    private String marca;
    private boolean original;       // true=Original / false=Genérico
    private String color;
    private String tamano;
    private Double peso;
    private String talla;
    private boolean nuevo;          // true=Nuevo / false=Usado
    private Integer cantidad;
    private Double valor;
    private List<String> imagenes;  // rutas mock de imágenes
    private boolean activo;
    private LocalDateTime creadoEn;
}
