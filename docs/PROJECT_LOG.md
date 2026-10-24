# 📜 Project Activity & Engineering Log

This document tracks every major engineering milestone, architectural decision, and implementation step performed in the **URL-Shortener** monorepo.

---

## 📅 Timeline & Milestone Summary

| Milestone | Date / Version | Status | Key Deliverables |
| :--- | :--- | :--- | :--- |
| **1. Monorepo Architecture Setup** | v1.0.0-alpha | ✅ Completed | Monorepo structure created with `frontend/` and `backend/`. |
| **2. Spring Boot 3 Backend** | v1.0.0-alpha | ✅ Completed | Java 17, Spring Web, Spring Data JPA, H2 + PostgreSQL support, URL hashing, redirects, atomic clicks. |
| **3. React + Vite Frontend** | v1.0.0-alpha | ✅ Completed | Modern glassmorphic SPA, TypeScript, Lucide icons, Toast alerts, instant shortcode copy, stats dashboard. |
| **4. Docker Containerization** | v1.0.0-beta | ✅ Completed | Multi-stage Dockerfiles for backend & frontend, `docker-compose.yml` with PostgreSQL 15. |
| **5. Remote Git & Branching Strategy** | v1.0.0-rc | ✅ Completed | Repository linked to `avin15cs/URL-Shortener`, branches initialized: `main`, `release`, `dev`. |
| **6. CI/CD Automation (GitHub Actions)**| v1.0.0 | ✅ Completed | `ci.yml` (tests, lint, build, Docker validation) and `cd.yml` (GHCR package release & semantic tags). |
| **7. Architecture & Developer Docs** | v1.0.0 | ✅ Completed | Full documentation suite (`ARCHITECTURE.md`, `CI_CD_PIPELINE.md`, `DEVELOPER_GUIDE.md`, `PROJECT_LOG.md`). |

---

## 🛠️ Detailed Engineering Log

### Phase 1: Backend Implementation (`backend/`)
* **Technology**: Java 17, Spring Boot 3.2.0, Maven.
* **Core Components**:
  * `UrlMapping` JPA Entity: stores `id`, `originalUrl`, `shortCode`, `clickCount`, `createdAt`, `updatedAt`, `expiresAt`.
  * `UrlMappingRepository`: Spring Data JPA repository with indexed queries for `shortCode` lookups and exists checks.
  * `UrlShortenerService`:
    * Generates 7-character alphanumeric base62 shortcodes with collision detection.
    * Supports custom alias generation and validation.
    * Atomic incrementing for click counts upon redirection.
  * `UrlMappingController`:
    * `POST /api/urls/shorten` — Shortens URL with optional custom alias.
    * `GET /api/urls` — Returns all active shortlinks sorted by creation date.
    * `GET /api/urls/{id}` — URL metadata lookup.
    * `DELETE /api/urls/{id}` — Deletes a shortcode.
    * `GET /api/urls/{id}/stats` — Click analytics endpoint.
    * `GET /{shortCode}` — HTTP 302 redirect with click tracking.
  * Dual-profile database config:
    * `application.yml` / `default`: In-memory H2 database with Web Console (`/h2-console`) for rapid local dev.
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
  * Stage 1 (`maven:3.9.5-eclipse-temurin-17`): builds production JAR.
  * Stage 2 (`eclipse-temurin:17-jre-alpine`): lightweight non-root runtime container (~150MB).
* **Multi-stage Dockerfile for Frontend**:
  * Stage 1 (`node:20-alpine`): installs dependencies and builds Vite static output.
  * Stage 2 (`nginx:alpine`): high-performance web server with custom `nginx.conf` routing reverse-proxy requests to the backend.
* **`docker-compose.yml`**:
  * Coordinates `postgres:15-alpine`, `backend`, and `frontend` on a shared Docker bridge network with health checks.

### Phase 4: Git Repository & Branching Strategy
* **Remote Repository**: `https://github.com/avin15cs/URL-Shortener`
* **Branch Setup**:
  * `main`: Protected production branch.
  * `release`: Release candidate staging branch.
  * `dev`: Primary integration development branch.

### Phase 5: CI/CD Pipeline Setup
* **`.github/workflows/ci.yml`**:
  * Automatically tests every PR and commit pushed to `main`, `dev`, or `release`.
  * Checks Node 20 / TypeScript / Vite build.
  * Checks Java 17 Temurin / Maven unit tests & JAR packaging.
  * Tests Docker multi-stage builds.
* **`.github/workflows/cd.yml`**:
  * Automatically builds and publishes Docker containers to **GitHub Container Registry (GHCR)**:
    * `ghcr.io/avin15cs/url-shortener-backend`
    * `ghcr.io/avin15cs/url-shortener-frontend`
  * Publishes automated semantic GitHub releases on `release` branch updates.
