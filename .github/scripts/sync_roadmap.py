#!/usr/bin/env python3
"""Synchronize docs/ROADMAP.md with owned GitHub issues and a Project v2."""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass
from pathlib import Path

ROADMAP_FILE = Path("docs/ROADMAP.md")
ROADMAP_LABEL = "roadmap"
ROADMAP_LABEL_COLOR = "0075ca"
ROADMAP_LABEL_DESCRIPTION = "Managed from docs/ROADMAP.md"
MARKER_TEMPLATE = "<!-- roadmap-sync:id={task_id} -->"

HEADER_RE = re.compile(r"^##\s+(.+?)\s*$")
META_RE = re.compile(r"\[(label|milestone):([^\]]+)]")
TASK_RE = re.compile(r"^- \[([ xX])]\s+(.+?)\s*$")
ID_RE = re.compile(
    r"\s*<!--\s*roadmap-id:\s*([a-z0-9][a-z0-9_-]*)\s*-->\s*$"
)
MARKER_RE = re.compile(r"<!--\s*roadmap-sync:id=([a-z0-9][a-z0-9_-]*)\s*-->")
DESCRIPTION_RE = re.compile(r"\s+(?:\u2014|\u2013|--)\s+", re.UNICODE)


@dataclass(frozen=True)
class Task:
    task_id: str
    title: str
    description: str
    label: str | None
    milestone: str | None
    done: bool


def parse_roadmap(path: Path = ROADMAP_FILE) -> list[Task]:
    tasks: list[Task] = []
    seen_ids: set[str] = set()
    current_label: str | None = None
    current_milestone: str | None = None

    for line_number, raw_line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        line = raw_line.rstrip()
        header = HEADER_RE.match(line)
        if header:
            metadata = {key: value.strip() for key, value in META_RE.findall(header.group(1))}
            current_label = metadata.get("label") or None
            current_milestone = metadata.get("milestone") or None
            continue

        task_match = TASK_RE.match(line)
        if not task_match:
            continue

        raw_task = task_match.group(2).strip()
        id_match = ID_RE.search(raw_task)
        if not id_match:
            raise ValueError(
                f"{path}:{line_number}: roadmap task requires "
                "<!-- roadmap-id: stable-id -->"
            )
        task_id = id_match.group(1)
        if task_id in seen_ids:
            raise ValueError(f"{path}:{line_number}: duplicate roadmap-id {task_id!r}")
        seen_ids.add(task_id)

        task_text = ID_RE.sub("", raw_task).strip()
        parts = DESCRIPTION_RE.split(task_text, maxsplit=1)
        title = parts[0].strip()
        if not title:
            raise ValueError(f"{path}:{line_number}: task title cannot be empty")

        tasks.append(
            Task(
                task_id=task_id,
                title=title,
                description=parts[1].strip() if len(parts) == 2 else "",
                label=current_label,
                milestone=current_milestone,
                done=task_match.group(1).lower() == "x",
            )
        )

    if not tasks:
        raise ValueError(f"{path}: no roadmap tasks found")
    return tasks


class GitHubClient:
    def __init__(self, repository: str, token: str):
        if "/" not in repository:
            raise ValueError("GITHUB_REPOSITORY must use owner/repository format")
        self.repository = repository
        self.owner, self.repository_name = repository.split("/", 1)
        self.token = token

    def request(self, method: str, path: str, payload: dict | None = None):
        url = path if path.startswith("https://") else f"https://api.github.com{path}"
        data = json.dumps(payload).encode() if payload is not None else None
        request = urllib.request.Request(
            url,
            data=data,
            method=method,
            headers={
                "Accept": "application/vnd.github+json",
                "Authorization": f"Bearer {self.token}",
                "Content-Type": "application/json",
                "User-Agent": "url-shortener-roadmap-sync",
                "X-GitHub-Api-Version": "2022-11-28",
            },
        )
        try:
            with urllib.request.urlopen(request) as response:
                body = response.read()
                return json.loads(body) if body else None
        except urllib.error.HTTPError as error:
            details = error.read().decode(errors="replace")
            raise RuntimeError(f"GitHub API {method} {url} failed: {error.code} {details}") from error

    def paged(self, path: str) -> list[dict]:
        separator = "&" if "?" in path else "?"
        page = 1
        results: list[dict] = []
        while True:
            batch = self.request("GET", f"{path}{separator}per_page=100&page={page}")
            if not batch:
                return results
            results.extend(batch)
            if len(batch) < 100:
                return results
            page += 1

    def graphql(self, query: str, variables: dict):
        result = self.request(
            "POST",
            "https://api.github.com/graphql",
            {"query": query, "variables": variables},
        )
        if result.get("errors"):
            raise RuntimeError(f"GitHub GraphQL failed: {json.dumps(result['errors'])}")
        return result["data"]


