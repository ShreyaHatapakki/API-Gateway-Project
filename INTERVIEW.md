# Interview notes — API Gateway project

Study this in your own words. Interviewers prefer a **simple story** plus **why**, not memorized paragraphs.

There is no complete list of “all” questions. Below are the ones that match **this project** and common follow-ups.

---

## 1. Resume (one bullet)

Built a Spring Cloud Gateway–based API Gateway for microservices, implementing centralized routing, JWT authentication, request filtering, logging, Redis-backed rate limiting, and resilient fallback handling.

**Shorter version:**

Built an API Gateway in front of User, Product, and Order services with JWT at the door, Redis rate limiting, and circuit-breaker fallbacks; containerized with Docker Compose.

---

## 2. Two-minute spoken explanation

Use this structure. Pause if they ask a question.

“I built a small **microservices** system. Instead of the client calling three backends, it calls one **API Gateway** on port 8080.

The gateway **routes** `/api/users` to 8081, `/api/products` to 8082, `/api/orders` to 8083. That is a **reverse proxy**: the client thinks it only talked to 8080.

**JWT** is checked at the gateway. No token or a bad token returns **401** before the request hits Product Service. Login is `POST /api/auth/login` on the gateway for this demo.

Every request is **logged** (method, path, status, time). Errors use one **JSON shape**.

**Redis** stores rate-limit counters so a client that sends too many requests gets **429**.

If Product Service is down, a **circuit breaker** stops hammering it and a **fallback** returns **503** with a clear message. Users can still work.

I ran the stack with **Docker Compose**. Inside Docker the gateway calls `product-service` by **name**, not `localhost`, because localhost inside a container is the container itself.”

If they ask what you would do next: real user database, Spring Security, CORS for a real frontend, Eureka/Kubernetes discovery, secrets in environment variables, not YAML.

---

## 3. Architecture diagram (draw this on a whiteboard)

```
CLIENT --> API Gateway :8080
              |
              +--> User :8081
              +--> Product :8082
              +--> Order :8083
              +--> Redis :6379 (counts only)
```

Say: backends are **dummy JSON** (no MySQL) so the learning focus is the **gateway**.

---

## 4. Technologies — what / why / how we used them

| Tech | What it is | Why | In this project |
|------|------------|-----|-----------------|
| **REST API** | HTTP URLs + GET/POST + JSON | Common way programs talk | `/api/products` returns JSON |
| **Spring Boot 3** | Java framework to run servers | Fast setup | All four apps |
| **Spring Cloud Gateway** | API gateway library (WebFlux / Netty, not Tomcat) | Routing, filters | `api-gateway` module |
| **Maven** | Downloads libraries, builds JARs | Standard in Java jobs | `pom.xml` |
| **JWT** | Signed token in three parts | Prove login without DB on every call | `JwtService` + `JwtAuthFilter` |
| **Redis** | Fast in-memory store | Shared counters | Rate limiting |
| **Resilience4j** | Circuit breaker + timeout | Stop calling a dead service | Per-route circuit breaker |
| **Docker / Compose** | Containers + start-many-at-once | Same runtime everywhere | `Dockerfile` + `docker-compose.yml` |

**Do not mix:** Gateway uses **Netty**. User/Product/Order use **Tomcat** (`spring-boot-starter-web`). Do not add `starter-web` on the gateway.

---

## 5. HTTP statuses you should know cold

| Code | Meaning in our project |
|------|------------------------|
| **200** | Success |
| **401** | Missing / invalid / expired JWT, or bad login |
| **404** | No gateway route (example: `GET /`) |
| **429** | Rate limit exceeded |
| **503** | Backend down or fallback |

---

## 6. Interview questions and answers

Answers are written so you can **say** them. Shorten if the interviewer is senior.

### A. Basics

**What is an API?**  
A contract so one program can request data or work from another (what URL, what method, what comes back).

**What is a REST API?**  
An API over HTTP. GET reads, POST creates, and so on. Data is often JSON.

**What is a backend server?**  
A program that waits for requests, does work, returns a response. Ours listen on ports 8080–8083.

**Monolith vs microservices?**  
Monolith: one app does users + products + orders. Microservices: small apps, one job each. Harder to run, easier to scale one part and isolate failures.

