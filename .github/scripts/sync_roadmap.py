#!/usr/bin/env python3
"""
sync_roadmap.py — Parses docs/ROADMAP.md and syncs tasks to GitHub Issues.

Rules:
  - Each `- [ ] Title — description` line becomes a GitHub Issue (if not already open).
  - Each `- [x] Title` line closes the matching open issue (if one exists).
  - Phase headers `## Phase Name [label:X] [milestone:Y]` set the label and milestone
    applied to all tasks in that phase.
  - Issues created by this script are tagged with the `roadmap` label for easy filtering.
  - Re-running is idempotent: existing open issues are skipped, not duplicated.
"""

import json
import os
import re
import subprocess
import sys

# ---------------------------------------------------------------------------
# Config
# ---------------------------------------------------------------------------
ROADMAP_FILE = "docs/ROADMAP.md"
REPO = os.environ.get("GITHUB_REPOSITORY", "avin15cs/URL-Shortener")
GH = ["gh", "--repo", REPO]

ROADMAP_LABEL        = "roadmap"
ROADMAP_LABEL_COLOR  = "0075ca"
ROADMAP_LABEL_DESC   = "Tracked in docs/ROADMAP.md"

PHASE_RE   = re.compile(r"^## (.+?)(?:\s*\[label:([^\]]+)\])?(?:\s*\[milestone:([^\]]+)\])?")
OPEN_RE    = re.compile(r"^- \[ \] (.+)")
CLOSED_RE  = re.compile(r"^- \[x\] (.+)", re.IGNORECASE)
EM_DASH_RE = re.compile(r"\s+[—–-]{1,2}\s+")  # em-dash, en-dash, or double-dash


def run(cmd, **kwargs):
    return subprocess.run(cmd, capture_output=True, text=True, **kwargs)


# ---------------------------------------------------------------------------
# Label helpers
# ---------------------------------------------------------------------------
def ensure_label(name: str, color: str = "ededed", description: str = ""):
    run(GH + ["label", "create", name,
               "--color", color,
               "--description", description,
               "--force"])


def ensure_milestone(name: str) -> str | None:
    """Create milestone if missing; return its number as a string."""
    list_result = run(GH + ["api", f"repos/{REPO}/milestones",
                             "--jq", f'[.[] | select(.title=="{name}")] | first | .number'])
    num = list_result.stdout.strip()
    if num and num != "null":
        return num
    create_result = run(GH + ["api", f"repos/{REPO}/milestones",
                               "--method", "POST",
                               "--field", f"title={name}"])
    data = json.loads(create_result.stdout or "{}")
    return str(data.get("number", "")) or None


# ---------------------------------------------------------------------------
# Issue helpers
# ---------------------------------------------------------------------------
def get_open_issues() -> dict[str, int]:
    """Return {title: number} for all open issues."""
    result = run(GH + ["issue", "list", "--state", "open",
                        "--json", "title,number", "--limit", "500"])
    return {i["title"]: i["number"] for i in json.loads(result.stdout or "[]")}


def issue_exists_closed(title: str) -> bool:
    result = run(GH + ["issue", "list", "--state", "closed",
                        "--search", f'"{title}" in:title',
                        "--json", "title", "--limit", "50"])
    items = json.loads(result.stdout or "[]")
    return any(i["title"] == title for i in items)


def create_issue(title: str, body: str, labels: list[str], milestone: str | None):
    args = GH + ["issue", "create", "--title", title, "--body", body]
    for label in labels:
        args += ["--label", label]
    if milestone:
        args += ["--milestone", milestone]
    result = run(args)
    if result.returncode == 0:
        print(f"  ✅ Created issue: {title}")
    else:
        print(f"  ❌ Failed to create '{title}': {result.stderr.strip()}", file=sys.stderr)


def close_issue(number: int, title: str):
    result = run(GH + ["issue", "close", str(number),
                        "--comment", "✅ Marked as done in `docs/ROADMAP.md`."])
    if result.returncode == 0:
        print(f"  🔒 Closed issue #{number}: {title}")
    else:
        print(f"  ❌ Failed to close #{number}: {result.stderr.strip()}", file=sys.stderr)


