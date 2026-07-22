package com.adaptive.loadmanagement.driver.controller;

import com.adaptive.loadmanagement.driver.domain.model.DriverLocation;
import com.adaptive.loadmanagement.driver.dto.request.UpdateLocationRequest;
import com.adaptive.loadmanagement.driver.service.DriverService;
import com.adaptive.loadmanagement.starter.annotation.AdaptiveRateLimit;
import com.adaptive.loadmanagement.starter.exception.RateLimitExceededException;
import com.adaptive.loadmanagement.starter.model.PriorityTier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/drivers")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @PostMapping("/location")
    @AdaptiveRateLimit(key = "driver-service:update_location", defaultPriority = PriorityTier.P0, baseCapacity = 100)
    public ResponseEntity<DriverLocation> updateLocation(
            @RequestBody(required = false) UpdateLocationRequest request,
            @RequestHeader(value = "X-Priority-Tier", defaultValue = "P0") String priorityHeader) {
        
        UpdateLocationRequest body = (request != null) ? request : new UpdateLocationRequest("DRV-9941", 37.7749, -122.4194);
        DriverLocation location = driverService.updateLocation(body, priorityHeader);
        return ResponseEntity.ok(location);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleThrottled(RateLimitExceededException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                "error", "TOO_MANY_REQUESTS",
                "message", ex.getMessage(),
                "reason", ex.getReason(),
                "priority", ex.getPriorityTier().name()
        ));
    }
}
