package com.adaptive.loadmanagement.starter.config;

import com.adaptive.loadmanagement.starter.aspect.AdaptiveRateLimitAspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
@ConditionalOnProperty(name = "adaptive.limiter.enabled", havingValue = "true", matchIfMissing = true)
public class AdaptiveLimiterAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AdaptiveRateLimitAspect adaptiveRateLimitAspect(
            StringRedisTemplate redisTemplate,
            KafkaTemplate<String, Object> kafkaTemplate,
            io.micrometer.core.instrument.MeterRegistry meterRegistry,
            @Value("${spring.application.name:default-service}") String applicationName) {
        return new AdaptiveRateLimitAspect(redisTemplate, kafkaTemplate, meterRegistry, applicationName);
    }
}
