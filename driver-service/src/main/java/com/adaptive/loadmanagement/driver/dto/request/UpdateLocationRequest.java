package com.adaptive.loadmanagement.driver.dto.request;

public record UpdateLocationRequest(
        String driverId,
        double latitude,
        double longitude
) {}
