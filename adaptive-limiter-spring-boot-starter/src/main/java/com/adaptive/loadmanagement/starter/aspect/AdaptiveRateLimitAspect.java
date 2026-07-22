package com.adaptive.loadmanagement.starter.aspect;

import com.adaptive.loadmanagement.starter.annotation.AdaptiveRateLimit;
import com.adaptive.loadmanagement.starter.exception.RateLimitExceededException;
import com.adaptive.loadmanagement.starter.model.MetricTelemetryPayload;
import com.adaptive.loadmanagement.starter.model.PriorityTier;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Aspect
public class AdaptiveRateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(AdaptiveRateLimitAspect.class);

    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final io.micrometer.core.instrument.MeterRegistry meterRegistry;
    private final String applicationName;
    private final String instanceId = UUID.randomUUID().toString().substring(0, 8);

    public AdaptiveRateLimitAspect(StringRedisTemplate redisTemplate,
                                   KafkaTemplate<String, Object> kafkaTemplate,
                                   io.micrometer.core.instrument.MeterRegistry meterRegistry,
                                   String applicationName) {
        this.redisTemplate = redisTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.meterRegistry = meterRegistry;
        this.applicationName = applicationName;
    }


    private static final String TELEMETRY_TOPIC = "microservice-telemetry-metrics";

    private final DefaultRedisScript<List> evaluateScript = new DefaultRedisScript<>() {{
        setLocation(new ClassPathResource("scripts/evaluate_limit.lua"));
        setResultType(List.class);
    }};

    private final DefaultRedisScript<Long> releaseScript = new DefaultRedisScript<>() {{
        setLocation(new ClassPathResource("scripts/release_concurrency.lua"));
        setResultType(Long.class);
    }};

    @Around("@annotation(adaptiveRateLimit)")
    public Object enforceAdaptiveLimit(ProceedingJoinPoint joinPoint, AdaptiveRateLimit adaptiveRateLimit) throws Throwable {
        HttpServletRequest request = getHttpServletRequest();
        
        String priorityHeader = (request != null) ? request.getHeader("X-Priority-Tier") : null;
        PriorityTier priority = PriorityTier.fromHeaderOrDefault(priorityHeader);

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String endpointKey = adaptiveRateLimit.key().isEmpty() ? 
                applicationName + ":" + method.getName() : adaptiveRateLimit.key();

        String tokenKey = "rate:limit:" + endpointKey;
        String concurrencyKey = "concurrency:" + endpointKey;

        // Fetch dynamic concurrency limit & current cluster load factor from Redis (Set by Control Plane)
        String currentLimitStr = redisTemplate.opsForValue().get("config:limit:" + endpointKey);
        int dynamicLimit = (currentLimitStr != null) ? Integer.parseInt(currentLimitStr) : adaptiveRateLimit.baseCapacity();

        String loadFactorStr = redisTemplate.opsForValue().get("config:load_factor:" + applicationName);
        double currentLoadFactor = (loadFactorStr != null) ? Double.parseDouble(loadFactorStr) : 0.0;

        // Execute Atomic Redis Lua Evaluation
        List<Object> result = redisTemplate.execute(
                evaluateScript,
                Arrays.asList(tokenKey, concurrencyKey),
                String.valueOf(priority.getLevel()),
                String.valueOf(dynamicLimit),
                String.valueOf(currentLoadFactor),
                String.valueOf(PriorityTier.P1.getMaxAllowedLoadFactor()),
                String.valueOf(PriorityTier.P2.getMaxAllowedLoadFactor()),
                String.valueOf(PriorityTier.P3.getMaxAllowedLoadFactor()),
                String.valueOf(System.currentTimeMillis())
        );

        if (result == null || result.isEmpty()) {
            log.warn("Redis Lua script returned null/empty result. Proceeding without rate limit enforcement.");
            return joinPoint.proceed();
        }

        Long allowedStatus = (Long) result.get(0);
        String reason = (String) result.get(1);

        if (allowedStatus == 0) {
            log.warn("Request THROTTLED for endpoint [{}] Priority [{}] Reason [{}]", endpointKey, priority, reason);
            meterRegistry.counter("uber.ratelimit.requests.total", "tier", priority.name(), "status", "shed").increment();
            throw new RateLimitExceededException(
                    "Request throttled by Adaptive Load Manager: " + reason,
                    reason,
                    priority
            );
        }

        meterRegistry.counter("uber.ratelimit.requests.total", "tier", priority.name(), "status", "allowed").increment();

        long startTime = System.currentTimeMillis();
        boolean isError = false;

        try {
            return joinPoint.proceed();
        } catch (Throwable t) {
            isError = true;
            throw t;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Release Concurrency Counter in Redis
            redisTemplate.execute(releaseScript, List.of(concurrencyKey));

            // Publish Telemetry Payload to Kafka asynchronously
            publishTelemetryAsync(endpointKey, executionTime, isError, priority);
        }
    }

    private void publishTelemetryAsync(String endpointKey, long executionTime, boolean isError, PriorityTier priority) {
        try {
            MetricTelemetryPayload payload = new MetricTelemetryPayload(
                    applicationName,
                    instanceId,
                    endpointKey,
                    executionTime,
                    0,
                    isError,
                    priority.name(),
                    System.currentTimeMillis()
            );

            kafkaTemplate.send(TELEMETRY_TOPIC, endpointKey, payload);
        } catch (Exception e) {
            log.error("Failed to publish telemetry to Kafka", e);
        }
    }


    private HttpServletRequest getHttpServletRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
