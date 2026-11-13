# Local Development Workflow

> Canonical feature document. Update this file for all later changes to the native
> development workflow; do not create separate documents for its individual scripts,
> fixes, or enhancements.

## Purpose

The repository contains an independently built Spring Boot backend and Vite frontend.
Previously, native development required two terminals and separate commands. The root
npm orchestrator provides one reproducible command that starts both services and labels
their combined output.

## Setup and Usage

Install the locked root tooling and frontend dependencies from the repository root:

```bash
npm ci
```

The root `postinstall` script runs `npm ci --prefix frontend`, preserving the
frontend's independent lockfile for its Docker build and CI job.

Start both services:

```bash
npm run dev
```

| Service | Address | Development behavior |
| :--- | :--- | :--- |
| Frontend | `http://localhost:5173` | Vite hot reload and `/api` proxying |
| Backend | `http://localhost:8080` | Spring Boot DevTools with the in-memory H2 profile |

The command prefixes output with `frontend` or `backend`. If either process fails,
`concurrently` stops the companion process. In an interactive terminal, press `Ctrl+C`
once to stop the development stack.

Individual services remain available from the repository root:

```bash
npm run dev:backend
npm run dev:frontend
```

## Native Development Versus Docker Compose

`npm run dev` is intended for fast native development with H2 and hot reload.
`docker compose up --build` remains the production-like workflow with PostgreSQL and
Nginx.

Do not run both workflows simultaneously: their backend services both bind host port
`8080`. Stop the Compose stack before native development:

```bash
docker compose down
```

## Windows PowerShell

If PowerShell execution policy blocks the `npm.ps1` shim, use the executable command
shim without changing project or system configuration:

```powershell
npm.cmd ci
npm.cmd run dev
```

## Dependency and Lockfile Design

- The root `package.json` is private and contains only repository-level development
  orchestration.
- The root `package-lock.json` locks `concurrently` and its transitive dependencies.
- The existing frontend package and lockfile remain independent.
- npm workspaces are intentionally not introduced because frontend CI and Docker builds
  use `frontend/package-lock.json` directly.

## Validation

The implementation was validated with:

- A clean root `npm ci`, including the frontend postinstall; both audits reported zero
  known vulnerabilities.
- Frontend linting and a production Vite build.
- All nine backend Maven tests.
- Combined startup with HTTP 200 from Vite and `UP` from Spring Boot Actuator.

## Ownership Boundaries

Update this document for changes to root npm scripts, native frontend/backend startup,
local ports, local dependency installation, H2-based combined development, or related
troubleshooting. Container topology belongs in `CONTAINERIZED_FULL_STACK.md`.

## Primary Implementation Files

- `package.json`
- `package-lock.json`
- `frontend/package.json`
- `frontend/vite.config.ts`
- `backend/pom.xml`
- `backend/src/main/resources/application.yml`
