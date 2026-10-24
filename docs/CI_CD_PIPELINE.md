# 🔄 CI/CD Pipeline & Branching Guide

This repository uses **GitHub Actions** for automated continuous integration, testing, packaging, and continuous delivery.

---

## 1. Git Branching Strategy

We follow a GitFlow-inspired branching strategy:

```
[feature/xxx] ──> [dev] ──(PR)──> [release] ──(PR)──> [main]
                    ▲                 ▲                  ▲
                    │                 │                  │
                CI runs           CI + Docker        CI + Production
               all tests         Auto-Tagged           Containers
                                   Release             to GHCR
```

* **`dev`**: Daily integration branch. Active development merges here. Every push triggers full CI checks.
* **`release`**: Release candidates. Pushing here triggers CI, builds production Docker images, tags a new version (e.g. `v1.0.20261024...`), and publishes a **GitHub Release**.
* **`main`**: Production-ready code. Represents the current live state. Pushes automatically build and tag `:latest` container images on GHCR.

---

## 2. GitHub Actions Workflows

### Pipeline 1: Continuous Integration (`.github/workflows/ci.yml`)

Runs on:
- Every push to `dev`, `release`, and `main`
- Every Pull Request targeting `dev`, `release`, or `main`
- Manual execution via `workflow_dispatch`

#### Jobs:
1. **`frontend-ci`**:
   - Environment: `ubuntu-latest`
   - Node.js 20 with `npm` caching.
   - Installs dependencies: `npm ci`
   - Compiles TypeScript & bundles Vite: `npm run build`
   - Saves production build artifacts (`frontend-dist`).
2. **`backend-ci`**:
   - Environment: `ubuntu-latest`
   - JDK 17 (Eclipse Temurin) with Maven caching.
   - Runs tests: `mvn clean test -B`
   - Packages JAR: `mvn package -DskipTests -B`
   - Saves build artifact (`backend-jar`).
3. **`docker-check`**:
   - Builds both `backend/Dockerfile` and `frontend/Dockerfile` using Docker Buildx without pushing, catching container regression early.

---

### Pipeline 2: Continuous Delivery (`.github/workflows/cd.yml`)

Runs on:
- Pushes to `release` and `main`

#### Features:
1. **GitHub Container Registry (GHCR) Publishing**:
   - No external credentials required — uses the native `${{ secrets.GITHUB_TOKEN }}`.
   - Images generated:
     - `ghcr.io/avin15cs/url-shortener-backend:<sha>` and `:latest`
     - `ghcr.io/avin15cs/url-shortener-frontend:<sha>` and `:latest`
2. **Automated GitHub Release**:
   - When a commit is merged into `release`, it automatically creates an official GitHub Release with generated changelog notes.

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
