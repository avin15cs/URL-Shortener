# CI/CD and Release Automation

> Canonical feature document for protected branch promotion, automated verification,
> container publication, and GitHub Release creation.

## Branch and Promotion Model

Development flows from a feature, fix, or chore branch into `dev`. Production promotion
is a pull request from `dev` to `main`. The CI promotion-policy job rejects pull requests
to `main` from any other source branch.

Repository rulesets protect `dev` and `main` with pull requests, required checks,
resolved conversations, and blocked force pushes/deletions. CI is the merge gate.

## Continuous Integration

`.github/workflows/ci.yml` runs for pushes and pull requests on `dev` and `main`, plus
manual dispatch. Superseded runs on the same ref are cancelled.

| Job | Responsibilities |
| :--- | :--- |
| Branch Promotion Policy | Enforce `dev -> main` production promotion |
| Frontend CI | Node 22 setup, locked install, ESLint, TypeScript/Vite build, artifact upload |
| Backend CI | Java 21 setup, roadmap validation, Maven tests, JAR packaging, artifact upload |
| Docker Images Build Check | Build backend and frontend images without publishing |

The Docker validation waits for both application jobs, so container regressions cannot
be hidden behind successful source builds.

## Continuous Delivery

`.github/workflows/cd.yml` runs on pushes to `main` and manual dispatch. It grants only
the content and package permissions required to publish artifacts and releases, and it
serializes runs per ref rather than cancelling a release already in progress.

The publication job authenticates to GHCR with `GITHUB_TOKEN`, builds both Dockerfiles,
and publishes branch, short-SHA, and `latest` tags for:

- `ghcr.io/avin15cs/url-shortener-backend`
- `ghcr.io/avin15cs/url-shortener-frontend`

After image publication, the release job creates a timestamped GitHub tag and Release
with generated notes. `target_commitish` is the exact triggering commit so the Release
cannot silently point at an older default-branch revision.

## Security and Reproducibility

- Workflow permissions are explicitly scoped.
- Node and Maven caches use their lock/build metadata.
- Docker builds use Buildx and GitHub Actions caching.
- Application and runtime base images are pinned in their Dockerfiles.
- Registry authentication does not require a separately stored container password.

## Ownership Boundaries

Update this document for branch promotion policy, protected checks, application CI,
Docker build validation, GHCR publishing, release creation, workflow permissions, or
release concurrency. Roadmap issue synchronization belongs in
`ROADMAP_ISSUE_AND_PROJECT_SYNC.md`.

## Primary Implementation Files

- `.github/workflows/ci.yml`
- `.github/workflows/cd.yml`
- `.github/pull_request_template.md`
- `backend/Dockerfile`
- `frontend/Dockerfile`
- `frontend/package-lock.json`
- `backend/pom.xml`
