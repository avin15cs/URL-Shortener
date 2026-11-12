# 📜 Project Activity & Engineering Log

This document tracks every major engineering milestone, architectural decision, and implementation step performed in the **URL-Shortener** monorepo.

---

## 📅 Timeline & Milestone Summary

| Milestone | Date / Version | Status | Key Deliverables |
| :--- | :--- | :--- | :--- |
| **1. Monorepo Architecture Setup** | v1.0.0-alpha | ✅ Completed | Monorepo structure created with `frontend/` and `backend/`. |
| **2. Spring Boot 4 Backend** | v1.0.0-alpha | ✅ Completed | Java 21, Spring Web, Spring Data JPA, H2 + PostgreSQL support, URL hashing, redirects, atomic clicks. |
| **3. React + Vite Frontend** | v1.0.0-alpha | ✅ Completed | Modern glassmorphic SPA, TypeScript, Lucide icons, Toast alerts, instant shortcode copy, stats dashboard. |
| **4. Docker Containerization** | v1.0.0-beta | ✅ Completed | Multi-stage Dockerfiles for backend & frontend, `docker-compose.yml` with PostgreSQL 15. |
| **5. Remote Git & Branching Strategy** | v1.0.0-rc | ✅ Completed | Repository linked to `avin15cs/URL-Shortener`, branches initialized: `main`, `release`, `dev`. |
| **6. CI/CD Automation (GitHub Actions)**| v1.0.0 | ✅ Completed | `ci.yml` (tests, lint, build, Docker validation) and `cd.yml` (GHCR package release & semantic tags). |
| **7. Architecture & Developer Docs** | v1.0.0 | ✅ Completed | Full documentation suite (`ARCHITECTURE.md`, `CI_CD_PIPELINE.md`, `DEVELOPER_GUIDE.md`, `PROJECT_LOG.md`). |
| **8. Observability & Swagger Docs** | v1.0.1 | ✅ Completed | SpringDoc OpenAPI 3.1.1 (`/swagger-ui.html`), Actuator (`/actuator/**`), YAML reserved paths & CORS. |
| **9. Node 22 LTS Security Hardening**| v1.0.2 | ✅ Completed | Node.js 22 LTS Docker image, CI/CD runner update, ESLint config, and full build validation. |
| **10. Sqids Identifier Encoding** | v1.0.3 | ✅ Completed | Replaced random tokens with reversible ID-based Sqids and unified aliases in the public-code namespace. |
| **11. Local PostgreSQL Docker Hardening** | v1.0.4 | ✅ Completed | Pinned PostgreSQL 15.19/Alpine 3.24, separated the local `postgres-dev` profile, and repaired backend container health checks. |
| **12. Release Readiness & Promotion Policy** | v1.0.5 | ✅ Completed | Repaired ESLint configuration, enforced frontend lint in CI, added least-privilege CI permissions, and restricted promotions to `dev` → `release` → `main`. |

---

## 🛠️ Detailed Engineering Log

### Phase 1: Backend Implementation (`backend/`)
* **Technology**: Java 21, Spring Boot 4.1.2, Maven.
* **Core Components**:
  * `Url` JPA Entity: stores `id`, `originalUrl`, `shortCode`, `customAlias`, `title`, `clickCount`, `createdAt`, `updatedAt`, `expiresAt`, `isActive`.
  * `UrlRepository`: Spring Data JPA repository with indexed queries for `shortCode` lookups and exists checks.
  * `UrlService`:
    * Generates Sqids from persisted numeric URL IDs with a minimum length of 7.
    * Supports custom alias generation and validation against reserved paths.
    * Atomic incrementing for click counts upon redirection.
  * `UrlController`:
    * `POST /api/urls/shorten` — Shortens URL with optional custom alias.
    * `GET /api/urls` — Returns all active shortlinks sorted by creation date.
    * `GET /api/urls/{id}` — URL metadata lookup.
    * `DELETE /api/urls/{id}` — Deletes a shortcode.
    * `GET /api/urls/{id}/stats` — Click analytics endpoint.
    * `GET /{shortCode}` — HTTP 302 redirect with click tracking.
  * Environment-specific database config:
    * `application.yml` / `default`: In-memory H2 database with Web Console (`/api/h2-console/`) for rapid local dev.
    * `application-postgres-dev.yml`: Local Docker PostgreSQL with Hibernate schema updates.
    * `application-prod.yml`: PostgreSQL configuration with connection pooling for production deployments.

### Phase 2: Frontend Implementation (`frontend/`)
* **Technology**: React 18, Vite 5, TypeScript, Vanilla CSS (custom design system), Axios, Lucide React icons.
* **Key Features**:
  * Glassmorphic modern aesthetic with dark mode gradient styling.
  * Instant URL input with auto-protocol prefixing (`http://` / `https://`).
  * Custom alias drawer/collapsible options.
  * 1-click clipboard copy with animated feedback.
  * Real-time link history and click statistics counter.
  * QR Code generator integration for every shortened URL.
  * Responsive layout tested on desktop, tablet, and mobile viewports.

### Phase 3: Containerization & Local Orchestration
* **Multi-stage Dockerfile for Backend**:
  * Stage 1 (`maven:3.9.5-eclipse-temurin-21`): builds production JAR.
  * Stage 2 (`eclipse-temurin:21-jre-alpine`): lightweight non-root runtime container (~150MB).
* **Multi-stage Dockerfile for Frontend**:
  * Stage 1 (`node:22-alpine`): installs dependencies and builds Vite static output.
  * Stage 2 (`nginx:alpine`): high-performance web server with custom `nginx.conf` routing reverse-proxy requests to the backend.
