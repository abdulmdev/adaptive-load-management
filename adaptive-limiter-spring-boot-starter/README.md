# Adaptive Limiter Spring Boot Starter (`adaptive-limiter-spring-boot-starter`)

Custom Spring Boot Starter providing automatic AOP-driven latency gradient concurrency control, atomic Redis Lua token bucket evaluation, and real-time Kafka telemetry streaming.

---

## 📦 Features

- **Annotation-Driven Enforcement**: Simply annotate any REST or gRPC endpoint with `@AdaptiveRateLimit`.
- **Atomic Redis Lua Execution**: Single pass evaluation of tokens, concurrency limits, and load factors in `< 1ms`.
- **Async Kafka Telemetry**: Streams endpoint execution times ($RTT$), error statuses, and in-flight request counts to Kafka without blocking client responses.

---

## 🛠 How to Use in a Microservice

### 1. Add Dependency to `pom.xml`
```xml
<dependency>
    <groupId>com.uber.loadmanagement</groupId>
    <artifactId>adaptive-limiter-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. Enable Auto-Configuration in `application.yml`
```yaml
adaptive:
  limiter:
    enabled: true
```

### 3. Annotate Controller Methods
```java
@PostMapping("/checkout")
@AdaptiveRateLimit(
    key = "order-service:checkout",
    defaultPriority = PriorityTier.P0,
    baseCapacity = 50
)
public ResponseEntity<OrderResponse> checkout() {
    return ResponseEntity.ok(orderService.processCheckout());
}
```

---

## 📜 Core Components

- `AdaptiveRateLimit.java`: Target method annotation.
- `AdaptiveRateLimitAspect.java`: Spring AOP aspect intercepting calls and calling Redis Lua.
- `evaluate_limit.lua`: Single-pass Lua script handling rate limits & priority shedding.
- `release_concurrency.lua`: Lua script releasing concurrency locks upon request completion.
