# 🤖 Agent Context & Project Instructions (URL-Shortener)

> **Note for AI Assistants**: This repository is a production-grade full-stack URL shortener monorepo. Read this file and the documents in `docs/` before making changes to understand existing patterns and conventions.

---

## 1. Monorepo Overview & Tech Stack

* **Repository**: `https://github.com/avin15cs/URL-Shortener`
* **Architecture**: Monorepo with two independent application directories:
  * **`backend/`**: Spring Boot 4.1.2 REST API (Java 21, Maven, Spring Data JPA).
    * Database: H2 in-memory (local dev on port 8080 at `/api/h2-console/`) and PostgreSQL 15 (prod profile).
    * Core Entity: `Url` (id, originalUrl, shortCode, customAlias, title, clickCount, timestamps, isActive).
    * Logic: Sqids-based identifiers with a 7-character minimum, custom alias support with reserved path validation, collision handling, atomic redirect click counter.
    * Tooling: SpringDoc OpenAPI 3.1.1 (`/swagger-ui.html`, `/v3/api-docs`), Actuator (`/actuator/**`), `AppProperties` for YAML-configured reserved paths and CORS.
  * **`frontend/`**: Single Page Application (React 18, TypeScript, Vite, Node 22 LTS, Vanilla CSS).
    * Glassmorphic modern dark-mode aesthetic with Lucide React icons and Toast alerts.
    * Features: URL shortening, custom alias toggle, 1-click clipboard copy, QR code display, analytics list.
  * **`docker-compose.yml`**: Orchestrates the exactly pinned `postgres:15.19-alpine3.24`, `backend`, and `frontend` (Nginx).

---

## 2. Git Branching Strategy

Always respect the 3-branch convention:
* **`dev`**: **Default active branch for development**. All feature work, fixes, and PRs must target `dev`.
* **`release`**: Release candidate staging branch. Merging into `release` triggers semantic version tagging and releases.
* **`main`**: Production branch. Contains stable, deployable code.

---

## 3. CI/CD Workflows (`.github/workflows/`)

* **`ci.yml`**: Runs on push/PR to `main`, `dev`, `release`.
  * Runs frontend linting & Vite build.
  * Runs backend tests (`mvn test`) and JAR packaging.
  * Verifies multi-stage Docker builds.
* **`cd.yml`**: Runs on push to `main` and `release`.
  * Builds and publishes Docker images to **GitHub Container Registry**:
    * `ghcr.io/avin15cs/url-shortener-backend`
    * `ghcr.io/avin15cs/url-shortener-frontend`
  * Automatically creates GitHub Releases with changelogs on `release` branch updates.

---

## 4. Documentation Standards

Whenever you make architectural changes, add new features, or alter configurations:
1. Update `docs/PROJECT_LOG.md` to log the new milestone/changes.
2. Update `docs/ARCHITECTURE.md` if data models, endpoints, or algorithms change.
3. Update `docs/DEVELOPER_GUIDE.md` if setup steps or environment variables change.

---

## 5. Quick Commands Reference

* **Run Backend Locally**: `cd backend && mvn spring-boot:run` (Port 8080)
* **Run Frontend Locally**: `cd frontend && npm install && npm run dev` (Port 5173)
* **Run Full Docker Stack**: `docker compose up --build` (Port 80, 8080, 5432)
* **Run Tests**:
  * Backend: `cd backend && mvn test`
  * Frontend: `cd frontend && npm run lint && npm run build`
