# 🔗 URL Shortener — Full-Stack Monorepo

[![CI Pipeline](https://github.com/avin15cs/URL-Shortener/actions/workflows/ci.yml/badge.svg)](https://github.com/avin15cs/URL-Shortener/actions/workflows/ci.yml)
[![CD Pipeline](https://github.com/avin15cs/URL-Shortener/actions/workflows/cd.yml/badge.svg)](https://github.com/avin15cs/URL-Shortener/actions/workflows/cd.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue.svg)](https://reactjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.2-blue.svg)](https://www.typescriptlang.org/)

A production-ready full-stack URL Shortener application built as a clean monorepo featuring a **Spring Boot 3** REST API backend and a **React 18 + Vite** frontend.

---

## 📚 Complete Documentation

Detailed technical documentation is available in the [`docs/`](docs/) directory:

- 🏛️ [**System Architecture & Technical Specs**](docs/ARCHITECTURE.md) — System design, database schema, algorithm, and API design.
- 🔄 [**CI/CD Pipeline & Branching Guide**](docs/CI_CD_PIPELINE.md) — GitHub Actions workflows, GHCR Docker container releases, and branch rules.
- 💻 [**Developer & Contribution Guide**](docs/DEVELOPER_GUIDE.md) — Step-by-step local dev setup, H2 console, Docker compose, and testing.
- 📜 [**Project History & Engineering Log**](docs/PROJECT_LOG.md) — Audit log of everything created, configured, and implemented.

---

## 📁 Repository Structure

```
URL-Shortener/
├── .github/
│   └── workflows/
│       ├── ci.yml              # CI: Tests, builds, and Docker validation
│       └── cd.yml              # CD: Builds and publishes to GHCR & releases
├── backend/                    # Spring Boot 3 REST API (Java 17, Maven)
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
│   └── PROJECT_LOG.md
├── docker-compose.yml          # Multi-container orchestration (DB + API + UI)
├── .gitattributes
├── .gitignore
└── README.md
```

---

## ⚡ Quick Start

### 1. Run Everything with Docker Compose (Recommended)

```bash
docker compose up --build
```

* **Frontend**: [http://localhost:80](http://localhost:80)
* **Backend API**: [http://localhost:8080](http://localhost:8080)
* **PostgreSQL Database**: `localhost:5432`

---

### 2. Run Locally for Development

#### Backend:
```bash
cd backend
mvn spring-boot:run
```
* Runs with in-memory H2 database at [http://localhost:8080](http://localhost:8080).
* H2 Console available at [http://localhost:8080/h2-console](http://localhost:8080/h2-console).

#### Frontend:
```bash
cd frontend
npm install
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
| `DELETE`| `/api/urls/{id}` | Delete a shortened URL | `204 No Content`|
| `GET` | `/api/urls/{id}/stats` | Get click count and statistics | `200 OK` |
| `GET` | `/{shortCode}` | **Redirect to original URL (atomic click count)** | `302 Found` |

---

## 🌿 Git Branches

* **`main`**: Production-ready code.
* **`release`**: Release candidates, version tags, and package publishing.
* **`dev`**: Active integration development branch.
