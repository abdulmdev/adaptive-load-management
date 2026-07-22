package com.adaptive.loadmanagement.driver.domain.model;

public record DriverLocation(
        String driverId,
        double latitude,
        double longitude,
        String status,
        long timestamp
) {}