def issue_marker(task_id: str) -> str:
    return MARKER_TEMPLATE.format(task_id=task_id)


def issue_body(task: Task) -> str:
    lines = [
        issue_marker(task.task_id),
        "> Managed from [`docs/ROADMAP.md`](../blob/dev/docs/ROADMAP.md).",
        "",
    ]
    if task.description:
        lines.extend(["## Description", "", task.description, ""])
    if task.milestone:
        lines.append(f"**Roadmap milestone:** `{task.milestone}`")
    return "\n".join(lines).rstrip() + "\n"


def owned_issue_id(body: str | None) -> str | None:
    match = MARKER_RE.search(body or "")
    return match.group(1) if match else None


def ensure_labels(client: GitHubClient, tasks: list[Task]) -> None:
    labels = {label["name"] for label in client.paged(f"/repos/{client.repository}/labels")}
    wanted = {
        ROADMAP_LABEL: (ROADMAP_LABEL_COLOR, ROADMAP_LABEL_DESCRIPTION),
        **{task.label: ("ededed", "Roadmap phase") for task in tasks if task.label},
    }
    for name, (color, description) in wanted.items():
        if name not in labels:
            client.request(
                "POST",
                f"/repos/{client.repository}/labels",
                {"name": name, "color": color, "description": description},
            )
            print(f"Created label: {name}")


def ensure_milestones(client: GitHubClient, tasks: list[Task]) -> dict[str, int]:
    milestones = {
        milestone["title"]: milestone["number"]
        for milestone in client.paged(f"/repos/{client.repository}/milestones?state=all")
    }
    for title in sorted({task.milestone for task in tasks if task.milestone}):
        if title not in milestones:
            created = client.request(
                "POST", f"/repos/{client.repository}/milestones", {"title": title}
            )
            milestones[title] = created["number"]
            print(f"Created milestone: {title}")
    return milestones


def get_owned_issues(client: GitHubClient) -> dict[str, dict]:
    issues = client.paged(
        f"/repos/{client.repository}/issues?state=all&labels={urllib.parse.quote(ROADMAP_LABEL)}"
    )
    owned: dict[str, dict] = {}
    for issue in issues:
        if "pull_request" in issue:
            continue
        task_id = owned_issue_id(issue.get("body"))
        if not task_id:
            continue
        if task_id in owned:
            raise RuntimeError(f"Multiple roadmap issues use roadmap-id {task_id!r}")
        owned[task_id] = issue
    return owned


def sync_issues(
    client: GitHubClient, tasks: list[Task], milestones: dict[str, int]
) -> dict[str, dict]:
    owned = get_owned_issues(client)
    synced: dict[str, dict] = {}

    for task in tasks:
        issue = owned.get(task.task_id)
        if task.done and not issue:
            print(f"Completed without managed issue: {task.task_id}")
            continue

        labels = [ROADMAP_LABEL] + ([task.label] if task.label else [])
        payload = {
            "title": task.title,
            "body": issue_body(task),
            "labels": labels,
            "milestone": milestones.get(task.milestone) if task.milestone else None,
        }

        if not issue:
            issue = client.request("POST", f"/repos/{client.repository}/issues", payload)
            print(f"Created issue #{issue['number']}: {task.title}")
        else:
            desired_state = "closed" if task.done else "open"
            payload["state"] = desired_state
            if task.done:
                payload["state_reason"] = "completed"
            issue = client.request(
                "PATCH", f"/repos/{client.repository}/issues/{issue['number']}", payload
            )
            print(f"Updated issue #{issue['number']}: {task.title}")

        if task.done and issue["state"] != "closed":
            issue = client.request(
                "PATCH",
                f"/repos/{client.repository}/issues/{issue['number']}",
                {"state": "closed", "state_reason": "completed"},
            )
        synced[task.task_id] = issue
    return synced


