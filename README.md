# 🔗 URL Shortener — Full-Stack Monorepo

[![CI Pipeline](https://github.com/avin15cs/URL-Shortener/actions/workflows/ci.yml/badge.svg)](https://github.com/avin15cs/URL-Shortener/actions/workflows/ci.yml)
[![CD Pipeline](https://github.com/avin15cs/URL-Shortener/actions/workflows/cd.yml/badge.svg)](https://github.com/avin15cs/URL-Shortener/actions/workflows/cd.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Node](https://img.shields.io/badge/Node.js-22%20LTS-green.svg)](https://nodejs.org/)
[![React](https://img.shields.io/badge/React-18-blue.svg)](https://reactjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.2-blue.svg)](https://www.typescriptlang.org/)
[![OpenAPI](https://img.shields.io/badge/OpenAPI-3.1-green.svg)](http://localhost:8080/swagger-ui.html)

A production-ready full-stack URL Shortener application built as a clean monorepo featuring a **Spring Boot 4** REST API backend and a **React 18 + Vite** frontend.

---

## 📚 Complete Documentation

Detailed technical documentation is available in the [`docs/`](docs/) directory:

- 🏛️ [**System Architecture & Technical Specs**](docs/ARCHITECTURE.md) — System design, database schema, algorithm, and API design.
- 🔄 [**CI/CD Pipeline & Branching Guide**](docs/CI_CD_PIPELINE.md) — GitHub Actions workflows, GHCR Docker container releases, and branch rules.
- 💻 [**Developer & Contribution Guide**](docs/DEVELOPER_GUIDE.md) — Step-by-step local dev setup, H2 console, Docker compose, and testing.
- 📜 [**Project History & Engineering Log**](docs/PROJECT_LOG.md) — Audit log of everything created, configured, and implemented.
- 🧩 [**Feature Documentation**](docs/features/) — Implementation details, usage, validation, and troubleshooting for developed solutions.

---

## 📁 Repository Structure

```
URL-Shortener/
├── .github/
│   └── workflows/
│       ├── ci.yml              # CI: Tests, builds, and Docker validation
│       └── cd.yml              # CD: Builds and publishes to GHCR & releases
├── backend/                    # Spring Boot 4 REST API (Java 21, Maven)
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
├── frontend/                   # React 18 + TypeScript + Vite SPA
│   ├── src/
│   ├── package.json
│   ├── vite.config.ts
│   ├── nginx.conf
│   └── Dockerfile
├── docs/                       # Project documentation suite
│   ├── ARCHITECTURE.md
│   ├── CI_CD_PIPELINE.md
│   ├── DEVELOPER_GUIDE.md
│   ├── PROJECT_LOG.md
│   └── features/               # Per-solution implementation documentation
├── package.json                # Root local-development command orchestration
├── package-lock.json           # Locked root development-tool dependencies
├── docker-compose.yml          # Multi-container orchestration (DB + API + UI)
├── .gitattributes
├── .gitignore
└── README.md
```

---

## ⚡ Quick Start

### 1. Run Locally for Development (Recommended)

Install the locked root tooling and frontend dependencies once:

```bash
npm ci
```

Then start Spring Boot and Vite together from the repository root:

```bash
npm run dev
```

* **Frontend with hot reload**: [http://localhost:5173](http://localhost:5173)
* **Backend API**: [http://localhost:8080](http://localhost:8080)
* **Database**: Persistent `urlshortener` database in the shared local `postgres-db` container
* Press `Ctrl+C` once to stop both processes.
* Stop any running Docker Compose stack first because both workflows use port `8080`.

On Windows PowerShell systems that block `npm.ps1`, use `npm.cmd ci` and
`npm.cmd run dev`, or adjust the PowerShell execution policy for your user account.

---

### 2. Run Everything with Docker Compose (Production-Like)

```bash
npm run compose:up
```

* **Frontend**: [http://localhost:80](http://localhost:80)
* **Backend API**: [http://localhost:8080](http://localhost:8080)
* **PostgreSQL 15.19 Database**: `localhost:5432`

Auth Server uses the same local `postgres-db` container with a separate `authserver`
database. Starting either repository first creates the shared container and only that
service's database. Liquibase applies pending URL Shortener schema changes on startup.

---

### 3. Run Services Individually for Development

#### Backend:
```bash
npm run dev:backend
```
* Ensures `postgres-db`, activates `postgres-dev`, and runs at [http://localhost:8080](http://localhost:8080).
* **Swagger UI Documentation**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
* **OpenAPI 3.1 Spec**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
* **Actuator Health Check**: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)

For an isolated disposable H2 run, execute `mvn spring-boot:run` directly from `backend/`
without the `postgres-dev` profile.

#### Frontend:
```bash
cd frontend
npm ci
npm run dev
```
* Vite dev server live at [http://localhost:5173](http://localhost:5173).

---

## 🌐 API Reference

| Method | Endpoint | Description | Status Code |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/urls/shorten` | Create shortened URL (auto or custom alias) | `201 Created` |
| `GET` | `/api/urls` | Retrieve all shortened URLs | `200 OK` |
| `GET` | `/api/urls/{id}` | Get URL details and metadata | `200 OK` |
| `DELETE`| `/api/urls/{id}` | Soft-delete a shortened URL | `200 OK`|
| `GET` | `/api/urls/{id}/stats` | Get click count and statistics | `200 OK` |
| `GET` | `/{shortCode}` | **Redirect to original URL (atomic click count)** | `302 Found` |
| `GET` | `/swagger-ui.html` | Interactive Swagger UI API documentation | `200 OK` |
| `GET` | `/v3/api-docs` | OpenAPI 3.1 JSON specification | `200 OK` |
| `GET` | `/actuator/health` | Service health status check | `200 OK` |

---

## 🌿 Git Branches

* **`main`**: Production-ready code.
* **`release`**: Release candidates, version tags, and package publishing.
* **`dev`**: Active integration development branch.