* **`docker-compose.yml`**:
  * Coordinates `postgres:15.19-alpine3.24`, `backend`, and `frontend` on a shared Docker bridge network with health checks.

### Phase 4: Git Repository & Branching Strategy
* **Remote Repository**: `https://github.com/avin15cs/URL-Shortener`
* **Branch Setup**:
  * `main`: Protected production branch.
  * `release`: Release candidate staging branch.
  * `dev`: Primary integration development branch.

### Phase 5: CI/CD Pipeline Setup
* **`.github/workflows/ci.yml`**:
  * Automatically tests every PR and commit pushed to `main`, `dev`, or `release`.
  * Checks Node 22 / TypeScript / Vite build.
  * Checks Java 21 Temurin / Maven unit tests & JAR packaging.
  * Tests Docker multi-stage builds.
* **`.github/workflows/cd.yml`**:
  * Automatically builds and publishes Docker containers to **GitHub Container Registry (GHCR)**:
    * `ghcr.io/avin15cs/url-shortener-backend`
    * `ghcr.io/avin15cs/url-shortener-frontend`
  * Publishes automated semantic GitHub releases on `release` branch updates.

### Phase 6: Observability, OpenAPI/Swagger & Path Safety
* **Spring Boot Actuator**:
  * Health, info, metrics, env, and beans exposed at `/actuator/**` in dev.
  * Restricted to `/health` only in `application-prod.yml`.
* **OpenAPI 3 / Swagger UI**:
  * Added `springdoc-openapi-starter-webmvc-ui` (v3.1.1 for Spring Boot 4 compatibility).
  * Interactive UI available at `/swagger-ui.html` and JSON spec at `/v3/api-docs`.
  * Disabled automatically in production profile (`springdoc.swagger-ui.enabled: false`).
* **YAML-Configured Reserved Paths & Configurable CORS**:
  * Added [`AppProperties`](../backend/src/main/java/com/urlshortener/config/AppProperties.java) (`@ConfigurationProperties(prefix = "app")`).
  * `app.reserved-paths` configured in `application.yml`: prevents single-segment routes (`actuator`, `swagger-ui`, `v3`, `favicon.ico`, `api`, `error`, `webjars`) from colliding with `/{shortCode}`.
  * `UrlService` validates custom aliases against reserved paths to avoid namespace hijacking.
  * `app.cors.allowed-origins` configured in `application.yml` and overridable via `${CORS_ALLOWED_ORIGIN}` in production.

### Phase 7: Node.js 22 LTS Security Hardening
* Upgraded frontend Docker build container from `node:20-alpine` to `node:22-alpine` (Active LTS).
* Upgraded GitHub Actions CI/CD workflow (`ci.yml`) frontend runner to Node `22`.
* Added `"engines": { "node": ">=22.0.0" }` to `frontend/package.json` to enforce modern runtime and resolve legacy CVEs.

### Phase 8: Spring Boot 4 H2 Console Modularization Fix
* **Added `org.springframework.boot:spring-boot-h2console`**: Spring Boot 4 modularized `H2ConsoleAutoConfiguration` out of core autoconfigure into this dedicated starter module.
* **Auto-Redirect View Controllers**: Added redirects in `WebConfig` and `RedirectController` so requests to `/h2-console`, `/h2-console/`, and `/api/h2-console` seamlessly normalize to `/api/h2-console/`.
* **Exception Handling**: Added explicit handlers in `GlobalExceptionHandler` for `NoResourceFoundException` and `ResponseStatusException` returning accurate 404 responses.

### Phase 9: Sqids-Based Public Identifiers
* Added the official `org.sqids:sqids` Java implementation and configured it as a thread-safe Spring bean.
* Automatic codes now encode the persisted URL ID instead of using pseudorandom characters.
* Extended the standard Sqids blocklist with application-reserved routes and retained collision retries for custom/legacy aliases.
* Custom aliases are now stored as the canonical `short_code`, giving redirects one primary public-code namespace.
* Blank aliases are normalized to `null`, preventing malformed root short URLs.
* Kept schema generation development-only: H2 uses `create-drop` and Docker PostgreSQL uses the dedicated `postgres-dev` profile with `update`.
* Reserved Liquibase as the future schema owner before the first persistent or shared deployment; the `prod` profile remains validation-only.

### Phase 10: Local PostgreSQL Docker Hardening
* Pinned the Docker database image to `postgres:15.19-alpine3.24` for reproducible local environments.
* Kept local Compose on `postgres-dev`, allowing Hibernate to initialize the disposable local schema while production remains validation-only.
* Removed the production-profile override from the backend image so Compose and deployment environments control the active Spring profile.
* Installed `curl` in the backend runtime image so its Compose health check can execute successfully.
* Documented PostgreSQL-only startup, `psql`, DataGrip, and IntelliJ connection settings.

### Phase 11: Release Readiness & Promotion Policy
* Repaired the ESLint configuration and made linting a dedicated CI step before the frontend build.
* Set CI workflow permissions to read-only and added concurrency controls to avoid redundant CI runs.
* Added a CI promotion-policy check: only `dev` may target `release`, and only `release` may target `main`.
* Added serialized CD concurrency per branch to prevent overlapping image publication or release creation.
* Defined branch-protection rules for `dev`, `release`, and `main`; applying them to this private repository requires GitHub Pro or changing the repository visibility, so they remain an account-level follow-up.
