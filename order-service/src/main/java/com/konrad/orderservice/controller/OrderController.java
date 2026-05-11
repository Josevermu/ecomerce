package com.konrad.orderservice.controller;

import com.konrad.orderservice.model.entity.Cart;
import com.konrad.orderservice.model.entity.*;
import com.konrad.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // ─── Carrito ──────────────────────────────────────────────────────────────

    /** GET /cart/{buyerId} — ver carrito actual */
    @GetMapping("/cart/{buyerId}")
    public ResponseEntity<Cart> getCart(@PathVariable String buyerId) {
        return ResponseEntity.ok(orderService.getOrCreateCart(buyerId));
    }

    /** POST /cart/{buyerId}/items — agregar producto al carrito (punto 11) */
    @PostMapping("/cart/{buyerId}/items")
    public ResponseEntity<Cart> addItem(@PathVariable String buyerId,
                                         @RequestBody Cart.CartItem item) {
        return ResponseEntity.ok(orderService.addToCart(buyerId, item));
    }

    /** PUT /cart/{buyerId}/delivery — elegir retiro o domicilio */
    @PutMapping("/cart/{buyerId}/delivery")
    public ResponseEntity<Cart> setDelivery(@PathVariable String buyerId,
                                              @RequestParam boolean domicilio,
                                              @RequestParam(required = false) String ciudad) {
        return ResponseEntity.ok(orderService.updateCartDelivery(buyerId, domicilio, ciudad));
    }

    // ─── Checkout ─────────────────────────────────────────────────────────────

    /** POST /orders/checkout/{buyerId} — genera la orden con cálculo de totales (punto 11) */
    @PostMapping("/orders/checkout/{buyerId}")
    public ResponseEntity<Order> checkout(@PathVariable String buyerId,
                                           @RequestParam String paymentId) {
        return ResponseEntity.ok(orderService.checkout(buyerId, paymentId));
    }

    // ─── Calificación ─────────────────────────────────────────────────────────

    /** POST /orders/{orderId}/rating — calificar transacción 1-10 (punto 12) */
    @PostMapping("/orders/{orderId}/rating")
    public ResponseEntity<Order> rate(@PathVariable String orderId,
                                       @RequestBody Map<String, Object> body) {
        int calificacion = (int) body.get("calificacion");
        String comentario = (String) body.getOrDefault("comentario", "");
        return ResponseEntity.ok(orderService.rateTransaction(orderId, calificacion, comentario));
    }

    // ─── Consultas ────────────────────────────────────────────────────────────

    /** GET /orders/{id} — detalle de orden */
    @GetMapping("/orders/{id}")
    public ResponseEntity<Order> detail(@PathVariable String id) {
        return ResponseEntity.ok(orderService.findById(id));
    }

    /** GET /orders/buyer/{buyerId} — órdenes de un comprador */
    @GetMapping("/orders/buyer/{buyerId}")
    public ResponseEntity<List<Order>> byBuyer(@PathVariable String buyerId) {
        return ResponseEntity.ok(orderService.findByBuyer(buyerId));
    }

    /** GET /orders/seller/{sellerId} — ventas de un vendedor */
    @GetMapping("/orders/seller/{sellerId}")
    public ResponseEntity<List<Order>> bySeller(@PathVariable String sellerId) {
        return ResponseEntity.ok(orderService.findBySeller(sellerId));
    }
}
