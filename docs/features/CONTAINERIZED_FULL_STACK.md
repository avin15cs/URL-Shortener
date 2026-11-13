# Containerized Full Stack

> Canonical feature document for the Docker Compose PostgreSQL, Spring Boot, and Nginx
> stack and the production-style routing between those services.

## Purpose and Topology

Docker Compose provides a production-like local stack with three services:

```text
Browser :80
    |
    v
Frontend Nginx ---- /api and /{shortCode} ----> Backend :8080
                                                   |
                                                   v
                                            PostgreSQL :5432
```

Start or rebuild the stack from the repository root:

```bash
docker compose up --build
```

Stop containers while retaining the database volume with `docker compose down`. Use
`docker compose down -v` only when the local PostgreSQL data should be deleted.

## PostgreSQL Service

- Built from `postgres/Dockerfile` and pinned to PostgreSQL 15.19 on Alpine 3.24.
- Publishes port 5432 only on host loopback (`127.0.0.1`).
- Stores data in the named `postgres_data` volume.
- Uses a `pg_isready` health check before the backend may start.
- Uses local-only development credentials from `docker-compose.yml`; these are not
  production secrets.

The image applies current Alpine packages and rebuilds the privilege-dropping `gosu`
helper from its pinned source commit for reproducibility.

## Backend Service

The multi-stage image builds the JAR with Maven and runs it on a pinned Java 21 JRE as
the non-root `spring` user. Compose activates the `postgres-dev` profile and supplies
database connection variables plus the public base URL.

The service waits for PostgreSQL health, publishes port 8080, and reports its own health
through `/actuator/health`. The development PostgreSQL profile permits Hibernate
`ddl-auto: update`; the production profile remains `validate` and requires a migration
owner before persistent deployment.

## Frontend and Edge Routing

The frontend image builds the Vite SPA with pinned Node 22 and serves static assets from
a pinned Nginx Alpine runtime on host port 80.

Nginx:

- Proxies `/api/` to the backend API.
- Proxies single-segment alphanumeric, underscore, and hyphen paths to backend redirect
  resolution.
- Falls back to `index.html` for SPA routes.
- Enables gzip and long-lived immutable caching for static assets.

## Environment Contract

| Variable | Compose value/purpose |
| :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | `postgres-dev` |
| `DB_HOST` | Compose service name `postgres` |
| `DB_PORT` | `5432` |
| `DB_NAME` | `urlshortener` |
| `DB_USER` | Local PostgreSQL user |
| `DB_PASSWORD` | Local-only development password |
| `APP_BASE_URL` | Public backend URL used in generated short links |

## Ownership Boundaries

Update this document for Dockerfiles, Compose topology, PostgreSQL container behavior,
container health/dependencies, Nginx production routing, image hardening, volumes, or
container-facing environment variables. Native H2/Vite startup belongs in
`LOCAL_DEVELOPMENT_WORKFLOW.md`.

## Primary Implementation Files

- `docker-compose.yml`
- `postgres/Dockerfile`
- `backend/Dockerfile`
- `frontend/Dockerfile`
- `frontend/nginx.conf`
- `backend/src/main/resources/application-postgres-dev.yml`
- `backend/src/main/resources/application-prod.yml`
