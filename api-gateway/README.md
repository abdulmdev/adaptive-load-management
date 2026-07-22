# API Gateway (`api-gateway`)

Spring Cloud Gateway Ingress routing requests to downstream microservices and inspecting Priority Tier HTTP headers (`X-Priority-Tier`).

---

## 🛠 Ingress Routes

- `/api/v1/orders/**` $\rightarrow$ `http://localhost:8081` (`order-service`)
- `/api/v1/drivers/**` $\rightarrow$ `http://localhost:8082` (`driver-service`)

---

## 🚀 Running API Gateway
```bash
mvn spring-boot:run -pl api-gateway
```
Port: `8080`  
Actuator Metrics: `http://localhost:8080/actuator/prometheus`
