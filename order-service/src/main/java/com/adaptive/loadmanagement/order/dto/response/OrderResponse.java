package com.adaptive.loadmanagement.order.dto.response;

import java.math.BigDecimal;

public record OrderResponse(
        String orderId,
        String customerId,
        BigDecimal amount,
        String status,
        String priorityTier,
        long latencyMs,
        long timestamp
) {}
