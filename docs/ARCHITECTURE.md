# 🏗️ Architecture & Technical Specification

## 1. System Overview

The **URL Shortener** application is designed as a distributed, containerized monorepo split into two primary layers:
* **Presentation Tier**: React SPA (Single Page Application) with Vite & TypeScript.
* **Application / API Tier**: Spring Boot 3 RESTful microservice with Spring Data JPA.
* **Persistence Tier**: In-memory H2 (Local development) or PostgreSQL 15 (Docker / Production).

```
                 ┌────────────────────────────────┐
                 │       Client Browser / User    │
                 └──────────────┬─────────────────┘
                                │
                                ▼
            ┌──────────────────────────────────────────┐
            │  Nginx Web Server / Frontend Container   │
            │  (Port 80) — Serves Vite React Bundle    │
            └─────────┬──────────────────────┬─────────┘
                      │                      │
       API Requests   │                      │  Redirects (GET /{code})
      /api/urls/**    │                      │
                      ▼                      ▼
            ┌──────────────────────────────────────────┐
            │       Spring Boot Backend Service        │
            │       (Port 8080) — Java 17 + REST       │
            └─────────────────────┬────────────────────┘
                                  │
                                  ▼
            ┌──────────────────────────────────────────┐
            │          PostgreSQL 15 Database          │
            │          (Port 5432) — URL Mappings      │
            └──────────────────────────────────────────┘
```

---

## 2. Data Model & Database Schema

The core domain model is centered around the `url_mappings` table:

```sql
CREATE TABLE url_mappings (
    id BIGSERIAL PRIMARY KEY,
    original_url TEXT NOT NULL,
    short_code VARCHAR(50) NOT NULL UNIQUE,
    click_count BIGINT DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX idx_url_mappings_short_code ON url_mappings(short_code);
```

### Entity Fields
| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | `BIGINT` | Auto-incrementing primary key. |
| `original_url` | `VARCHAR/TEXT` | Destination URL provided by the user. |
| `short_code` | `VARCHAR(50)` | Unique 7-character token or custom alias. |
| `click_count` | `BIGINT` | Total count of successful redirects. |
| `created_at` | `TIMESTAMP` | Timestamp when the shortlink was generated. |
| `updated_at` | `TIMESTAMP` | Timestamp of last modification. |
| `expires_at` | `TIMESTAMP` | Optional expiration date/time. |

---

## 3. Shortening Algorithm & Collision Strategy

1. **Custom Alias Mode**:
   - If user supplies a custom alias, regex validation ensures `^[a-zA-Z0-9_-]{3,50}$`.
   - Checks `existsByShortCode(alias)`. If found, throws `409 Conflict`.
2. **Auto-Generated Mode**:
   - Uses a Base62 alphabet (`[a-zA-Z0-9]`).
   - Generates a 7-character pseudorandom string ($62^7 \approx 3.5 \times 10^{12}$ unique possibilities).
   - Verifies uniqueness against repository; in case of collision, retries with backoff.

---

## 4. Redirect & Analytics Lifecycle

When a client hits `GET http://localhost:8080/{shortCode}`:
1. Controller queries `UrlMappingRepository.findByShortCode(shortCode)`.
2. If non-existent: returns `404 Not Found` with custom error payload.
3. If found:
   - Increments `click_count` atomically.
   - Sets `HttpHeaders.LOCATION` to `originalUrl`.
   - Returns HTTP `302 Found` (temporary redirect) to ensure subsequent clicks hit the server for accurate analytics tracking.

---

## 5. Security & Production Considerations

* **Non-Root Docker execution**: Backend runs under `spring:spring` unprivileged user; Frontend runs under `nginx:alpine`.
* **CORS Policy**: Configured in Spring Boot to allow frontend requests from configured domain origins.
* **Input Validation**: Uses `jakarta.validation` annotations (`@NotBlank`, `@URL`, `@Size`) to sanitize inputs.
