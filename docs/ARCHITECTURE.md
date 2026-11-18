# 🏗️ Architecture & Technical Specification

## 1. System Overview

The **URL Shortener** application is designed as a distributed, containerized monorepo split into two primary layers:
* **Presentation Tier**: React SPA (Single Page Application) with Vite & TypeScript.
* **Application / API Tier**: Spring Boot 4 RESTful service with a code-level CQRS
  application layer and Spring Data JPA.
* **Persistence Tier**: PostgreSQL 15.19 with Liquibase for normal local and deployed
  runs; in-memory H2 remains available for isolated default-profile development and tests.
* **Identity Tier**: The sibling `auth-server` is the OAuth 2.0 Authorization Server and
  OpenID Connect Provider. The backend is both a confidential OAuth client/BFF for the
  React application and a JWT resource server for service clients.

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

### Backend Application Boundaries

The backend applies code-level Command Query Responsibility Segregation (CQRS):

- REST controllers translate HTTP input into explicit command or query records.
- `UrlCommandService` owns state-changing use cases: link creation, soft deletion,
  and atomic click recording.
- `UrlQueryService` owns read-only use cases: listing links, retrieving link details or
  statistics, and resolving public short codes.
- `UrlResponseMapper` is shared projection logic so both sides produce consistent API
  responses without coupling query operations to command handling.

This is an in-process application design, not distributed CQRS. Commands and queries
share the same Spring Boot deployment, `UrlRepository`, entity model, and database.
There is no message broker, event sourcing, separate read store, or eventual-consistency
contract. Query-service transactions are marked read-only; command handlers define
write transactions.

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
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    owner_uid UUID,
    owner_client_id VARCHAR(100)
);

CREATE INDEX idx_urls_short_code ON urls(short_code);
CREATE INDEX idx_urls_custom_alias ON urls(custom_alias);
```

Liquibase owns PostgreSQL schema evolution and Hibernate validates the resulting model.
The initial changeset creates the schema on a new database or marks itself applied when
adopting the legacy Hibernate-created `urls` table. H2 remains disposable and is generated
from JPA mappings only when the default profile is started directly.

The ownership changeset adds `owner_uid` and `owner_client_id` to both new and adopted
schemas. Existing rows remain unowned and are intentionally visible only to `URL_ADMIN`.

Local Auth Server and URL Shortener processes share one container named `postgres-db`, while
retaining separate databases, roles, and Liquibase histories. Production preserves the same
database-per-service boundary with independently configured PostgreSQL endpoints.

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
| `owner_uid` | `UUID` | Immutable auth-server `uid` for a human-owned link. |
| `owner_client_id` | `VARCHAR(100)` | OAuth client ID for a machine-owned link. Mutually exclusive with `owner_uid`. |

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
2. `RedirectController` sends `ResolveShortCodeQuery` to the read-only query service,
   which checks canonical `shortCode`; `customAlias` remains a compatibility fallback
   for older rows.
3. If non-existent, inactive, or expired: returns `404 Not Found`.
4. If valid:
   - Sends `RecordClickCommand` to the command service, which increments `click_count`
     synchronously and atomically.
   - Sets `HttpHeaders.LOCATION` to `originalUrl`.
   - Returns HTTP `302 Found` (temporary redirect) to ensure subsequent clicks hit the server for accurate analytics tracking.

---

## 5. Security & Production Considerations

### Authentication and authorization

The React browser starts Authorization Code login through
`/oauth2/authorization/auth-server`. The auth-server UI supports Google and explicitly
invited local accounts. The backend uses PKCE as a confidential client,
keeps access and rotated refresh tokens server-side, and exposes only its HttpOnly
session cookie plus a CSRF token to the SPA. `GET /api/auth/session` returns the current
display identity, audience-scoped roles, and CSRF token; it never returns OAuth tokens.

The same backend accepts bearer JWTs for automation. JWT validation checks the auth-server
signature, exact issuer, expiry, and the `url-shortener-api` audience. `urls.read` gates
reads and `urls.write` gates mutations. Human tokens must have `subject_type=user` and a
valid UUID `uid`; machine tokens must have `subject_type=client` and `client_id`, and are
rejected if they contain human roles.

Data visibility is resolved before mapping entities to API responses:

| Identity | Visible/manageable URLs |
| :--- | :--- |
| User | URLs whose `owner_uid` matches the token `uid`. |
| User with `URL_ADMIN` | All URLs, including legacy unowned rows. |
| Service client | URLs whose `owner_client_id` matches its `client_id`; it cannot impersonate a user. |

Public `GET /{shortCode}` redirects remain anonymous. All URL management and analytics
endpoints require authentication. Existing rows without either owner field are intentionally
admin-only instead of being assigned to an arbitrary user. The management command and
query messages carry the resolved actor into their respective CQRS handlers, while public
redirect resolution and atomic click recording remain anonymous.

* **Non-Root Docker execution**: Backend runs under the unprivileged `spring:spring` user; the frontend uses the pinned Nginx Alpine slim runtime. Local PostgreSQL uses the pinned official PostgreSQL image and binds only to host loopback.
* **CORS Policy**: Configured via `app.cors.allowed-origins` in `application.yml` and injected into `WebConfig`. Supports environment override via `${CORS_ALLOWED_ORIGIN}`.
* **Reserved Path Routing & Collision Prevention**: Injected via `AppProperties.reservedPaths`. Protects system endpoints (`/actuator/**`, `/swagger-ui/**`, `/v3/api-docs`, `/api/**`, `/favicon.ico`) from being hijacked by custom aliases or intercepted by the `/{shortCode}` wildcard.
* **Input Validation**: Uses `jakarta.validation` annotations (`@NotBlank`, `@URL`, `@Size`) to sanitize inputs.
* **Production Profile Hardening**: Swagger UI and OpenAPI documentation are disabled in production; Actuator is restricted to health checks only.
