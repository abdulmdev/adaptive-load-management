package com.adaptive.loadmanagement.order.service;

import com.adaptive.loadmanagement.order.domain.model.Order;
import com.adaptive.loadmanagement.order.dto.request.CreateOrderRequest;
import com.adaptive.loadmanagement.order.dto.response.OrderResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static volatile long artificialLatencyMs = 20L;

    public OrderResponse createOrder(CreateOrderRequest request, String priorityHeader) throws InterruptedException {
        simulateProcessingTime();

        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("Order processed successfully. ID: {}, Priority: {}", orderId, priorityHeader);

        return new OrderResponse(
                orderId,
                request.customerId() != null ? request.customerId() : "CUST-1001",
                request.amount() != null ? request.amount() : new BigDecimal("49.99"),
                "CREATED",
                priorityHeader,
                artificialLatencyMs,
                System.currentTimeMillis()
        );
    }

    public List<Order> getOrders(String priorityHeader) throws InterruptedException {
        simulateProcessingTime();

        return List.of(
                new Order("ORD-101", "CUST-1001", new BigDecimal("25.50"), "COMPLETED", priorityHeader, System.currentTimeMillis()),
                new Order("ORD-102", "CUST-1002", new BigDecimal("120.00"), "PROCESSING", priorityHeader, System.currentTimeMillis())
        );
    }

    public void setArtificialLatency(long delayMs) {
        artificialLatencyMs = delayMs;
        log.warn("ARTIFICIAL LATENCY UPDATED to {}ms", delayMs);
    }

    public long getArtificialLatencyMs() {
        return artificialLatencyMs;
    }

    private void simulateProcessingTime() throws InterruptedException {
        long jitter = ThreadLocalRandom.current().nextLong(0, 10);
        Thread.sleep(artificialLatencyMs + jitter);
    }
}
