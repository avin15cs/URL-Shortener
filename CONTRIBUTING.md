# Contributing to URL-Shortener

Thank you for your interest in contributing! Please read this guide before opening a PR.

---

## 🌿 Branch Strategy

```
feature/* ──► dev ──► main (release)
```

| Branch | Purpose | Who can merge? |
|--------|---------|----------------|
| `feature/*` | Your work | You (via PR → `dev`) |
| `dev` | Integration / staging | Maintainers via PR after CI passes |
| `main` | Production release | Maintainers via a `dev` → `main` PR |

> **Direct pushes to `dev` and `main` are blocked by branch rulesets.**

---

## ✅ How to Contribute

### 1. Fork & Branch

```bash
# Always branch off dev
git checkout dev
git pull origin dev
git checkout -b feature/your-feature-name
```

Branch naming:
- `feature/short-description` — new features
- `fix/short-description` — bug fixes
- `chore/short-description` — maintenance / deps

### 2. Make Your Changes

- Follow existing code style and conventions.
- Keep commits focused and atomic.
- Write clear commit messages (e.g. `feat: add QR code download button`).

### 3. Open a Pull Request → `dev`

- **Target branch must be `dev`** — PRs targeting `main` will be rejected.
- Fill in the PR template.
- Ensure CI checks pass (lint, build, tests).
- Request a review from a maintainer.

### 4. After Merge into `dev`

Once your feature is merged into `dev`, it goes through integration testing. When a release is ready, a maintainer promotes `dev` → `main` through a CI-gated PR — you don't need to do anything.

---

## 🚫 What NOT to Do

| ❌ Don't | ✅ Do instead |
|---------|--------------|
| Push directly to `dev` | Open a PR from `feature/*` → `dev` |
| Push directly to `main` | Admins handle `dev` → `main` promotions |
| Merge an external contribution without review | Wait for maintainer review |

---

## 🛠️ Local Dev Setup

```bash
# Backend (Java 21 + Maven)
cd backend && mvn spring-boot:run        # http://localhost:8080

# Frontend (Node 22 + Vite)
cd frontend && npm install && npm run dev # http://localhost:5173

# Full stack (Docker)
docker compose up --build                # http://localhost:80
```

API docs: http://localhost:8080/swagger-ui.html

---

## ❓ Questions?

Open a [Discussion](../../discussions) or ping a maintainer in your PR.
