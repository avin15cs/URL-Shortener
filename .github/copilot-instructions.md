# GitHub Copilot & Codex Workspace Instructions

This repository is a full-stack URL Shortener monorepo. When assisting in this repository, follow these project rules:

## Architecture & Stack
- **Frontend** (`frontend/`): React 18, Vite, TypeScript, Node 22 LTS, Vanilla CSS design system.
- **Backend** (`backend/`): Spring Boot 4.1.2 (Java 21, Maven, Spring Data JPA, SpringDoc OpenAPI 3.1.1, Actuator).
- **Database**: H2 in-memory for local development (`application.yml` at `/api/h2-console/`), with PostgreSQL 15.19 pinned for Docker development and PostgreSQL 15 for production (`application-prod.yml`).
- **Orchestration**: `docker-compose.yml` (PostgreSQL + Backend + Frontend Nginx).

## Git Branching Rules
- **Active development branch is `dev`**. Never commit feature work directly to `main`.
- Promote releases with a pull request from `dev` to `main`.
- `main` is production-ready code; merges publish containers and create a GitHub Release.

## Documentation Requirements
- Documentation is part of the definition of done for every implementation, in new and existing chats alike.
- Always update `docs/PROJECT_LOG.md` when code, configuration, tooling, workflows, or developer experience changes.
- Update `docs/ARCHITECTURE.md` for data model, endpoint, algorithm, component, or runtime-interaction changes.
- Update `docs/DEVELOPER_GUIDE.md` for command, prerequisite, setup, environment-variable, or local-workflow changes.
- Keep affected entry points such as `README.md` and `CONTRIBUTING.md` synchronized in the same change.
- Consult `docs/features/README.md` before documenting implementation work.
- Maintain exactly one canonical `docs/features/` document per full-fledged end-to-end feature; update it for later tickets, fixes, endpoints, components, and incremental changes belonging to that feature.
- Create and index a new feature document only for a genuinely distinct end-to-end capability or substantial maintained developer workflow.
- Do not defer required documentation to a later chat or follow-up task.
