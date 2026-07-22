package com.adaptive.loadmanagement.controlplane.service;

import com.adaptive.loadmanagement.starter.model.MetricTelemetryPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class TelemetryAggregatorService {

    private static final Logger log = LoggerFactory.getLogger(TelemetryAggregatorService.class);

    private final Map<String, ConcurrentLinkedQueue<Long>> endpointLatencyMap = new ConcurrentHashMap<>();
    private final Map<String, Long> endpointMinRttMap = new ConcurrentHashMap<>();

    @KafkaListener(topics = "microservice-telemetry-metrics", groupId = "control-plane-group")
    public void consumeTelemetry(MetricTelemetryPayload payload) {
        String endpoint = payload.endpoint();
        long rtt = payload.executionTimeMs();

        endpointLatencyMap.computeIfAbsent(endpoint, k -> new ConcurrentLinkedQueue<>());
        ConcurrentLinkedQueue<Long> queue = endpointLatencyMap.get(endpoint);
        
        queue.add(rtt);
        if (queue.size() > 100) {
            queue.poll();
        }

        if (rtt > 0) {
            endpointMinRttMap.compute(endpoint, (k, currentMin) -> 
                    (currentMin == null) ? rtt : Math.min(currentMin, rtt));
        }
    }

    public Map<String, ConcurrentLinkedQueue<Long>> getEndpointLatencyMap() {
        return endpointLatencyMap;
    }

    public Map<String, Long> getEndpointMinRttMap() {
        return endpointMinRttMap;
    }
}

