package com.adaptive.loadmanagement.order.domain.model;

import java.math.BigDecimal;

/**
 * Domain Order entity model.
 */
public record Order(
        String orderId,
        String customerId,
        BigDecimal amount,
        String status,
        String priorityTier,
        long createdAt
) {}
