package com.adaptive.loadmanagement.starter.annotation;

import com.adaptive.loadmanagement.starter.model.PriorityTier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation to enable Uber-style Dynamic Adaptive Rate Limiting & Concurrency Control on Controller methods.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AdaptiveRateLimit {
    
    /**
     * Unique key identifier for the endpoint or resource rate limit bucket.
     */
    String key() default "";

    /**
     * Fallback priority tier if request header X-Priority-Tier is missing.
     */
    PriorityTier defaultPriority() default PriorityTier.P2;

    /**
     * Maximum baseline capacity before adaptive throttling kicks in.
     */
    int baseCapacity() default 100;
}
