# Roadmap Issue and Project Synchronization

> Canonical feature document for converting the Markdown roadmap into owned GitHub
> issues, labels, milestones, and optional Project v2 membership.

## Purpose and Source of Truth

`docs/ROADMAP.md` is the declarative source for roadmap work. When relevant changes
reach `dev`, automation validates the file and synchronizes the GitHub planning state.
The workflow is deliberately ownership-safe: it modifies only issues carrying both the
roadmap label and a generated marker tied to a stable task ID.

## Markdown Contract

Phase headings declare optional label and milestone metadata:

```markdown
## Phase Name [label:enhancement] [milestone:v1.1.0]
```

Every task requires a unique stable ID:

```markdown
- [ ] Feature title -- optional description <!-- roadmap-id: stable-id -->
```

The ID is the durable identity and must not be renamed after synchronization. Checking
a task closes its managed issue; unchecking it reopens that issue.

## Synchronization Lifecycle

1. The parser validates phase metadata, task syntax, unique IDs, and non-empty titles.
2. Required labels and milestones are created when missing.
3. Existing owned issues are discovered by their generated marker.
4. Issues are created or updated with title, description, labels, milestone, and state.
5. When `PROJECT_TOKEN` is configured, owned issues are added to the named Project v2
   and their status is updated.
6. Without `PROJECT_TOKEN`, issue synchronization succeeds and project synchronization
   is explicitly skipped.

Completed tasks without an existing managed issue are not retroactively created merely
to be closed.

## Workflow and Permissions

`.github/workflows/sync-roadmap.yml` runs on `dev` when the roadmap, synchronization
script, its tests, or the workflow changes; it also supports manual dispatch. It uses
read-only content access plus issue write access.

Issue synchronization uses `GITHUB_TOKEN`. Optional Project v2 synchronization for the
personal-account project uses the separately configured `PROJECT_TOKEN` and
`ROADMAP_PROJECT_TITLE`.

The main backend CI job also runs parser tests and `--check`, making valid roadmap syntax
part of the protected merge gate.

## Local Validation

```bash
python .github/scripts/test_sync_roadmap.py
python .github/scripts/sync_roadmap.py --check
```

These commands do not mutate GitHub. A live synchronization requires the workflow
environment and repository credentials.

## Ownership Boundaries

Update this document for roadmap Markdown syntax, stable identity, ownership markers,
issue/label/milestone reconciliation, Project v2 behavior, tokens, or sync triggers.
General CI and release behavior belongs in `CI_CD_AND_RELEASE_AUTOMATION.md`.

## Primary Implementation Files

- `docs/ROADMAP.md`
- `.github/scripts/sync_roadmap.py`
- `.github/scripts/test_sync_roadmap.py`
- `.github/workflows/sync-roadmap.yml`
- `.github/workflows/ci.yml`
