package com.konrad.orderservice.service;

import com.konrad.orderservice.model.entity.Cart;
import com.konrad.orderservice.model.entity.Order;
import com.konrad.orderservice.model.entity.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Servicio de órdenes y carrito.
 * Implementa las reglas de cálculo del total (punto 11):
 *   1. Comisión por categoría
 *   2. Costo de envío por ciudad y peso
 *   3. IVA configurable por producto
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository repo;

    // Comisiones por categoría (configurables — punto 11.1)
    private static final Map<String, Double> COMMISSION_RATES = Map.of(
        "Ropa", 0.05,
        "Calzado", 0.06,
        "Electrónica", 0.04,
        "Accesorios", 0.07
    );

    // Costos de envío por ciudad en COP (configurables — punto 11.2)
    private static final Map<String, Double> SHIPPING_BASE = Map.of(
        "Bogotá", 8000.0,
        "Medellín", 10000.0,
        "Cali", 12000.0,
        "Barranquilla", 15000.0
    );
    private static final double SHIPPING_PER_KG = 2000.0;

    private static final double IVA_RATE = 0.19; // 19% configurable (punto 11.3)

    // ─── Carrito ──────────────────────────────────────────────────────────────

    public Cart getOrCreateCart(String buyerId) {
        return repo.findCartByBuyer(buyerId).orElseGet(() -> {
            Cart cart = Cart.builder().buyerId(buyerId)
                .items(new java.util.ArrayList<>()).creadoEn(LocalDateTime.now()).build();
            return repo.saveCart(cart);
        });
    }

    public Cart addToCart(String buyerId, Cart.CartItem item) {
        Cart cart = getOrCreateCart(buyerId);
        cart.getItems().add(item);
        return repo.saveCart(cart);
    }

    public Cart updateCartDelivery(String buyerId, boolean domicilio, String ciudad) {
        Cart cart = getOrCreateCart(buyerId);
        cart.setEntregaDomicilio(domicilio);
        cart.setCiudadEntrega(ciudad);
        return repo.saveCart(cart);
    }

    // ─── Checkout ─────────────────────────────────────────────────────────────

    public Order checkout(String buyerId, String paymentId) {
        Cart cart = repo.findCartByBuyer(buyerId)
            .orElseThrow(() -> new RuntimeException("Carrito vacío para: " + buyerId));

        // Calcular totales con las 3 reglas del documento
        double subtotal = cart.getItems().stream()
            .mapToDouble(i -> i.getPrecioUnitario() * i.getCantidad()).sum();

        double comision = cart.getItems().stream()
            .mapToDouble(i -> i.getPrecioUnitario() * i.getCantidad()
                * COMMISSION_RATES.getOrDefault(i.getCategoria(), 0.05))
            .sum();

        double pesoTotal = cart.getItems().stream()
            .mapToDouble(i -> i.getPeso() * i.getCantidad()).sum();
        double costoEnvio = cart.isEntregaDomicilio()
            ? SHIPPING_BASE.getOrDefault(cart.getCiudadEntrega(), 15000.0)
              + (pesoTotal * SHIPPING_PER_KG)
            : 0.0;

        double iva = cart.getItems().stream()
            .filter(Cart.CartItem::isAplicaIVA)
            .mapToDouble(i -> i.getPrecioUnitario() * i.getCantidad() * IVA_RATE)
            .sum();

        double total = subtotal + comision + costoEnvio + iva;

        Order order = Order.builder()
            .buyerId(buyerId)
            .sellerId(cart.getItems().isEmpty() ? "" : "seller-001") // mock
            .items(cart.getItems())
            .subtotal(subtotal).comision(comision).costoEnvio(costoEnvio).iva(iva).total(total)
            .status(Order.OrderStatus.PAGADO)
            .paymentId(paymentId)
            .entregaDomicilio(cart.isEntregaDomicilio())
            .ciudadEntrega(cart.getCiudadEntrega())
            .creadoEn(LocalDateTime.now())
            .build();

        Order saved = repo.saveOrder(order);
        repo.deleteCart(cart.getId()); // Limpiar carrito tras la compra
        log.info("[ORDER] Orden creada: {} — total: ${}", saved.getId(), total);
        return saved;
    }

    // ─── Calificación (punto 12) ──────────────────────────────────────────────

    public Order rateTransaction(String orderId, int calificacion, String comentario) {
        if (calificacion < 1 || calificacion > 10) {
            throw new RuntimeException("Calificación debe estar entre 1 y 10");
        }
        Order order = repo.findOrderById(orderId)
            .orElseThrow(() -> new RuntimeException("Orden no encontrada: " + orderId));
        order.setCalificacion(calificacion);
        order.setComentario(comentario);
        return repo.saveOrder(order);
    }

    public List<Order> findByBuyer(String buyerId) { return repo.findOrdersByBuyer(buyerId); }
    public List<Order> findBySeller(String sellerId) { return repo.findOrdersBySeller(sellerId); }
    public Order findById(String id) {
        return repo.findOrderById(id).orElseThrow(() -> new RuntimeException("Orden no encontrada: " + id));
    }
}
