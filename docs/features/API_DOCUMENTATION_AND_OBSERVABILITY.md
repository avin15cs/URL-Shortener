# API Documentation and Observability

> Canonical feature document for OpenAPI discovery, Swagger UI, Actuator diagnostics,
> health checks, and the development H2 console.

## Purpose

The backend exposes interactive API discovery and operational diagnostics during local
development while reducing its production surface. These endpoints also support Docker
health checks and developer database inspection.

## Development Endpoints

| Endpoint | Purpose |
| :--- | :--- |
| `/swagger-ui.html` | Interactive Swagger UI |
| `/v3/api-docs` | OpenAPI 3 JSON document |
| `/actuator/health` | Application health |
| `/actuator/info` | Build and application metadata |
| `/actuator/metrics` | Runtime metrics |
| `/actuator/env` | Development environment diagnostics |
| `/actuator/beans` | Spring bean diagnostics |
| `/api/h2-console/` | Browser console for the default in-memory H2 database |

`OpenApiConfig` supplies the title, description, license, project contact, version, and
server URLs. SpringDoc also includes exposed Actuator endpoints in development.

## Profile Behavior

- The default profile enables Swagger/OpenAPI, development Actuator endpoints, and the
  H2 console.
- `postgres-dev` disables the H2 console while retaining local diagnostics.
- `prod` disables Swagger UI and the OpenAPI JSON and exposes only health without
  detailed output.

The production restrictions are configuration controls, not authentication. Any future
expansion of production diagnostics must be reviewed as a security-sensitive change.

## Routing Safety

Documentation and management route prefixes are registered in `app.reserved-paths` so
custom aliases cannot capture them and the single-segment redirect handler cannot treat
them as public codes. H2 console convenience redirects normalize `/h2-console`,
`/h2-console/`, and `/api/h2-console` to `/api/h2-console/`.

The backend container installs `curl`, and Compose uses `/actuator/health` to determine
service health.

## Ownership Boundaries

Update this document for SpringDoc/OpenAPI, Swagger UI, Actuator exposure, service
health, application info, H2 console access, or their profile-specific security.
Business endpoint contracts remain in their owning feature documents.

## Primary Implementation Files

- `backend/src/main/java/com/urlshortener/config/OpenApiConfig.java`
- `backend/src/main/java/com/urlshortener/config/AppProperties.java`
- `backend/src/main/java/com/urlshortener/config/WebConfig.java`
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/application-postgres-dev.yml`
- `backend/src/main/resources/application-prod.yml`
- `backend/pom.xml`
- `backend/Dockerfile`
- `docker-compose.yml`
