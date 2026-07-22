# Order Microservice (`order-service`)

Core business microservice representing order creation and retrieval endpoints, integrated with the `@AdaptiveRateLimit` SDK starter.

---

## 🛠 Features & Endpoints

- **`POST /api/v1/orders`**: Create Order endpoint (Tagged as **Priority $P0$**).
- **`GET /api/v1/orders`**: List Orders endpoint (Tagged as **Priority $P2$**).
- **`POST /api/v1/orders/admin/simulate-degradation?delayMs=500`**: Artificial Database Latency Simulator endpoint used for testing adaptive backpressure & load shedding.

---

## 🚀 Running Order Service
```bash
mvn spring-boot:run -pl order-service
```
Port: `8081`  
Actuator Metrics: `http://localhost:8081/actuator/prometheus`
