# Control Plane Microservice (`control-plane-service`)

Centralized closed-loop controller microservice that listens to cluster telemetry streams via Kafka and dynamically recalculates node concurrency limits using the **TCP Vegas Gradient Algorithm**.

---

## ⚙️ Core Responsibilities

1. **Kafka Telemetry Ingestion**: Listens to topic `microservice-telemetry-metrics` and updates sliding window execution latency ($RTT$) statistics per endpoint.
2. **TCP Vegas Gradient Algorithm**:
   - $\text{Gradient} = \frac{\text{MinRTT}}{\text{SmoothedRTT}}$
   - If $\text{Gradient} < 0.95 \implies$ Multiplicative decrease ($\text{NewLimit} = \max(\text{MinLimit}, \text{Limit} \times \text{Gradient})$).
   - If $\text{Gradient} \ge 0.95 \implies$ Additive increase ($\text{NewLimit} = \text{Limit} + 1.5$).
3. **State Push to Redis**: Updates keys `config:limit:{endpoint}` and `config:load_factor:{service}` in Redis every 1000ms.
4. **Prometheus Gauge Export**: Exports live metrics (`adaptive_concurrency_limit`, `adaptive_load_factor`) for Grafana visualization.

---

## 🚀 Running the Control Plane
```bash
mvn spring-boot:run -pl control-plane-service
```
Port: `8090`  
Actuator Metrics: `http://localhost:8090/actuator/prometheus`
