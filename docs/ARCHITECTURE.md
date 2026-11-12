# 🏗️ Architecture & Technical Specification

## 1. System Overview

The **URL Shortener** application is designed as a distributed, containerized monorepo split into two primary layers:
* **Presentation Tier**: React SPA (Single Page Application) with Vite & TypeScript.
* **Application / API Tier**: Spring Boot 4 RESTful microservice with Spring Data JPA.
* **Persistence Tier**: In-memory H2 (Local development) or PostgreSQL 15.19 (Docker; image pinned to Alpine 3.24).

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
            │       (Port 8080) — Java 21 + REST       │
            └─────────────────────┬────────────────────┘
                                  │
                                  ▼
            ┌──────────────────────────────────────────┐
            │        PostgreSQL 15.19 Database         │
            │          (Port 5432) — URL Mappings      │
            └──────────────────────────────────────────┘
```

---

## 2. Data Model & Database Schema

The core domain model is centered around the `urls` table:

```sql
CREATE TABLE urls (
    id BIGSERIAL PRIMARY KEY,
    original_url VARCHAR(2048) NOT NULL,
    short_code VARCHAR(50) NOT NULL UNIQUE,
    custom_alias VARCHAR(50),
    title VARCHAR(255),
    click_count BIGINT DEFAULT 0 NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITHOUT TIME ZONE,
    is_active BOOLEAN DEFAULT TRUE NOT NULL
);

CREATE INDEX idx_urls_short_code ON urls(short_code);
CREATE INDEX idx_urls_custom_alias ON urls(custom_alias);
```

During development, Hibernate creates the disposable H2 schema and updates the local
Docker PostgreSQL schema from the JPA mappings. Before the first persistent or shared
deployment, Liquibase will become the schema owner and Hibernate will only validate it.

### Entity Fields (`Url.java`)
| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | `BIGINT` | Auto-incrementing primary key. |
| `original_url` | `VARCHAR(2048)` | Destination URL provided by the user. |
| `short_code` | `VARCHAR(50)` | Canonical unique public token (custom alias or Sqid). |
| `custom_alias` | `VARCHAR(50)` | Optional custom user-defined alias. |
| `title` | `VARCHAR(255)` | Optional descriptive label/title. |
| `click_count` | `BIGINT` | Total count of successful redirects. |
| `created_at` | `TIMESTAMP` | Timestamp when the shortlink was generated. |
| `updated_at` | `TIMESTAMP` | Timestamp of last modification. |
| `expires_at` | `TIMESTAMP` | Optional expiration date/time. |
| `is_active` | `BOOLEAN` | Soft-delete and active status flag. |

---

## 3. Shortening Algorithm & Collision Strategy

1. **Custom Alias Mode**:
   - If user supplies a custom alias, regex validation ensures `^[a-zA-Z0-9_-]{3,50}$`.
   - Normalizes the alias and stores it as both the canonical `short_code` and alias metadata.
   - Checks the shared public-code namespace. If found, throws `409 Conflict`.
2. **Auto-Generated Mode**:
   - Persists the URL to obtain its numeric database ID.
   - Encodes that ID with [Sqids](https://sqids.org/) using a minimum output length of 7.
   - Sqids' standard blocklist is extended with the application's reserved routes.
   - If an encoded value conflicts with a custom alias or legacy alias, the ID is encoded with a small collision discriminator and retried.
   - The unique constraint on `short_code` remains the final database-level integrity guarantee.

Sqids are reversible identifiers, not encryption. They prevent sequential IDs from being exposed directly but should not be treated as secrets or authorization tokens.

---

## 4. Redirect & Analytics Lifecycle

When a client hits `GET http://localhost:8080/{shortCode}`:
1. Controller checks `AppProperties.reservedPaths` to prevent routing collisions.
2. Queries the canonical `shortCode`; `customAlias` is checked only as a compatibility fallback for older rows.
3. If non-existent, inactive, or expired: returns `404 Not Found`.
4. If valid:
   - Increments `click_count` atomically.
   - Sets `HttpHeaders.LOCATION` to `originalUrl`.
   - Returns HTTP `302 Found` (temporary redirect) to ensure subsequent clicks hit the server for accurate analytics tracking.

---

## 5. Security & Production Considerations

* **Non-Root Docker execution**: Backend runs under the unprivileged `spring:spring` user; the frontend uses the pinned Nginx Alpine slim runtime. The PostgreSQL entrypoint drops privileges through a pinned-source `gosu` build.
* **CORS Policy**: Configured via `app.cors.allowed-origins` in `application.yml` and injected into `WebConfig`. Supports environment override via `${CORS_ALLOWED_ORIGIN}`.
* **Reserved Path Routing & Collision Prevention**: Injected via `AppProperties.reservedPaths`. Protects system endpoints (`/actuator/**`, `/swagger-ui/**`, `/v3/api-docs`, `/api/**`, `/favicon.ico`) from being hijacked by custom aliases or intercepted by the `/{shortCode}` wildcard.
* **Input Validation**: Uses `jakarta.validation` annotations (`@NotBlank`, `@URL`, `@Size`) to sanitize inputs.
* **Production Profile Hardening**: Swagger UI and OpenAPI documentation are disabled in production; Actuator is restricted to health checks only.