def get_or_create_project(client: GitHubClient, title: str) -> dict:
    query = """
    query($owner: String!, $name: String!) {
      repository(owner: $owner, name: $name) {
        id
        owner {
          id
          login
          ... on User { projectsV2(first: 100) { nodes { id number title url } } }
          ... on Organization { projectsV2(first: 100) { nodes { id number title url } } }
        }
      }
    }
    """
    data = client.graphql(
        query, {"owner": client.owner, "name": client.repository_name}
    )["repository"]
    projects = data["owner"]["projectsV2"]["nodes"]
    existing = next((project for project in projects if project["title"] == title), None)
    if existing:
        return existing

    mutation = """
    mutation($ownerId: ID!, $repositoryId: ID!, $title: String!) {
      createProjectV2(input: {
        ownerId: $ownerId,
        repositoryId: $repositoryId,
        title: $title
      }) { projectV2 { id number title url } }
    }
    """
    project = client.graphql(
        mutation,
        {"ownerId": data["owner"]["id"], "repositoryId": data["id"], "title": title},
    )["createProjectV2"]["projectV2"]
    print(f"Created project: {project['url']}")
    return project


def project_data(client: GitHubClient, project_id: str) -> tuple[dict[str, str], dict]:
    query = """
    query($projectId: ID!, $cursor: String) {
      node(id: $projectId) {
        ... on ProjectV2 {
          fields(first: 50) {
            nodes {
              ... on ProjectV2SingleSelectField {
                id
                name
                options { id name }
              }
            }
          }
          items(first: 100, after: $cursor) {
            nodes { id content { ... on Issue { id } } }
            pageInfo { hasNextPage endCursor }
          }
        }
      }
    }
    """
    items: dict[str, str] = {}
    fields: dict = {}
    cursor = None
    while True:
        node = client.graphql(query, {"projectId": project_id, "cursor": cursor})["node"]
        if not fields:
            fields = {
                field["name"]: field
                for field in node["fields"]["nodes"]
                if field and field.get("name")
            }
        for item in node["items"]["nodes"]:
            if item.get("content"):
                items[item["content"]["id"]] = item["id"]
        page_info = node["items"]["pageInfo"]
        if not page_info["hasNextPage"]:
            return items, fields
        cursor = page_info["endCursor"]


def sync_project(
    client: GitHubClient, tasks: list[Task], issues: dict[str, dict], title: str
) -> None:
    project = get_or_create_project(client, title)
    items, fields = project_data(client, project["id"])
    status = fields.get("Status")
    status_options = {
        option["name"].lower(): option["id"] for option in status.get("options", [])
    } if status else {}

    add_mutation = """
    mutation($projectId: ID!, $contentId: ID!) {
      addProjectV2ItemById(input: {projectId: $projectId, contentId: $contentId}) {
        item { id }
      }
    }
    """
    update_mutation = """
    mutation($projectId: ID!, $itemId: ID!, $fieldId: ID!, $optionId: String!) {
      updateProjectV2ItemFieldValue(input: {
        projectId: $projectId,
        itemId: $itemId,
        fieldId: $fieldId,
        value: {singleSelectOptionId: $optionId}
      }) { projectV2Item { id } }
    }
    """

    for task in tasks:
        issue = issues.get(task.task_id)
        if not issue:
            continue
        issue_node_id = issue["node_id"]
        item_id = items.get(issue_node_id)
        if not item_id:
            item_id = client.graphql(
                add_mutation,
                {"projectId": project["id"], "contentId": issue_node_id},
            )["addProjectV2ItemById"]["item"]["id"]
            print(f"Added issue #{issue['number']} to project")

        desired = "done" if task.done else "todo"
        option_id = status_options.get(desired)
        if status and option_id:
            client.graphql(
                update_mutation,
                {
                    "projectId": project["id"],
                    "itemId": item_id,
                    "fieldId": status["id"],
                    "optionId": option_id,
                },
            )

    print(f"Project synchronized: {project['url']}")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="validate Markdown only")
    args = parser.parse_args()

    try:
        tasks = parse_roadmap()
        print(f"Validated {len(tasks)} roadmap tasks with unique stable IDs")
        if args.check:
            return 0

        repository = os.environ.get("GITHUB_REPOSITORY", "")
        github_token = os.environ.get("GITHUB_TOKEN", "")
        if not repository or not github_token:
            raise ValueError("GITHUB_REPOSITORY and GITHUB_TOKEN are required")

        issue_client = GitHubClient(repository, github_token)
        ensure_labels(issue_client, tasks)
        milestones = ensure_milestones(issue_client, tasks)
        issues = sync_issues(issue_client, tasks, milestones)

        project_token = os.environ.get("PROJECT_TOKEN", "").strip()
        if project_token:
            project_title = os.environ.get(
                "ROADMAP_PROJECT_TITLE", "URL Shortener Roadmap"
            )
            sync_project(GitHubClient(repository, project_token), tasks, issues, project_title)
        else:
            print("PROJECT_TOKEN is not configured; issue sync completed, project sync skipped")
        return 0
    except (OSError, ValueError, RuntimeError) as error:
        print(f"ERROR: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
