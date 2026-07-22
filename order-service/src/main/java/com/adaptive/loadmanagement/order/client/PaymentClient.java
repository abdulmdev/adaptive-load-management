package com.adaptive.loadmanagement.order.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class PaymentClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentClient.class);
    
    // Switch to simulate external API degradation
    private final AtomicBoolean isDegraded = new AtomicBoolean(false);

    /**
     * Simulates an external network call to a payment gateway (e.g., Stripe, Braintree).
     * Protected by Resilience4j Circuit Breaker.
     */
    @CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
    public Map<String, Object> processPayment(String orderId, double amount) {
        log.info("Attempting external payment call for order: {}", orderId);
        
        if (isDegraded.get()) {
            try {
                log.warn("Payment API is degraded! Blocking thread for 5 seconds...");
                // Simulating a very slow network call that hogs Tomcat threads
                Thread.sleep(5000); 
                throw new RuntimeException("External Payment API Timeout / 503");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        
        // Fast, successful response
        return Map.of("status", "SUCCESS", "transactionId", "txn_" + System.currentTimeMillis());
    }

    /**
     * Fallback method executed IMMEDIATELY when the circuit is OPEN,
     * or when the primary method throws an exception.
     */
    public Map<String, Object> paymentFallback(String orderId, double amount, Throwable t) {
        log.error("Payment Fallback invoked for order {}. Reason: {}", orderId, t.getMessage());
        return Map.of("status", "FALLBACK_PENDING", "message", "Payment processing delayed due to gateway issues. We will retry asynchronously.");
    }

    public void setDegraded(boolean degraded) {
        this.isDegraded.set(degraded);
        log.info("External Payment API degraded status set to: {}", degraded);
    }
    
    public boolean isDegraded() {
        return this.isDegraded.get();
    }
}
