package com.konrad.orderservice.model.entity;

import com.konrad.orderservice.model.entity.Cart;
import com.konrad.orderservice.model.entity.*;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class OrderRepository {

    private final Map<String, Cart> cartStore = new ConcurrentHashMap<>();
    private final Map<String, Order> orderStore = new ConcurrentHashMap<>();

    public OrderRepository() { loadMockData(); }

    private void loadMockData() {
        // Carrito activo de un comprador
        cartStore.put("cart-001", Cart.builder()
            .id("cart-001").buyerId("buyer-001")
            .items(List.of(
                Cart.CartItem.builder().productId("prod-001")
                    .nombre("Camiseta Polo Classic").cantidad(2)
                    .precioUnitario(180000.0).categoria("Ropa")
                    .peso(0.3).aplicaIVA(false).build(),
                Cart.CartItem.builder().productId("prod-003")
                    .nombre("Audífonos Bluetooth Pro").cantidad(1)
                    .precioUnitario(250000.0).categoria("Electrónica")
                    .peso(0.25).aplicaIVA(true).build()))
            .entregaDomicilio(true).ciudadEntrega("Bogotá")
            .creadoEn(LocalDateTime.now().minusHours(2)).build());

        // Orden ya pagada
        orderStore.put("ord-001", Order.builder()
            .id("ord-001").buyerId("buyer-001").sellerId("seller-001")
            .subtotal(610000.0).comision(30500.0).costoEnvio(15000.0)
            .iva(47500.0).total(703000.0)
            .status(Order.OrderStatus.ENTREGADO)
            .paymentId("pay-002").entregaDomicilio(true).ciudadEntrega("Bogotá")
            .creadoEn(LocalDateTime.now().minusDays(5))
            .calificacion(9).comentario("Excelente vendedor, envío rápido")
            .build());
    }

    // Carritos
    public Cart saveCart(Cart c) {
        if (c.getId() == null) c.setId("cart-" + UUID.randomUUID().toString().substring(0, 8));
        cartStore.put(c.getId(), c);
        return c;
    }
    public Optional<Cart> findCartByBuyer(String buyerId) {
        return cartStore.values().stream().filter(c -> c.getBuyerId().equals(buyerId)).findFirst();
    }
    public void deleteCart(String id) { cartStore.remove(id); }

    // Órdenes
    public Order saveOrder(Order o) {
        if (o.getId() == null) o.setId("ord-" + UUID.randomUUID().toString().substring(0, 8));
        orderStore.put(o.getId(), o);
        return o;
    }
    public Optional<Order> findOrderById(String id) { return Optional.ofNullable(orderStore.get(id)); }
    public List<Order> findOrdersByBuyer(String buyerId) {
        return orderStore.values().stream().filter(o -> o.getBuyerId().equals(buyerId))
            .collect(Collectors.toList());
    }
    public List<Order> findOrdersBySeller(String sellerId) {
        return orderStore.values().stream().filter(o -> o.getSellerId().equals(sellerId))
            .collect(Collectors.toList());
    }
}
