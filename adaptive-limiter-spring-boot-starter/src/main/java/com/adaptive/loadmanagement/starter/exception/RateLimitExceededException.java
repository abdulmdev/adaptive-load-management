package com.adaptive.loadmanagement.starter.exception;

import com.adaptive.loadmanagement.starter.model.PriorityTier;

public class RateLimitExceededException extends RuntimeException {
    private final String reason;
    private final PriorityTier priorityTier;

    public RateLimitExceededException(String message, String reason, PriorityTier priorityTier) {
        super(message);
        this.reason = reason;
        this.priorityTier = priorityTier;
    }

    public String getReason() {
        return reason;
    }

    public PriorityTier getPriorityTier() {
        return priorityTier;
    }
}
