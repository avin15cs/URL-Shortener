# 💻 Developer Setup & Contribution Guide

This guide walks through local environment setup, running tests, database management, and best practices.

---

## 1. Prerequisites

Before starting, ensure you have the following installed on your machine:
* **Java**: JDK 21 or higher (`java -version`)
* **Maven**: 3.8+ (`mvn -version`)
* **Node.js**: 22.12+ (`node -v`)
* **Docker & Docker Compose**: (Optional, for containerized run)
* A running clone of the sibling **`auth-server`** with its backend on port `9000` and
  login/admin UI on port `5173`, configured according to that repository's guide.

---

## 2. Running Locally for Development

### A. Start the authorization server

From the sibling `auth-server` repository, copy its `.env.example`, generate a private
development JWK Set, and configure either Google login or the local-profile administrator.
The URL Shortener client must use audience `url-shortener-api`, callback
`http://localhost:8080/login/oauth2/code/auth-server`, and post-logout URI
`http://localhost:5174/`.

```powershell
cd ..\auth-server
npm ci
npm --prefix admin-ui ci
npm run dev
```

Set the same strong `URL_SHORTENER_CLIENT_SECRET` in both repositories. The latest auth
server supports Google and administrator-invited local accounts; development-only local
login additionally requires `LOCAL_LOGIN_ENABLED=true` under its `local` profile. If the
client already exists with different metadata, update or rotate it through the auth-server
administration UI because bootstrap changes do not rewrite persisted secrets.

### B. Start Backend and Frontend Together

For the implementation design, validation record, and troubleshooting details, see
[Local Development Workflow](features/LOCAL_DEVELOPMENT_WORKFLOW.md).

From the repository root, install the locked development tooling and frontend
dependencies:

```bash
npm ci
```

The root install runs `npm ci --prefix frontend` automatically, so both dependency
sets remain reproducible while the frontend keeps its independent package boundary
for CI and Docker builds.

Start both development servers with one command:

```bash
npm run dev
```

This idempotently starts the shared `postgres-db` container, provisions the isolated
`urlshortener` database and role, applies Liquibase changesets, starts Spring Boot with
the `postgres-dev` profile on port `8080`, and starts Vite with hot reload on port `5174`.
Output is labeled `backend` and `frontend`; if either process fails, the other is stopped.
Press `Ctrl+C` once to stop the application processes. PostgreSQL remains available for
Auth Server or the next URL Shortener run.

Do not run this command at the same time as the Docker Compose stack because both
backends bind port `8080`. Run `docker compose down` first when switching from the
containerized stack.

On Windows PowerShell systems where script execution policy blocks `npm.ps1`, use:

```powershell
npm.cmd ci
npm.cmd run dev
```

The execution policy is a machine/user setting and is not changed by this project.

To start only one service from the repository root, use `npm run dev:backend` or
`npm run dev:frontend`.

### C. Backend Development (Spring Boot)

The root backend command uses the shared PostgreSQL database:

```bash
npm run dev:backend
```

Set `URL_SHORTENER_CLIENT_SECRET` in the ignored root `.env` file to exactly match the
auth-server configuration. Browse to the frontend and use **Sign in**; provider selection
and local credential handling remain inside the auth-server, and the SPA never receives
an access or refresh token.

* **API Base**: `http://localhost:8080`
* **Swagger UI Docs**: `http://localhost:8080/swagger-ui.html`
* **OpenAPI Spec**: `http://localhost:8080/v3/api-docs`
* **Health Check**: `http://localhost:8080/actuator/health`
Liquibase owns the PostgreSQL schema and Hibernate validates it. To run the backend alone with
the disposable H2 default profile, execute `mvn spring-boot:run` directly from `backend/`.
That isolated mode retains the H2 console at `http://localhost:8080/api/h2-console/`.

### D. Frontend Development (React + Vite)

In a separate terminal:

```bash
cd frontend
npm install
npm run dev
```

* **Vite Dev Server**: `http://localhost:5174`
* Vite automatically proxies `/api` requests to the backend at `http://localhost:8080`.

---

## 3. Running with Docker Compose (Full Stack + PostgreSQL)

To test the complete production-like stack locally with a real PostgreSQL database:

```bash
copy .env.example .env
# Set URL_SHORTENER_CLIENT_SECRET to the auth-server client's value.
npm run compose:up
```

Start the sibling auth-server on host port `9000` first. Compose keeps the public issuer
as `http://localhost:9000` while using `host.docker.internal` for backend-to-auth-server
token, UserInfo, and JWKS calls. The host-gateway mapping is included for Linux Docker.

Services started:
* **PostgreSQL 15.19** (`postgres-db`, shared with Auth Server): `127.0.0.1:5432` (host loopback only)
* **Backend**: `localhost:8080`
* **Frontend**: `localhost:80` (or `http://localhost`)

Docker Compose activates the `postgres-dev` Spring profile. Liquibase applies versioned
changesets and Hibernate uses `ddl-auto: validate`. Production uses the same migration model
with a service-specific PostgreSQL endpoint.

Both repositories declare the same pinned PostgreSQL service, Compose project, network, volume,
and container name. Whichever starts first creates `postgres-db`; the other reuses it and runs
only its idempotent database initializer. To ensure PostgreSQL, verify it, and open `psql`:

```bash
npm run db:ensure
docker ps --filter name=postgres-db
docker exec -it postgres-db psql -U urlshortener -d urlshortener
```

