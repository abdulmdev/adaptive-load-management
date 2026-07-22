# Driver Microservice (`driver-service`)

Secondary business microservice handling real-time driver location updates and driver availability logic.

---

## 🛠 Endpoints

- **`POST /api/v1/drivers/location`**: Driver GPS Location update ping (Tagged as **Priority $P0$**).

---

## 🚀 Running Driver Service
```bash
mvn spring-boot:run -pl driver-service
```
Port: `8082`  
Actuator Metrics: `http://localhost:8082/actuator/prometheus`
