package com.adaptive.loadmanagement.controlplane.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Closed-Loop Feedback Controller executing TCP-Vegas Gradient Concurrency algorithm:
 * 
 * Gradient = MinRTT / SmoothedRTT
 * NewLimit = max(MinLimit, currentLimit * Gradient + Beta)
 */
@Service
public class ClosedLoopController {

    private static final Logger log = LoggerFactory.getLogger(ClosedLoopController.class);

    private final TelemetryAggregatorService telemetryAggregatorService;
    private final StringRedisTemplate redisTemplate;
    private final MeterRegistry meterRegistry;

    public ClosedLoopController(TelemetryAggregatorService telemetryAggregatorService,
                                StringRedisTemplate redisTemplate,
                                MeterRegistry meterRegistry) {
        this.telemetryAggregatorService = telemetryAggregatorService;
        this.redisTemplate = redisTemplate;
        this.meterRegistry = meterRegistry;
    }


    private static final int MIN_CONCURRENCY_LIMIT = 5;
    private static final int DEFAULT_BASE_LIMIT = 50;
    private static final double BETA_ADDITIVE_INCREASE = 1.5;

    private final Map<String, AtomicInteger> limitGauges = new ConcurrentHashMap<>();
    private final Map<String, java.util.concurrent.atomic.AtomicLong> rttSmoothedGauges = new ConcurrentHashMap<>();
    private final Map<String, java.util.concurrent.atomic.AtomicLong> minRttGauges = new ConcurrentHashMap<>();

    @Scheduled(fixedRate = 1000)
    public void executeFeedbackControlLoop() {
        Map<String, ConcurrentLinkedQueue<Long>> latencyMap = telemetryAggregatorService.getEndpointLatencyMap();
        Map<String, Long> minRttMap = telemetryAggregatorService.getEndpointMinRttMap();

        if (latencyMap.isEmpty()) {
            return;
        }

        latencyMap.forEach((endpoint, rttQueue) -> {
            if (rttQueue.isEmpty()) return;

            Long minRtt = minRttMap.getOrDefault(endpoint, 10L);
            double smoothedRtt = rttQueue.stream().mapToLong(Long::longValue).average().orElse(minRtt);

            double gradient = (double) minRtt / Math.max(1.0, smoothedRtt);
            gradient = Math.max(0.1, Math.min(2.0, gradient));

            String currentLimitStr = redisTemplate.opsForValue().get("config:limit:" + endpoint);
            int currentLimit = (currentLimitStr != null) ? Integer.parseInt(currentLimitStr) : DEFAULT_BASE_LIMIT;

            int newLimit;
            if (gradient < 0.95) {
                newLimit = (int) Math.max(MIN_CONCURRENCY_LIMIT, Math.floor(currentLimit * gradient));
                log.warn("[CONTROL-PLANE] DEGRADATION DETECTED for [{}]: MinRTT={}ms, SmoothedRTT={}ms, Gradient={}. Throttling Limit: {} -> {}",
                        endpoint, minRtt, String.format("%.2f", smoothedRtt), String.format("%.2f", gradient), currentLimit, newLimit);
            } else {
                newLimit = (int) Math.min(200, Math.ceil(currentLimit + BETA_ADDITIVE_INCREASE));
            }

            double loadFactor = Math.min(1.0, Math.max(0.0, (smoothedRtt - minRtt) / (minRtt * 4.0)));

            redisTemplate.opsForValue().set("config:limit:" + endpoint, String.valueOf(newLimit));
            
            String serviceName = endpoint.split(":")[0];
            redisTemplate.opsForValue().set("config:load_factor:" + serviceName, String.format("%.2f", loadFactor));

            // Metric tracking via Gauge
            limitGauges.computeIfAbsent(endpoint, k -> 
                    meterRegistry.gauge("uber_ratelimit_concurrency_limit", Tags.of("endpoint", endpoint), new AtomicInteger(newLimit))
            ).set(newLimit);

            rttSmoothedGauges.computeIfAbsent(endpoint, k -> 
                    meterRegistry.gauge("uber_ratelimit_rtt_smoothed", Tags.of("endpoint", endpoint), new java.util.concurrent.atomic.AtomicLong((long) smoothedRtt))
            ).set((long) smoothedRtt);

            minRttGauges.computeIfAbsent(endpoint, k -> 
                    meterRegistry.gauge("uber_ratelimit_rtt_min", Tags.of("endpoint", endpoint), new java.util.concurrent.atomic.AtomicLong(minRtt))
            ).set(minRtt);
        });
    }
}