Use `\q` to leave `psql`.

### Connecting with DataGrip or IntelliJ IDEA Ultimate

Create a PostgreSQL data source with these local-development settings:

| Setting | Value |
| :--- | :--- |
| Host | `localhost` |
| Port | `5432` |
| Database | `urlshortener` |
| User | `urlshortener` |
| Password | `local-urlshortener-password` |
| Schema | `public` |
| JDBC URL | `jdbc:postgresql://localhost:5432/urlshortener` |

Download the PostgreSQL driver when the IDE prompts, then use **Test Connection**. These
credentials are intentionally local-only and must not be reused in a shared or production
environment.

### OAuth and BFF environment variables

| Variable | Purpose | Default |
| :--- | :--- | :--- |
| `AUTH_ISSUER` | Exact JWT issuer validated in `iss`. | `http://localhost:9000` |
| `AUTH_AUDIENCE` | Required access-token audience. | `url-shortener-api` |
| `AUTH_AUTHORIZATION_URI` | Browser-facing authorization endpoint. | `http://localhost:9000/oauth2/authorize` |
| `AUTH_TOKEN_URI` | Backend-reachable token endpoint. | `http://localhost:9000/oauth2/token` |
| `AUTH_JWK_SET_URI` | Backend-reachable signing-key endpoint. | `http://localhost:9000/oauth2/jwks` |
| `AUTH_USER_INFO_URI` | Backend-reachable OIDC UserInfo endpoint. | `http://localhost:9000/userinfo` |
| `URL_SHORTENER_CLIENT_ID` | Confidential OAuth client ID. | `url-shortener` |
| `URL_SHORTENER_CLIENT_SECRET` | Confidential OAuth client secret. | Development placeholder only |
| `URL_SHORTENER_REDIRECT_URI` | Exact registered login callback. | `http://localhost:8080/login/oauth2/code/auth-server` |
| `FRONTEND_URL` | Login/logout success redirect. | `http://localhost:5174` |
| `COOKIE_SECURE` | Send the BFF session cookie only over HTTPS. | `false` locally; must be `true` in production |

Assign `URL_ADMIN` for audience `url-shortener-api` through the auth-server management
API to enable the cross-owner list and search UI. Normal users and service clients remain
restricted to their own records.

To stop the URL Shortener application containers while retaining the shared database:
```bash
docker compose stop backend frontend
```

Stop `postgres-db` only when neither application needs it:
```bash
docker stop postgres-db
```

Never use `docker compose down -v` for ordinary development. The
`url-shortener_postgres_data` volume contains both services' local databases.

## 4. Database Changes

- Add ordered formatted-SQL changesets under
  `backend/src/main/resources/db/changelog/changes/` and include them from the master changelog.
- Never edit a changeset that has been applied.
- PostgreSQL profiles run Liquibase before Hibernate validation.
- The original baseline marks itself applied when adopting an existing Hibernate-created
  `urls` table; subsequent schema changes always use forward changesets.
- PostgreSQL Testcontainers verifies the changelog against the real database engine.

---

## 5. Running Tests

For the complete cross-repository acceptance flow, including two-user ownership,
`URL_ADMIN`, service-client scopes, CSRF, and Docker, follow
[`Authentication and URL Ownership`](features/AUTHENTICATION_AND_URL_OWNERSHIP.md).

### Backend Unit Tests:
```bash
cd backend
mvn test
```

### Frontend Linting & Build Verification:
```bash
cd frontend
npm run build
npm run lint
```

---

## 6. Git Workflow for Features

Always branch off `dev`:

```bash
# 1. Update your local dev branch
git checkout dev
git pull origin dev

# 2. Create your feature branch
git checkout -b feature/my-new-feature

# 3. Make changes and commit
git add .
git commit -m "feat: description of feature"

# 4. Push to origin
git push -u origin feature/my-new-feature

# 5. Open a Pull Request on GitHub targeting 'dev'
```

---

## 7. Managing the Roadmap from Markdown

[`ROADMAP.md`](ROADMAP.md) is the source of truth for planned work. When a roadmap
change reaches `dev`, GitHub Actions creates or updates the corresponding labels,
milestones, and owned issues. Marking a task complete closes its managed issue.

Each task must end with a unique stable marker:

```markdown
- [ ] Add link expiry -- Support a configurable TTL <!-- roadmap-id: link-expiry -->
```

Do not rename a `roadmap-id` after synchronization. The workflow only changes issues
that have both the `roadmap` label and its generated ownership marker, so unrelated
issues with matching titles remain untouched.

Issue synchronization uses the workflow's built-in `GITHUB_TOKEN` and needs no setup.
To also create and maintain the repository's GitHub Project, add a repository Actions
secret named `PROJECT_TOKEN`. Because this repository's Project is owned by a personal
account, use a personal access token (classic) with the `project` and `repo` scopes.
GitHub currently does not support fine-grained personal access tokens for user-owned
Projects. Give the token a short expiration and rotate it when it expires.

Create it under **GitHub Settings -> Developer settings -> Personal access tokens**, then
add it under **Repository Settings -> Secrets and variables -> Actions**. Never commit
or paste the token into `ROADMAP.md`. Without this secret, issue synchronization still
works and the Project step is skipped explicitly.

Validate roadmap syntax and automation locally with:

```bash
python .github/scripts/test_sync_roadmap.py
python .github/scripts/sync_roadmap.py --check
```