# ---------------------------------------------------------------------------
# Parser
# ---------------------------------------------------------------------------
def parse_roadmap() -> list[dict]:
    """
    Returns a list of task dicts:
      { title, description, label, milestone, done }
    """
    tasks = []
    current_label = None
    current_milestone = None

    with open(ROADMAP_FILE, encoding="utf-8") as f:
        for line in f:
            line = line.rstrip()

            # Phase header
            m = PHASE_RE.match(line)
            if m:
                current_label     = (m.group(2) or "").strip() or None
                current_milestone = (m.group(3) or "").strip() or None
                continue

            # Open task
            m = OPEN_RE.match(line)
            if m:
                raw = m.group(1).strip()
                parts = EM_DASH_RE.split(raw, maxsplit=1)
                tasks.append({
                    "title":       parts[0].strip(),
                    "description": parts[1].strip() if len(parts) > 1 else "",
                    "label":       current_label,
                    "milestone":   current_milestone,
                    "done":        False,
                })
                continue

            # Closed/done task
            m = CLOSED_RE.match(line)
            if m:
                raw = m.group(1).strip()
                parts = EM_DASH_RE.split(raw, maxsplit=1)
                tasks.append({
                    "title":       parts[0].strip(),
                    "description": parts[1].strip() if len(parts) > 1 else "",
                    "label":       current_label,
                    "milestone":   current_milestone,
                    "done":        True,
                })

    return tasks


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
def main():
    print(f"📋 Syncing ROADMAP.md → GitHub Issues for {REPO}")

    # Ensure base roadmap label exists
    ensure_label(ROADMAP_LABEL, ROADMAP_LABEL_COLOR, ROADMAP_LABEL_DESC)

    tasks       = parse_roadmap()
    open_issues = get_open_issues()

    todo_count  = sum(1 for t in tasks if not t["done"])
    done_count  = sum(1 for t in tasks if t["done"])
    print(f"   Found {todo_count} open tasks, {done_count} completed tasks in ROADMAP.md")
    print(f"   Found {len(open_issues)} open issues on GitHub\n")

    milestone_cache: dict[str, str] = {}

    for task in tasks:
        title     = task["title"]
        done      = task["done"]
        raw_label = task["label"]
        raw_ms    = task["milestone"]

        # Resolve milestone number
        ms_num = None
        if raw_ms:
            if raw_ms not in milestone_cache:
                ms_num = ensure_milestone(raw_ms)
                if ms_num:
                    milestone_cache[raw_ms] = ms_num
            ms_num = milestone_cache.get(raw_ms)

        if done:
            # Close open issue if one exists
            if title in open_issues:
                close_issue(open_issues[title], title)
            else:
                print(f"  ⏭️  Already closed/absent: {title}")
        else:
            # Create open issue if it doesn't exist yet
            if title in open_issues:
                print(f"  ⏭️  Already open: #{open_issues[title]} — {title}")
                continue

            # Also skip if already closed (don't reopen)
            if issue_exists_closed(title):
                print(f"  ⏭️  Exists closed: {title}")
                continue

            # Build labels list
            labels = [ROADMAP_LABEL]
            if raw_label:
                ensure_label(raw_label)
                labels.append(raw_label)

            # Build issue body
            body_lines = [
                "<!-- auto-generated by sync-roadmap workflow -->",
                f"> 📋 This issue was auto-generated from [`docs/ROADMAP.md`](../blob/dev/docs/ROADMAP.md). "
                "Edit the file and push to `dev` to update.",
                "",
            ]
            if task["description"]:
                body_lines += [f"## Description", "", task["description"], ""]
            if raw_ms:
                body_lines += [f"**Milestone:** `{raw_ms}`"]

            create_issue(title, "\n".join(body_lines), labels, ms_num)

    print("\n✅ Roadmap sync complete.")


if __name__ == "__main__":
    main()
