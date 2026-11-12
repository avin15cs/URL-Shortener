# 💻 Developer Setup & Contribution Guide

This guide walks through local environment setup, running tests, database management, and best practices.

---

## 1. Prerequisites

Before starting, ensure you have the following installed on your machine:
* **Java**: JDK 21 or higher (`java -version`)
* **Maven**: 3.8+ (`mvn -version`)
* **Node.js**: 22.12+ (`node -v`)
* **Docker & Docker Compose**: (Optional, for containerized run)

---

## 2. Running Locally for Development

### A. Backend Development (Spring Boot)

The default profile uses an in-memory **H2 database**, so no external database is needed for local dev:

```bash
cd backend
mvn spring-boot:run
```

* **API Base**: `http://localhost:8080`
* **Swagger UI Docs**: `http://localhost:8080/swagger-ui.html`
* **OpenAPI Spec**: `http://localhost:8080/v3/api-docs`
* **Health Check**: `http://localhost:8080/actuator/health`
* **H2 Web Console**: `http://localhost:8080/api/h2-console/`
  * JDBC URL: `jdbc:h2:mem:urlshortenerdb`
  * Username: `sa`
  * Password: *(empty)*
* Hibernate creates and drops the disposable H2 schema from the JPA mappings.

### B. Frontend Development (React + Vite)

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
docker compose up --build
```

Services started:
* **PostgreSQL 15.19** (hardened local image based on `postgres:15.19-alpine3.24`): `127.0.0.1:5432` (host loopback only)
* **Backend**: `localhost:8080`
* **Frontend**: `localhost:80` (or `http://localhost`)

Docker Compose activates the `postgres-dev` Spring profile. Hibernate uses `ddl-auto: update`
for this local development database. The `prod` profile remains on `ddl-auto: validate` and
must be paired with Liquibase before the first persistent deployment.

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
secret named `PROJECT_TOKEN`. Use a dedicated fine-grained personal access token with:

* access limited to this repository;
* read and write access to user Projects; and
* read and write access to Issues.

Create it under **GitHub Settings -> Developer settings -> Personal access tokens**, then
add it under **Repository Settings -> Secrets and variables -> Actions**. Never commit
or paste the token into `ROADMAP.md`. Without this secret, issue synchronization still
works and the Project step is skipped explicitly.

Validate roadmap syntax and automation locally with:

```bash
python .github/scripts/test_sync_roadmap.py
python .github/scripts/sync_roadmap.py --check
```
