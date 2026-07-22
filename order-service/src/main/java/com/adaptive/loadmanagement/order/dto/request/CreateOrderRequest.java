package com.adaptive.loadmanagement.order.dto.request;

import java.math.BigDecimal;

public record CreateOrderRequest(
        String customerId,
        BigDecimal amount,
        String itemDetails
) {}
