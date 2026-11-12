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

Recommended GitHub branch protection for both long-lived branches requires pull
requests, successful CI jobs, resolved conversations, linear history, and disabled force
pushes/deletions. GitHub requires a paid plan to enforce these rules on a private repository.

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
   - Runs tests: `mvn clean test -B`
   - Packages JAR: `mvn package -DskipTests -B`
   - Saves build artifact (`backend-jar`).
3. **`docker-check`**:
   - Builds both `backend/Dockerfile` and `frontend/Dockerfile` using Docker Buildx without pushing, catching container regression early.

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

## 3. How to Deploy

### Option A: Using Docker Compose on a Cloud VPS (AWS / DigitalOcean / Linode)

Run on your server:
```bash
git clone https://github.com/avin15cs/URL-Shortener.git
cd URL-Shortener
docker compose up -d --build
```

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
