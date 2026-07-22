package com.adaptive.loadmanagement.order.exception;

import com.adaptive.loadmanagement.starter.exception.RateLimitExceededException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleThrottled(RateLimitExceededException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                "error", "TOO_MANY_REQUESTS",
                "message", ex.getMessage(),
                "reason", ex.getReason(),
                "priority", ex.getPriorityTier().name(),
                "timestamp", System.currentTimeMillis()
        ));
    }
}
