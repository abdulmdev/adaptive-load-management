package com.adaptive.loadmanagement.starter.model;

/**
 * Real-time metric snapshot sent by microservice nodes over Kafka to the Control Plane.
 */
public record MetricTelemetryPayload(
        String serviceName,
        String instanceId,
        String endpoint,
        long executionTimeMs,
        int currentInFlight,
        boolean isError,
        String priority,
        long timestamp
) {}