**Why an API Gateway?**  
One URL for clients, routing, one place for auth, logging, rate limit, CORS. Clients should not know 20 internal ports.

**What is an API Gateway?**  
A single front door that receives HTTP, picks a backend, forwards, returns the response. Can also filter, authenticate, limit traffic.

**How does a request travel in your project?**  
Postman → Gateway 8080 → match path `/api/products/**` → Product 8082 → JSON back through gateway → client.

**What is routing?**  
Rules: if path looks like X, send to service Y.

**What is a reverse proxy?**  
The gateway forwards to an internal server. The client does not call 8082; 8082 still sees an HTTP request.

**Predicate vs filter?**  
Predicate = *whether* this route matches (Path=/api/products/**). Filter = *extra work* (JWT, rate limit, circuit breaker).

**Global filter vs route filter?**  
Global: every request (our JWT + logging). Route/default: configured on routes (rate limiter, circuit breaker).

---

### B. This project (they will ask “what did YOU build?”)

**What did you build?**  
See the two-minute explanation.

**Which port is the gateway?**  
8080. User 8081, Product 8082, Order 8083, Redis 6379.

**Did backends use a database?**  
No. Hardcoded lists so we could test the gateway.

**Where is JWT checked?**  
Gateway `JwtAuthFilter`, not in Product Service. That is **centralized authentication**.

**Which paths need JWT?**  
`/api/users`, `/api/products`, `/api/orders`. Login `/api/auth/login` is public.

**What is `Authorization: Bearer`?**  
Header format. `Bearer` then a space then the token. Missing prefix → 401.

**How do you get a token?**  
`POST /api/auth/login` with intern/password. Gateway signs a JWT (HS256) with a secret. Demo only.

**What if the token is expired?**  
401, message that it expired. Expiry is 1 hour in config.

**What do you log?**  
Method, path, status, duration in ms. Why: production debugging and slow-request hunting (**observability**).

**Why one error JSON format?**  
Frontend always reads `status` and `message`. Easier support. Fields: timestamp, status, error, message, path.

**What is rate limiting?**  
Cap how many requests a client can send. We count **per IP** in Redis. Over limit → 429. Protects backends from floods.

**Why Redis, not a Java `int`?**  
Memory store, very fast. Survives thinking about **two gateway instances** sharing one count. A local int is per process and resets on restart.

**Token bucket numbers in your YAML?**  
Learning values: replenish 2/sec, burst 4, 1 token per request. First requests 200, then 429, wait, 200 again.

**What is a circuit breaker?**  
Like a fuse. Too many failures → **open** (stop calling). After wait → **half-open** (try a few). Success → **closed** (normal).

**What is a fallback?**  
Backup response when the real service fails. We `forward:/fallback/products` inside the gateway. Client still called `/api/products`.

**Timeout vs circuit breaker?**  
Timeout = max wait for one call. Circuit breaker = stop calling after a pattern of failures.

**If Product is down, do Users fail?**  
They should not. Separate circuit breaker **instances** per service.

**JWT vs fallback order?**  
No token → **401**. We do not return product fallback without auth.

**Load balancing?**  
Spread requests across **copies** of the same service. We did not run two Product instances; we **explained** it.

**Service discovery?**  
A live address book. Service registers “I am product-service at this IP.” Gateway looks up the **name**. We used **static URLs** locally; in Docker, Compose **DNS names** (`product-service`) are a simple form of lookup.

**Why not Eureka?**  
Extra moving part. Fixed ports were enough to learn the gateway.

**What is `localhost` inside Docker?**  
The container itself. Gateway must call `http://product-service:8082` in Compose. YAML uses env vars with localhost **defaults** for IntelliJ.

---

### C. Spring Cloud Gateway (deeper)

**Why WebFlux?**  
Gateway is non-blocking so it can proxy many connections without one thread stuck per wait. We used `Mono` in filters; you do not need to be a WebFlux expert, but say “reactive / non-blocking proxy.”

**What is `spring-cloud-starter-gateway` vs Zuul?**  
Zuul 1 was older (servlet). Spring Cloud Gateway is the current Spring proxy on WebFlux.

**How are routes defined?**  
`application.yml`: `id`, `uri`, `predicates`, `filters`.

**What does `**` mean in Path?**  
This path and anything under it (`/api/products` and `/api/products/1`).

**Did you strip the prefix?**  
No. Backend also uses `/api/products`, so the path is forwarded as-is.

**`KeyResolver`?**  
Chooses the rate-limit **key**. Ours: client IP (`ipKeyResolver`). Could be user id from JWT later.

**`@Order(-2)` on the error handler?**  
Run before Spring’s default error page (`-1`) so the client gets JSON, not Whitelabel HTML.

**Could you use Spring Security?**  
Yes, especially on servlet apps. On this gateway we used a **custom JWT filter** to keep WebFlux security config simpler. Mention that as a next step.

---

### D. JWT (classic questions)

**What is JWT?**  
JSON Web Token: `header.payload.signature`. Payload has claims (we use `sub` = username). Signature proves it was not forged.

**Why signature?**  
If someone changes the payload, the signature no longer matches our secret.

**Access token vs session cookie?**  
Token is sent by the client (often header). Server can be stateless. We still must **protect the secret** and use HTTPS in production.

**Where should JWT be validated in microservices?**  
Often at the gateway (our choice). Some teams also validate on each service. Gateway-only is simpler; a forged internal call to 8082 could skip JWT unless the network is locked.

**Honest limitation:** backends are reachable **directly** on 8081–8083 without JWT. In a company you would not expose them publicly.

---

### E. Redis and rate limiting

**Why 429 not 500?**  
429 means “slow down,” a known API contract. 500 would look like a server bug.

**What if Redis is down?**  
Gateway may fail to start or rate limiting errors. Redis is a **dependency** of this design.

**Distributed rate limit?**  
Redis is shared. Two gateways increment the same keys (with a proper key design).

---

### F. Resilience

**Bulkhead?**  
Limit concurrent calls so one slow API cannot take all threads. We did **not** implement it; say you know the word.

**Retry?**  
Calling again after failure. Dangerous with POST (duplicate orders). We did not add retry.

**How did you test fallback?**  
Stop Product Service in IntelliJ, call GET `/api/products` with JWT, expect 503 fallback JSON; GET `/api/users` still 200.

---

### G. Docker

**Image vs container?**  
Image = template. Container = running instance.

**What is Docker Compose?**  
YAML to start many containers on one network.

**`depends_on`?**  
Start order, **not** “Spring is ready.” First requests can 503 until Java starts.

**Multi-stage Dockerfile?**  
Build with Maven image, run with smaller JRE image so the final box is not full of Maven.

---

### H. Behavioral / “weakness of your project”

Always have one improvement:

- Demo password and JWT secret in YAML  
- No HTTPS  
- Backends not private  
- No real users DB  
- Rate limit per IP (NAT shares IPs)  
- Low rate limits for learning  
- No automated tests  
- No CORS config for a browser app  

Interviewers **like** this. It shows you understand production.

---

### I. Rapid-fire

**Idempotent?** GET should be. POST login is not “read-only.”  
**CORS?** Browser rule for other origins; we did not add it yet.  
**Idempotency key?** Not in this project.  
**API versioning?** We used `/api/...` without `/v1`.  
**Observability vs logging?** Logging is one signal. Metrics and tracing are others (not built).  
**Netflix OSS?** Eureka, old Zuul; we used Spring Cloud Gateway + Resilience4j.

---

## 7. Questions you can ask THEM

- Do you put auth on the gateway, on each service, or both?  
- Do you use Kubernetes ingress plus a gateway, or only one?  
- How do you store JWT secrets and rotate them?

Asking one good question looks senior.

---

## 8. Practice task they might give in an interview

“Add Payment Service on 8084, route `/api/payments/**`.”

You should be able to:

1. Copy product-service pattern  
2. Add a route in gateway YAML  
3. Protect path in `JwtAuthFilter`  
4. Add circuit breaker + fallback  
5. Add Compose service + `PAYMENT_SERVICE_URI`

Do not freeze. Talk while you do it.

---

## 9. Cheat sheet (memorize)

- Gateway **8080** = only public door  
- Route by **path**  
- JWT at door → **401**  
- Redis count → **429**  
- Dead backend → breaker + **503** fallback  
- Docker: **names** not localhost  
- Discovery = **where is the service now**  
- Load balancing = **which copy gets this request**
