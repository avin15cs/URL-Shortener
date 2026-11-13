# 💻 Developer Setup & Contribution Guide

This guide walks through local environment setup, running tests, database management, and best practices.

---

## 1. Prerequisites

Before starting, ensure you have the following installed on your machine:
* **Java**: JDK 21 or higher (`java -version`)
* **Maven**: 3.8+ (`mvn -version`)
* **Node.js**: 22.12+ (`node -v`)
* **Docker & Docker Compose**: (Optional, for containerized run)
* A running clone of the sibling **`auth-server`** on port `9000`, configured with a
  Google OAuth client, signing JWK, and the URL Shortener client secret.

---

## 2. Running Locally for Development

### A. Start the authorization server

From the sibling `auth-server` repository, set a strong shared client secret and use its
local profile. The URL Shortener client defaults already use audience
`url-shortener-api` and callback `http://localhost:8080/login/oauth2/code/auth-server`.

```powershell
$env:URL_SHORTENER_CLIENT_SECRET = "a-strong-local-secret"
docker compose up -d postgres
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

The auth-server also needs its documented `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`,
`AUTH_JWK_SET`, and database settings. If the OAuth client was already bootstrapped with
a different secret, rotate it through the auth-server management API or recreate only
the disposable local auth database.

### B. Backend Development (Spring Boot)

The default profile uses an in-memory **H2 database**, so no external database is needed for local dev:

```bash
cd backend
set URL_SHORTENER_CLIENT_SECRET=a-strong-local-secret
mvn spring-boot:run
```

In PowerShell use `$env:URL_SHORTENER_CLIENT_SECRET = "a-strong-local-secret"`. The value
must exactly match the auth-server configuration. Browse to the frontend and use **Sign
in with Google**; the SPA never receives an access or refresh token.

* **API Base**: `http://localhost:8080`
* **Swagger UI Docs**: `http://localhost:8080/swagger-ui.html`
* **OpenAPI Spec**: `http://localhost:8080/v3/api-docs`
* **Health Check**: `http://localhost:8080/actuator/health`
* **H2 Web Console**: `http://localhost:8080/api/h2-console/`
  * JDBC URL: `jdbc:h2:mem:urlshortenerdb`
  * Username: `sa`
  * Password: *(empty)*
* Hibernate creates and drops the disposable H2 schema from the JPA mappings.

### C. Frontend Development (React + Vite)

In a separate terminal:

```bash
cd frontend
npm install
npm run dev
```

* **Vite Dev Server**: `http://localhost:5173`
* Vite automatically proxies `/api` and shortcode requests to the backend at `http://localhost:8080`.

---

## 3. Running with Docker Compose (Full Stack + PostgreSQL)

To test the complete production-like stack locally with a real PostgreSQL database:

```bash
copy .env.example .env
# Set URL_SHORTENER_CLIENT_SECRET in .env to the auth-server's value.
docker compose up --build
```

Start the sibling auth-server on host port `9000` first. Compose keeps the public issuer
as `http://localhost:9000` while using `host.docker.internal` for backend-to-auth-server
token, UserInfo, and JWKS calls. The host-gateway mapping is included for Linux Docker.

Services started:
* **PostgreSQL 15.19** (hardened local image based on `postgres:15.19-alpine3.24`): `127.0.0.1:5432` (host loopback only)
* **Backend**: `localhost:8080`
* **Frontend**: `localhost:80` (or `http://localhost`)

Docker Compose activates the `postgres-dev` Spring profile. Hibernate uses `ddl-auto: update`
for this disposable local development database. The `prod` profile enables Liquibase,
applies `db/changelog/db.changelog-master.sql`, and then uses `ddl-auto: validate`.

The PostgreSQL base image is pinned to an exact database and Alpine version. The local
Dockerfile applies current Alpine 3.24 security updates and rebuilds the entrypoint's `gosu`
helper from its pinned upstream source commit. To build/start only PostgreSQL, verify it,
and open `psql`:

```bash
docker compose up -d --build postgres
docker compose ps
docker exec -it urlshortener-db psql -U postgres -d urlshortener
```

Use `\q` to leave `psql`.

### Connecting with DataGrip or IntelliJ IDEA Ultimate

Create a PostgreSQL data source with these local-development settings:

| Setting | Value |
| :--- | :--- |
| Host | `localhost` |
| Port | `5432` |
| Database | `urlshortener` |
| User | `postgres` |
| Password | `password` |
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
| `FRONTEND_URL` | Login/logout success redirect. | `http://localhost:5173` |
| `COOKIE_SECURE` | Send the BFF session cookie only over HTTPS. | `false` locally; must be `true` in production |

Assign `URL_ADMIN` for audience `url-shortener-api` through the auth-server management
API to enable the cross-owner list and search UI. Normal users and service clients remain
restricted to their own records.

To stop all containers and retain data volume:
```bash
docker compose down
```

To stop containers and wipe database volumes:
```bash
docker compose down -v
```

---

## 4. Running Tests

For the complete cross-repository acceptance flow, including two-user ownership,
`URL_ADMIN`, service-client scopes, CSRF, and Docker, follow
[`AUTH_INTEGRATION_TESTING.md`](AUTH_INTEGRATION_TESTING.md).

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

## 5. Git Workflow for Features

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

## 6. Managing the Roadmap from Markdown

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
