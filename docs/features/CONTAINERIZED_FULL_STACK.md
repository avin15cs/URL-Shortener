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
npm run compose:up
```

Stop only the URL Shortener application containers with
`docker compose stop backend frontend`. The database is shared with Auth Server and has an
independent lifecycle.

## PostgreSQL Service

- Uses the pinned official PostgreSQL 15.19/Alpine 3.24 image.
- Uses the stable container name `postgres-db`, shared Compose project, and Docker network
  declared identically in Auth Server and URL Shortener.
- Publishes port 5432 only on host loopback (`127.0.0.1`).
- Stores both isolated local databases in the named `url-shortener_postgres_data` volume.
- Uses a `pg_isready` health check before the backend may start.
- Uses local-only development credentials from `docker-compose.yml`; these are not
  production secrets.

The one-shot `urlshortener-database-init` service idempotently creates the dedicated database
and role. It never creates Auth Server tables or runs Auth Server migrations.

## Backend Service

The multi-stage image builds the JAR with Maven and runs it on a pinned Java 21 JRE as
the non-root `spring` user. Compose activates the `postgres-dev` profile and supplies
database connection variables plus the public base URL.

The service waits for PostgreSQL health and database initialization, publishes port 8080, and
reports its own health through `/actuator/health`. Liquibase applies pending changesets before
Hibernate validates the schema in both PostgreSQL profiles.

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
| `DB_USER` | `urlshortener` |
| `DB_PASSWORD` | Local-only URL Shortener password |
| `APP_BASE_URL` | Public backend URL used in generated short links |

## Ownership Boundaries

Update this document for Dockerfiles, Compose topology, PostgreSQL container behavior,
container health/dependencies, Nginx production routing, image hardening, volumes, or
container-facing environment variables. Native Spring Boot/Vite startup belongs in
`LOCAL_DEVELOPMENT_WORKFLOW.md`.

## Primary Implementation Files

- `docker-compose.yml`
- `postgres/init-urlshortener.sql`
- `scripts/ensure-local-postgres.mjs`
- `backend/Dockerfile`
- `frontend/Dockerfile`
- `frontend/nginx.conf`
- `backend/src/main/resources/application-postgres-dev.yml`
- `backend/src/main/resources/application-prod.yml`
