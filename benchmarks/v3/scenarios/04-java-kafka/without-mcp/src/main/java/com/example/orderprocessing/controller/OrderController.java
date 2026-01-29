package com.example.orderprocessing.controller;

import com.example.orderprocessing.domain.entity.Order;
import com.example.orderprocessing.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * REST controller for order operations.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        List<OrderService.CreateOrderItemRequest> items = request.items().stream()
                .map(item -> new OrderService.CreateOrderItemRequest(
                        item.productId(),
                        item.productName(),
                        item.quantity(),
                        item.unitPrice()
                ))
                .toList();

        Order order = orderService.createOrder(request.customerId(), items);
        return ResponseEntity.status(HttpStatus.CREATED).body(OrderResponse.from(order));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId) {
        return orderService.getOrder(orderId)
                .map(order -> ResponseEntity.ok(OrderResponse.from(order)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByCustomer(@PathVariable String customerId) {
        List<OrderResponse> orders = orderService.getOrdersByCustomer(customerId).stream()
                .map(OrderResponse::from)
                .toList();
        return ResponseEntity.ok(orders);
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable String orderId,
            @RequestBody UpdateStatusRequest request) {
        try {
            Order order = orderService.updateOrderStatus(orderId, request.status());
            return ResponseEntity.ok(OrderResponse.from(order));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable String orderId) {
        try {
            Order order = orderService.cancelOrder(orderId);
            return ResponseEntity.ok(OrderResponse.from(order));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Request/Response DTOs

    public record CreateOrderRequest(
            @NotBlank String customerId,
            @NotEmpty List<CreateOrderItemRequest> items
    ) {}

    public record CreateOrderItemRequest(
            @NotBlank String productId,
            @NotBlank String productName,
            @Positive int quantity,
            @NotNull BigDecimal unitPrice
    ) {}

    public record UpdateStatusRequest(
            @NotNull Order.OrderStatus status
    ) {}

    public record OrderResponse(
            String id,
            String customerId,
            Order.OrderStatus status,
            BigDecimal totalAmount,
            List<OrderLineItemResponse> items
    ) {
        public static OrderResponse from(Order order) {
            List<OrderLineItemResponse> items = order.getLineItems().stream()
                    .map(item -> new OrderLineItemResponse(
                            item.getProductId(),
                            item.getProductName(),
                            item.getQuantity(),
                            item.getUnitPrice(),
                            item.getSubtotal()
                    ))
                    .toList();

            return new OrderResponse(
                    order.getId(),
                    order.getCustomerId(),
                    order.getStatus(),
                    order.getTotalAmount(),
                    items
            );
        }
    }

    public record OrderLineItemResponse(
            String productId,
            String productName,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal subtotal
    ) {}
}
