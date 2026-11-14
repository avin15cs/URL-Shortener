# 🤖 Agent Context & Project Instructions (URL-Shortener)

> **Note for AI Assistants**: This repository is a production-grade full-stack URL shortener monorepo. Read this file and the documents in `docs/` before making changes to understand existing patterns and conventions.

---

## 1. Monorepo Overview & Tech Stack

* **Repository**: `https://github.com/avin15cs/URL-Shortener`
* **Architecture**: Monorepo with two independent application directories:
  * **`backend/`**: Spring Boot 4.1.2 REST API (Java 21, Maven, Spring Data JPA).
    * Database: PostgreSQL 15 with Liquibase for normal local and production-style runs; H2 remains available for isolated default-profile tests and direct backend experiments.
    * Core Entity: `Url` (id, originalUrl, shortCode, customAlias, title, clickCount, timestamps, isActive).
    * Logic: Sqids-based identifiers with a 7-character minimum, custom alias support with reserved path validation, collision handling, atomic redirect click counter.
    * Tooling: SpringDoc OpenAPI 3.1.1 (`/swagger-ui.html`, `/v3/api-docs`), Actuator (`/actuator/**`), `AppProperties` for YAML-configured reserved paths and CORS.
  * **`frontend/`**: Single Page Application (React 18, TypeScript, Vite, Node 22 LTS, Vanilla CSS).
    * Glassmorphic modern dark-mode aesthetic with Lucide React icons and Toast alerts.
    * Features: URL shortening, custom alias toggle, 1-click clipboard copy, QR code display, analytics list.
  * **`docker-compose.yml`**: Orchestrates the exactly pinned `postgres:15.19-alpine3.24`, `backend`, and `frontend` (Nginx).

---

## 2. Git Branching Strategy

Always respect the 2-branch convention:
* **`dev`**: **Default active branch for development**. All feature work, fixes, and PRs must target `dev`.
* **`main`**: Production branch. Only promotion PRs from `dev` may target `main`; merging publishes containers and creates a GitHub Release.

---

## 3. CI/CD Workflows (`.github/workflows/`)

* **`ci.yml`**: Runs on push/PR to `main` and `dev`.
  * Runs frontend linting & Vite build.
  * Runs backend tests (`mvn test`) and JAR packaging.
  * Verifies multi-stage Docker builds.
* **`cd.yml`**: Runs on push to `main`.
  * Builds and publishes Docker images to **GitHub Container Registry**:
    * `ghcr.io/avin15cs/url-shortener-backend`
    * `ghcr.io/avin15cs/url-shortener-frontend`
  * Automatically creates GitHub Releases with changelogs from the merged `main` commit.

---

## 4. Documentation Standards (Definition of Done)

Documentation is part of every implementation, regardless of whether work starts in a
new chat or an existing one. Before declaring any code, configuration, tooling,
workflow, or developer-experience change complete, every agent must review the
documentation impact and update the repository documentation in the same change.

1. Always update `docs/PROJECT_LOG.md` with the completed change.
2. Update `docs/ARCHITECTURE.md` when data models, endpoints, algorithms, components,
   or runtime interactions change.
3. Update `docs/DEVELOPER_GUIDE.md` when commands, setup steps, prerequisites,
   environment variables, or local workflows change.
4. Update user-facing entry points such as `README.md` and `CONTRIBUTING.md` when their
   instructions or examples are affected.
5. Use `docs/features/README.md` as the feature-documentation index. Each full-fledged
   end-to-end feature must have exactly one canonical document under `docs/features/`.
   Update that document whenever the feature changes. Do not create separate documents
   for its individual tickets, endpoints, components, fixes, or incremental changes.
   Create a new document and index entry only for a genuinely distinct end-to-end
   feature or substantial maintained developer workflow.
6. Review any other relevant document in `docs/` and keep it synchronized. If no
   additional document needs a change, state that explicitly in the final handoff.

Implementation and its required documentation updates are one unit of work; do not
defer documentation to a later chat or follow-up task.

---

## 5. Quick Commands Reference

* **Install Local Dependencies**: `npm ci` (run once from the repository root)
* **Run Backend + Frontend Locally**: `npm run dev` (Ports 8080 and 5173)
* **Run Backend Locally**: `npm run dev:backend` (Port 8080)
* **Run Frontend Locally**: `npm run dev:frontend` (Port 5173)
* **Run Full Docker Stack**: `npm run compose:up` (Port 80, 8080, 5432)
* **Run Tests**:
  * Backend: `cd backend && mvn test`
  * Frontend: `cd frontend && npm run lint && npm run build`
