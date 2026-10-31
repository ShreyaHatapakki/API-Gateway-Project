# API Gateway — Microservices

A Spring Cloud Gateway that acts as a **single entry point** for three small backend services. This is a portfolio project focused on gateway patterns (routing, JWT at the edge, Redis rate limiting, logging, error handling, and circuit-breaker fallbacks), not a full business domain.

User, Product, and Order services return **hardcoded JSON**. There is **no database**.

## Architecture

```
Client (browser / Postman)
        |
        v
 API Gateway  :8080
        |
        +---> User Service     :8081   GET /api/users
        +---> Product Service  :8082   GET /api/products
        +---> Order Service    :8083   GET /api/orders
        |
        +---> Redis            :6379   rate-limit counters
```

| Public path | Destination |
|-------------|-------------|
| `GET /api/users/**` | User Service |
| `GET /api/products/**` | Product Service |
| `GET /api/orders/**` | Order Service |
| `POST /api/auth/login` | Login on the **gateway** (issues JWT) |

## Technologies

- Java 17
- Spring Boot 3.3
- Spring Cloud Gateway
- Maven (multi-module)
- JWT (JJWT) at the gateway
- Redis + Spring Cloud Gateway `RequestRateLimiter`
- Resilience4j circuit breaker and timeouts
- Docker and Docker Compose

The gateway uses **Netty** (WebFlux). The three backend services use **Tomcat** (`spring-boot-starter-web`).

## Features (implemented)

- **Centralized routing** from port 8080 to the matching service
- **JWT authentication at the gateway** for `/api/users`, `/api/products`, and `/api/orders`
- **Request logging**: HTTP method, path, status, duration
- **JSON error responses** (same fields: timestamp, status, error, message, path)
- **Redis rate limiting** per client IP (learning limits: 2 tokens/second, burst 4)
- **Circuit breaker + fallback** per service (503 JSON if the backend is down or too slow)
- **Docker Compose** for gateway, three services, and Redis

## Not included

These were not built, so they are not claimed here:

- MySQL / JPA
- Eureka or Kubernetes service discovery
- Running multiple instances with load balancing
- Spring Security filter chain (JWT is a custom gateway filter)
- Real user registration or a user database (demo login is hardcoded for local testing)
- Automated tests
- A frontend

## Modules

| Module | Port | Role |
|--------|------|------|
| `api-gateway` | 8080 | Routing, JWT, logging, errors, rate limit, fallbacks |
| `user-service` | 8081 | Sample `GET /api/users` |
| `product-service` | 8082 | Sample `GET /api/products` |
| `order-service` | 8083 | Sample `GET /api/orders` |

## API

### Login

- **POST** `http://localhost:8080/api/auth/login`
- Header: `Content-Type: application/json`
- Body:

```json
{
  "username": "intern",
  "password": "password"
}
```

- **200:** `{ "token": "..." }`
- Wrong credentials: **401**

Demo username and password are **hardcoded in the gateway** for local testing only. Do not reuse this pattern in production.

### Protected resources

Send:

`Authorization: Bearer <token>`

| Method | URL | Success |
|--------|-----|---------|
| GET | `http://localhost:8080/api/users` | 200, sample users |
| GET | `http://localhost:8080/api/products` | 200, sample products |
| GET | `http://localhost:8080/api/orders` | 200, sample orders |

| Situation | Status |
|-----------|--------|
| Missing or invalid token | 401 |
| Unknown path (for example `GET /`) | 404 |
| Too many requests | 429 |
| Backend down (with a valid token) | 503 fallback |

Direct calls to `8081` / `8082` / `8083` skip the gateway (no JWT). That is expected in this local demo.

## Run on the host (IntelliJ or Maven)

1. Java 17 and Maven.
2. Redis on `localhost:6379`, for example:

```bash
docker run -d --name internship-redis -p 6379:6379 redis:7
```

3. Start `user-service`, `product-service`, `order-service`, then `api-gateway`.
4. Login, then call the GET APIs with the Bearer token.

Do not run IntelliJ and Docker Compose at the same time (same ports).

Optional: set `JWT_SECRET` to a random string of at least 32 characters. If it is unset, a local-dev default is used (not for production).

## Run with Docker Compose

Stop anything using ports 8080–8083 and 6379, then from the repository root:

```bash
docker compose up --build
```

The first build downloads Maven images and can take several minutes.

Inside Compose, the gateway calls services by **container name** (`http://product-service:8082`), not `localhost`.

Stop:

```bash
docker compose down
```

## Configuration notes

- Route URIs and Redis host support environment variables (`USER_SERVICE_URI`, `PRODUCT_SERVICE_URI`, `ORDER_SERVICE_URI`, `REDIS_HOST`) with localhost defaults for IDE runs.
- Copy `.env.example` to `.env` if you want to set `JWT_SECRET` for Compose. `.env` is gitignored.

## Interview notes

`INTERVIEW.md` has a short project pitch and common Q&A aligned with this codebase.
