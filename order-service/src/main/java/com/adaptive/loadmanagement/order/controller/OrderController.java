package com.adaptive.loadmanagement.order.controller;

import com.adaptive.loadmanagement.order.domain.model.Order;
import com.adaptive.loadmanagement.order.dto.request.CreateOrderRequest;
import com.adaptive.loadmanagement.order.dto.response.OrderResponse;
import com.adaptive.loadmanagement.order.service.OrderService;
import com.adaptive.loadmanagement.order.client.PaymentClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.adaptive.loadmanagement.starter.annotation.AdaptiveRateLimit;
import com.adaptive.loadmanagement.starter.model.PriorityTier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderService orderService;
    private final PaymentClient paymentClient;

    public OrderController(OrderService orderService, PaymentClient paymentClient) {
        this.orderService = orderService;
        this.paymentClient = paymentClient;
    }

    @PostMapping
    @AdaptiveRateLimit(key = "order-service:create_order", defaultPriority = PriorityTier.P0, baseCapacity = 50)
    public ResponseEntity<OrderResponse> createOrder(
            @RequestBody(required = false) CreateOrderRequest request,
            @RequestHeader(value = "X-Priority-Tier", defaultValue = "P0") String priorityHeader) throws InterruptedException {
        
        CreateOrderRequest body = (request != null) ? request : new CreateOrderRequest("CUST-1001", null, "Standard Order");
        OrderResponse response = orderService.createOrder(body, priorityHeader);
        
        // Invoke simulated external Payment API
        Map<String, Object> paymentResult = paymentClient.processPayment(response.orderId(), 49.99);
        log.info("Payment result for order {}: {}", response.orderId(), paymentResult);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    @AdaptiveRateLimit(key = "order-service:get_orders", defaultPriority = PriorityTier.P2, baseCapacity = 50)
    public ResponseEntity<List<Order>> getOrders(
            @RequestHeader(value = "X-Priority-Tier", defaultValue = "P2") String priorityHeader) throws InterruptedException {
        
        List<Order> orders = orderService.getOrders(priorityHeader);
        return ResponseEntity.ok(orders);
    }

    @PostMapping("/admin/simulate-degradation")
    public ResponseEntity<Map<String, Object>> setArtificialLatency(@RequestParam("delayMs") long delayMs) {
        orderService.setArtificialLatency(delayMs);
        return ResponseEntity.ok(Map.of("simulatedLatencyMs", orderService.getArtificialLatencyMs()));
    }

    @PostMapping("/admin/simulate-payment-outage")
    public ResponseEntity<Map<String, Object>> setPaymentOutage(@RequestParam("degraded") boolean degraded) {
        paymentClient.setDegraded(degraded);
        return ResponseEntity.ok(Map.of("paymentDegraded", paymentClient.isDegraded()));
    }
}
