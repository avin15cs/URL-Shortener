# 🔄 CI/CD Pipeline & Branching Guide

This repository uses **GitHub Actions** for automated continuous integration, testing, packaging, and continuous delivery.

---

## Branch Promotion Policy

Changes flow through pull requests in this order:

1. Feature or completion branch → `dev`
2. `dev` → `main`

The CI `Branch Promotion Policy` job rejects pull requests into `main` from any branch
other than `dev`.
The CI workflow uses read-only repository permissions and cancels superseded runs for the
same ref. CD runs are serialized per ref so image publication and release creation do not
overlap.

Active GitHub rulesets protect both long-lived branches. They require pull requests,
successful CI jobs, resolved conversations, and disable force pushes and deletions.
There are no bypass actors, including administrators. Because this is a solo-maintainer
repository, the rulesets do not require a separate approving review; CI is the merge gate.

---

## 1. Git Branching Strategy

We follow a lightweight two-branch strategy:

```
[feature/xxx] ──> [dev] ──(PR)──> [main]
                    ▲                 ▲
                    │                 │
                CI runs       CI + Production Containers
               all tests       + GitHub Release to GHCR
```

* **`dev`**: Daily integration branch. Active development merges here. Every push triggers full CI checks.
* **`main`**: Production-ready code promoted only from `dev`. Pushes build and tag `:latest` container images on GHCR and publish a **GitHub Release**.

---

## 2. GitHub Actions Workflows

### Pipeline 1: Continuous Integration (`.github/workflows/ci.yml`)

Runs on:
- Every push to `dev` and `main`
- Every Pull Request targeting `dev` or `main`
- Manual execution via `workflow_dispatch`

#### Jobs:
1. **`frontend-ci`**:
   - Environment: `ubuntu-latest`
   - Node.js 22 (Active LTS) with `npm` caching.
   - Installs dependencies: `npm ci`
   - Compiles TypeScript & bundles Vite: `npm run build`
   - Saves production build artifacts (`frontend-dist`).
2. **`backend-ci`**:
   - Environment: `ubuntu-latest`
   - JDK 21 (Eclipse Temurin) with Maven caching.
   - Runs tests: `mvn clean test -B`, including Liquibase migrations against PostgreSQL Testcontainers.
   - Packages JAR: `mvn package -DskipTests -B`
   - Saves build artifact (`backend-jar`).
3. **`docker-check`**:
   - Builds both `backend/Dockerfile` and `frontend/Dockerfile` using Docker Buildx without pushing, catching container regression early.

The backend CI job also tests the roadmap parser and validates every task's stable
`roadmap-id`, so invalid Markdown cannot pass the existing protected-branch check.

---

### Pipeline 2: Continuous Delivery (`.github/workflows/cd.yml`)

Runs on:
- Pushes to `main`

#### Features:
1. **GitHub Container Registry (GHCR) Publishing**:
   - No external credentials required — uses the native `${{ secrets.GITHUB_TOKEN }}`.
   - Images generated:
     - `ghcr.io/avin15cs/url-shortener-backend:<sha>` and `:latest`
     - `ghcr.io/avin15cs/url-shortener-frontend:<sha>` and `:latest`
2. **Automated GitHub Release**:
   - When a promotion PR is merged into `main`, it automatically creates an official GitHub Release with generated changelog notes, targeting that exact commit.

---

### Pipeline 3: Roadmap Synchronization (`.github/workflows/sync-roadmap.yml`)

Runs when roadmap automation files change on `dev`, or by manual dispatch. It validates
`docs/ROADMAP.md`, synchronizes owned GitHub issues, labels, and milestones, and optionally
adds those issues to the **URL Shortener Roadmap** GitHub Project. Project synchronization
requires the repository Actions secret `PROJECT_TOKEN`; issue synchronization uses the
built-in `GITHUB_TOKEN`.

---

## 3. How to Deploy

### Option A: Container deployment on a Cloud VPS (AWS / DigitalOcean / Linode)

Deploy the published backend and frontend images with a production-specific Compose or
orchestration manifest. The repository `docker-compose.yml` is intentionally for local
development: it uses the shared `postgres-db` container, local-only credentials, and the
`postgres-dev` profile. Production must instead set `SPRING_PROFILES_ACTIVE=prod`, inject
database credentials through secrets, and point the backend at URL Shortener's dedicated
PostgreSQL instance.

### Option B: Deploying GHCR Images directly
```bash
# Pull the latest backend and frontend images directly from GitHub
docker pull ghcr.io/avin15cs/url-shortener-backend:latest
docker pull ghcr.io/avin15cs/url-shortener-frontend:latest
```

### Option C: PaaS (Render, Railway, Fly.io)
1. Link your GitHub account to the platform.
2. Select repository `avin15cs/URL-Shortener`.
3. Set the backend root to `backend/` and frontend root to `frontend/`.
4. Add environment variable `SPRING_PROFILES_ACTIVE=prod`.
