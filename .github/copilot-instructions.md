# GitHub Copilot & Codex Workspace Instructions

This repository is a full-stack URL Shortener monorepo. When assisting in this repository, follow these project rules:

## Architecture & Stack
- **Frontend** (`frontend/`): React 18, Vite, TypeScript, Vanilla CSS design system.
- **Backend** (`backend/`): Spring Boot 3.2 (Java 17, Maven, Spring Data JPA).
- **Database**: H2 in-memory for local development (`application.yml`), PostgreSQL 15 for production (`application-prod.yml`).
- **Orchestration**: `docker-compose.yml` (PostgreSQL + Backend + Frontend Nginx).

## Git Branching Rules
- **Active development branch is `dev`**. Never commit feature work directly to `main` or `release`.
- `release` is reserved for release candidates and automated tagging.
- `main` is production-ready code.

## Documentation Requirements
- Whenever adding features or altering endpoints, update `docs/PROJECT_LOG.md` and `docs/ARCHITECTURE.md`.
- Reference `docs/DEVELOPER_GUIDE.md` for local environment instructions.
