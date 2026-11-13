# Feature Documentation Index

This directory contains the canonical implementation documentation for completed,
full-fledged end-to-end features and substantial developer workflows.

## Documentation Model

- Maintain exactly one canonical document for each end-to-end feature.
- Do not create a new document for every ticket, bug fix, endpoint, UI component, or
  incremental change.
- When work changes an existing feature, update that feature's canonical document.
- Create a new feature document only when the implementation introduces a distinct
  end-to-end capability with its own purpose, behavior, and operational lifecycle.
- A feature spanning frontend, backend, infrastructure, or documentation still owns
  one document; describe all of its participating components there.
- Cross-cutting developer workflows may have their own document when they form a
  complete maintained capability, such as unified local development.

These rules apply whether work begins in a new chat or an existing one. Agents must use
the index below before deciding whether to update an existing document or create a new
one.

## Required Contents

Each canonical feature document should cover, where applicable:

1. The problem and intended outcome.
2. The implemented design and important decisions.
3. Setup, configuration, and usage.
4. Validation and testing performed.
5. Operational constraints and troubleshooting.
6. Files or components affected.

Keep the document focused on the feature's current end-to-end design. Historical
milestones belong in `docs/PROJECT_LOG.md`; the feature document should not become a
chronological collection of individual tasks.

## Feature Index

| End-to-end feature | Scope | Canonical document |
| :--- | :--- | :--- |
| URL Shortening and Redirection | Link creation, Sqids, custom aliases, reserved paths, expiry enforcement, and public redirects | [URL_SHORTENING_AND_REDIRECTION.md](URL_SHORTENING_AND_REDIRECTION.md) |
| Link Management and Click Analytics | Link listing, lookup, copying, refresh, soft deletion, dashboard cards, and aggregate click counts | [LINK_MANAGEMENT_AND_CLICK_ANALYTICS.md](LINK_MANAGEMENT_AND_CLICK_ANALYTICS.md) |
| API Documentation and Observability | OpenAPI, Swagger UI, Actuator, health checks, application diagnostics, and the H2 console | [API_DOCUMENTATION_AND_OBSERVABILITY.md](API_DOCUMENTATION_AND_OBSERVABILITY.md) |
| Containerized Full Stack | Docker images, Compose topology, PostgreSQL, Nginx routing, health, volumes, and container configuration | [CONTAINERIZED_FULL_STACK.md](CONTAINERIZED_FULL_STACK.md) |
| CI/CD and Release Automation | Branch promotion, application CI, Docker validation, GHCR publication, and GitHub Releases | [CI_CD_AND_RELEASE_AUTOMATION.md](CI_CD_AND_RELEASE_AUTOMATION.md) |
| Roadmap Issue and Project Synchronization | Markdown roadmap parsing, owned issues, labels, milestones, and optional Project v2 synchronization | [ROADMAP_ISSUE_AND_PROJECT_SYNC.md](ROADMAP_ISSUE_AND_PROJECT_SYNC.md) |
| Local Development Workflow | Root dependency installation and combined native Spring Boot/Vite startup | [LOCAL_DEVELOPMENT_WORKFLOW.md](LOCAL_DEVELOPMENT_WORKFLOW.md) |

Add one row whenever a distinct end-to-end feature document is introduced. Keep the
feature name and scope specific enough that future agents can identify the correct
document without opening every file in this directory.

## Routing Changes to the Correct Document

When a change touches more than one feature, update every affected canonical document.
For example, adding per-link analytics fields could affect both shortening responses and
the analytics dashboard; that work belongs in both relevant documents rather than a new
ticket-specific file. Cross-cutting architecture and historical summaries must also stay
synchronized in `docs/ARCHITECTURE.md` and `docs/PROJECT_LOG.md` when applicable.
