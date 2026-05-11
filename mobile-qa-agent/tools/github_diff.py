"""GitHub interactions: fetch PR diff, post review comment.

Designed to work two ways:
1. Inside GitHub Actions, where GITHUB_EVENT_PATH points at the PR event.
2. Locally, with a saved diff file via `read_local_diff`.
"""
from __future__ import annotations

import json
import os
from dataclasses import dataclass
from typing import Iterable

import requests


GITHUB_API = "https://api.github.com"


@dataclass
class PRContext:
    repo: str          # "owner/name"
    number: int
    head_sha: str
    diff: str
    changed_files: list[str]


def _headers(token: str) -> dict[str, str]:
    return {
        "Authorization": f"Bearer {token}",
        "Accept": "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
    }


def load_event() -> dict:
    """Read the GitHub event payload from $GITHUB_EVENT_PATH (Actions)."""
    path = os.environ.get("GITHUB_EVENT_PATH")
    if not path or not os.path.isfile(path):
        raise RuntimeError(
            "GITHUB_EVENT_PATH is not set or missing — not running inside Actions?"
        )
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def _resolve_pr_from_env(token: str) -> tuple[str, int, str]:
    """Figure out (repo, pr_number, head_sha) from the current CI environment.

    Supports:
      - GitHub Actions  ($GITHUB_EVENT_PATH points at a pull_request event)
      - CircleCI        ($CIRCLE_PULL_REQUEST = https://github.com/owner/repo/pull/N)
    """
    # GitHub Actions
    if os.environ.get("GITHUB_EVENT_PATH"):
        event = load_event()
        pr = event.get("pull_request") or {}
        if pr:
            return (
                event["repository"]["full_name"],
                pr["number"],
                pr["head"]["sha"],
            )

    # CircleCI
    pr_url = os.environ.get("CIRCLE_PULL_REQUEST", "").strip()
    if pr_url:
        # e.g. https://github.com/owner/repo/pull/123
        parts = pr_url.rstrip("/").split("/")
        owner, repo_name, _, number_str = parts[-4], parts[-3], parts[-2], parts[-1]
        repo = f"{owner}/{repo_name}"
        head_sha = os.environ.get("CIRCLE_SHA1", "")
        if not head_sha:
            # Fetch via API as a fallback
            r = requests.get(
                f"{GITHUB_API}/repos/{repo}/pulls/{number_str}",
                headers=_headers(token),
                timeout=30,
            )
            r.raise_for_status()
            head_sha = r.json()["head"]["sha"]
        return repo, int(number_str), head_sha

    raise RuntimeError(
        "No PR context found. Expected GitHub Actions ($GITHUB_EVENT_PATH) "
        "or CircleCI ($CIRCLE_PULL_REQUEST) environment."
    )


def fetch_pr_context(event: dict | None = None, token: str | None = None) -> PRContext:
    """Pull the PR diff + changed files from GitHub for the current event.

    Works under both GitHub Actions and CircleCI.
    """
    token = token or os.environ.get("GITHUB_TOKEN")
    if not token:
        raise RuntimeError("GITHUB_TOKEN is required to fetch PR data.")

    if event is not None:
        pr = event.get("pull_request") or {}
        if not pr:
            raise RuntimeError("Event payload has no pull_request — not a PR event?")
        repo = event["repository"]["full_name"]
        number = pr["number"]
        head_sha = pr["head"]["sha"]
    else:
        repo, number, head_sha = _resolve_pr_from_env(token)

    # Diff
    diff_resp = requests.get(
        f"{GITHUB_API}/repos/{repo}/pulls/{number}",
        headers={**_headers(token), "Accept": "application/vnd.github.v3.diff"},
        timeout=30,
    )
    diff_resp.raise_for_status()
    diff_text = diff_resp.text

    # Changed files (paginated, but Hello World PRs won't exceed 30)
    files_resp = requests.get(
        f"{GITHUB_API}/repos/{repo}/pulls/{number}/files?per_page=100",
        headers=_headers(token),
        timeout=30,
    )
    files_resp.raise_for_status()
    changed_files = [f["filename"] for f in files_resp.json()]

    return PRContext(
        repo=repo,
        number=number,
        head_sha=head_sha,
        diff=diff_text,
        changed_files=changed_files,
    )


def read_local_diff(diff_file: str, changed_files: Iterable[str] | None = None) -> PRContext:
    """Build a PRContext from a local diff (for dry-run / offline testing)."""
    with open(diff_file, "r", encoding="utf-8") as f:
        diff_text = f.read()

    if changed_files is None:
        # Naive parse: grab paths from `diff --git a/<path> b/<path>` lines.
        files: list[str] = []
        for line in diff_text.splitlines():
            if line.startswith("diff --git "):
                parts = line.split()
                if len(parts) >= 4:
                    # 'b/<path>' → '<path>'
                    files.append(parts[3][2:])
        changed_files = files

    return PRContext(
        repo="local/dry-run",
        number=0,
        head_sha="LOCAL",
        diff=diff_text,
        changed_files=list(changed_files),
    )


def post_review_comment(ctx: PRContext, body: str, token: str | None = None) -> None:
    """Post a single review comment summarizing the agent's findings."""
    token = token or os.environ.get("GITHUB_TOKEN")
    if not token:
        raise RuntimeError("GITHUB_TOKEN is required to post a review.")

    url = f"{GITHUB_API}/repos/{ctx.repo}/issues/{ctx.number}/comments"
    resp = requests.post(
        url,
        headers=_headers(token),
        json={"body": body},
        timeout=30,
    )
    resp.raise_for_status()
