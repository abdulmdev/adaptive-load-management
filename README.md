# 🚗 Distributed Adaptive Load Management System

![Java](https://img.shields.io/badge/Java-21-orange.svg) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg) ![Kafka](https://img.shields.io/badge/Apache%20Kafka-Distributed-black.svg) ![Redis](https://img.shields.io/badge/Redis-Lua-red.svg) ![Grafana](https://img.shields.io/badge/Grafana-Observability-blue.svg)

A distributed closed-loop rate limiter designed to protect microservices from cascading failures. Using **TCP Vegas congestion control mathematics** and **Priority-Based Load Shedding**, this system dynamically shrinks concurrency limits when database degradation is detected, intentionally shedding low-priority background traffic to guarantee 100% success rates for critical user flows.

## 🌟 Key Features

*   **TCP Vegas Congestion Control:** Calculates real-time system health using the gradient of `MinRTT / SmoothedRTT`. If latency spikes, the limit is mathematically choked.
*   **Priority-Based Load Shedding:** Differentiates between `P0` (Critical - e.g., Checkout) and `P2` (Background - e.g., Feed Fetching). When degraded, `P2` traffic is violently shed (HTTP 429) to keep `P0` alive.
*   **Custom Spring Boot Starter:** A plug-and-play starter containing an Aspect (`@AdaptiveRateLimit`) that wraps around critical endpoints.
*   **Distributed Token Bucket:** Uses atomic Redis Lua scripts to enforce strict concurrency limits across a fleet of stateless microservices.
*   **Asynchronous Control Plane:** Microservices stream latency telemetry non-blockingly to **Apache Kafka**. A central Control Plane consumes this, calculates the new limits, and updates Redis.
*   **End-to-End Observability:** Heavily instrumented with Micrometer, exporting custom gauges and counters to **Prometheus**, visualized in **Grafana**.

---

## 🏗️ High-Level System Architecture (HLD)

```mermaid
flowchart TD
    %% Tiers
    Clients["📱 Global Clients (Web, Mobile, 3rd Party)"]
    WAF["🛡️ WAF & Global Load Balancer"]
    Gateway["🚪 API Gateway (Service Mesh Ingress)"]
    
    subgraph AppTier["⚙️ Application Tier (Stateless)"]
        Order["📦 Order Service"]
        Driver["🚗 Driver Service"]
        Payment["💳 Payment Gateway Stub"]
    end

    subgraph DataTier["🗄️ Data & Caching Tier"]
        Redis[("⚡ Redis Cluster (Rate Limits)")]
        DB_Primary[("🐘 PostgreSQL (Primary)")]
        DB_Replica[("🐘 PostgreSQL (Replica)")]
    end

    %% Flows
    Clients -->|HTTPS| WAF
    WAF -->|HTTPS| Gateway
    Gateway -->|Internal HTTP/2| Order
    Gateway -->|Internal HTTP/2| Driver

    Order -.->|External API| Payment

    Order <-->|Atomic Lua Scripts| Redis
    Driver <-->|Atomic Lua Scripts| Redis

    Order -->|JDBC| DB_Primary
    Driver -->|JDBC| DB_Primary
    DB_Primary -.->|Async Replication| DB_Replica

    %% Styling
    classDef edge fill:#fff3e0,stroke:#e65100,stroke-width:2px;
    classDef service fill:#e1f5fe,stroke:#0277bd,stroke-width:2px;
    classDef db fill:#f1f8e9,stroke:#558b2f,stroke-width:2px;
    
    class Clients,WAF edge;
    class Gateway,Order,Driver,Payment service;
    class Redis,DB_Primary,DB_Replica db;
```

## 🔍 Adaptive Control Plane & Observability (Deep Dive)

```mermaid
flowchart TD
    Microservice["⚙️ Microservices (with @AdaptiveRateLimit)"]
    
    subgraph ControlPlane["🧠 Feedback Loop (Control Plane)"]
        Kafka["📨 Apache Kafka (Telemetry Topic)"]
        CP["🧮 Control Plane (TCP Vegas Math)"]
        Redis[("⚡ Redis (Token Buckets)")]
    end

    subgraph ObsStack["📊 Observability Stack"]
        Prometheus["📈 Prometheus Server"]
        Grafana["🖥️ Grafana Dashboards"]
    end

    %% Async Telemetry Loop
    Microservice -.->|1. Async Fire-and-Forget Latency| Kafka
    Kafka -->|2. Batch Consume| CP
    CP -->|3. Calculate Gradient & Choke Limit| CP
    CP -->|4. Persist Dynamic Limit| Redis
    Microservice <-->|5. Check Token Bucket (Drop P2)| Redis

    %% Metrics Pipeline
    Prometheus -.->|Scrape Metrics| Microservice
    Prometheus -.->|Scrape Metrics| CP
    Grafana -.->|PromQL Queries| Prometheus

    %% Styling
    classDef loop fill:#f3e5f5,stroke:#6a1b9a,stroke-width:2px;
    classDef obs fill:#fffde7,stroke:#f57f17,stroke-width:2px;
    
    class Kafka,CP,Redis loop;
    class Prometheus,Grafana obs;
```

---

## 📈 Grafana Dashboard

*When database latency spikes, the Control Plane immediately detects the degradation, drops the dynamic concurrency limit, and sheds background traffic to protect the system.*

> **![Grafana Dashboard Showcase](./dashboard.png)**
> *(Note: Replace this image with a screenshot of your local Grafana dashboard showing the load test results!)*

---

## 🧠 The Mathematics (TCP Vegas)

The Control Plane recalculates the global concurrency limit every 5 seconds using the TCP Vegas congestion algorithm.

1.  **Min RTT:** The absolute fastest the system has ever responded (baseline health).
2.  **Smoothed RTT:** The exponentially weighted moving average of the current latency.
3.  **Gradient:** `Min RTT / Smoothed RTT`

```java
double gradient = (double) minRtt / smoothedRtt;

if (gradient < 0.95) {
    // DEGRADATION DETECTED: Multiplicative Decrease (Choke)
    newLimit = currentLimit * gradient; 
} else {
    // HEALTHY: Additive Increase (Probe for capacity)
    newLimit = currentLimit + BETA; 
}
```

---

## 🚀 How to Run Locally

### 1. Start the Infrastructure
Spin up the entire cluster (Postgres, Redis, Kafka, Zookeeper, Prometheus, Grafana, and the 4 Spring Boot microservices) using Docker Compose:

```bash
docker-compose up -d --build
```

### 2. View the Observability Dashboard
Navigate to **http://localhost:3000**
*   **Username:** `admin`
*   **Password:** `admin`
*   Open the **Adaptive Load Management** dashboard.

### 3. Run the Load Test Showcase
We will use `k6` to simulate high traffic, and then artificially inject a database degradation to watch the system adapt.

**Step A: Start the K6 Load Test**
```bash
docker run --rm -i --network adaptive-load-network -v $PWD/load-tests:/load-tests grafana/k6 run /load-tests/load_test.js
```

**Step B: Inject Database Degradation**
*Wait 5 seconds after starting the load test, then run this to simulate an 800ms database failure:*
```bash
curl -X POST "http://localhost:8080/api/v1/orders/admin/simulate-degradation?delayMs=800"
```

**Step C: Watch the Magic in Grafana**
Look at your dashboard! You will instantly see:
1.  **Latency** spike to 800ms.
2.  **Concurrency Limit** plunge downwards.
3.  **Requests Chart** turn red as `P2` background traffic is ruthlessly shed (HTTP 429), while `P0` critical traffic continues to succeed 100%.

---

## ⚖️ Architecture Trade-offs & Future Scaling

While this architecture uses Redis for atomic, distributed limit enforcement (which is highly effective and robust for clusters handling up to 5,000 Requests Per Second), centralizing enforcement creates a network bottleneck on the critical path at extreme scale.

To scale this system to 1,000,000+ RPS, the enforcement mechanism must be decoupled from Redis. The future migration path involves moving enforcement to **Local JVM Memory** (using a `Guava RateLimiter` or `Semaphore` per pod). The Control Plane would remain asynchronous, bypassing Redis entirely by broadcasting the sharded limits directly to the pods via a fast pub/sub mechanism. This removes all network hops from the critical path while maintaining the adaptive TCP Vegas closed-loop algorithm.
