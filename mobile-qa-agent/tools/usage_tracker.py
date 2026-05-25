"""Per-call LLM usage + cost tracking.

What this does
--------------
Every reviewer/generator call records one line to
`mobile-qa-agent/artifacts/usage.jsonl`. After a run, `main.py` and
`generate_tests.py` regenerate two artifacts from that log:

* `mobile-qa-agent/USAGE.md` — month-to-date rollup committed to the repo
  alongside generated tests, so the running cost is visible on the README /
  repo browser without leaving GitHub.
* `mobile-qa-agent/artifacts/run_cost.json` — only the records produced
  during *this* workflow run, so the workflow can render a "this PR cost
  $X.XX" line in the bot's PR comment.

Pricing table
-------------
`PRICING_USD_PER_M` maps model name → (input rate, output rate) in USD per
**1 million tokens**. Keep this list short and explicit — anything not in
the table contributes $0 to the rollup but still appears in the JSONL with
its raw token counts, so missing models are obvious.

Sources (update as needed):
  - https://openai.com/api/pricing/
  - https://www.anthropic.com/pricing
  - https://aws.amazon.com/bedrock/pricing/
"""
from __future__ import annotations

import json
import os
import time
from collections import defaultdict
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Iterable

# USD per 1M tokens — (input, output). Update when providers change prices.
PRICING_USD_PER_M: dict[str, tuple[float, float]] = {
    # OpenAI
    "gpt-4o-mini":              (0.15,  0.60),
    "gpt-4o":                   (2.50, 10.00),
    "gpt-4.1-mini":             (0.40,  1.60),
    "gpt-4.1":                  (3.00, 12.00),
    "o4-mini":                  (1.10,  4.40),

    # Anthropic (direct API)
    "claude-opus-4-6":          (15.00, 75.00),
    "claude-sonnet-4-6":        (3.00, 15.00),
    "claude-haiku-4-5":         (1.00,  5.00),
    "claude-3-5-sonnet":        (3.00, 15.00),

    # Bedrock — use the modelId prefix; _lookup_rates does a prefix match
    "us.amazon.nova-pro-v1:0":  (0.80,  3.20),
    "us.amazon.nova-lite-v1:0": (0.06,  0.24),
    "us.anthropic.claude-sonnet-4-5": (3.00, 15.00),
}

# ---------------------------------------------------------------------------
# Pricing
# ---------------------------------------------------------------------------


def _lookup_rates(model: str) -> tuple[float, float] | None:
    """Exact match first, then prefix match (so dated model IDs still resolve)."""
    if not model:
        return None
    if model in PRICING_USD_PER_M:
        return PRICING_USD_PER_M[model]
    for key, rates in PRICING_USD_PER_M.items():
        if model.startswith(key):
            return rates
    return None


def compute_cost(model: str, input_tokens: int, output_tokens: int) -> float:
    """Return USD cost for one call. Unknown models return 0.0."""
    rates = _lookup_rates(model)
    if not rates:
        return 0.0
    in_rate, out_rate = rates
    return (input_tokens / 1_000_000) * in_rate + (output_tokens / 1_000_000) * out_rate


# ---------------------------------------------------------------------------
# Recording
# ---------------------------------------------------------------------------


def default_log_path() -> Path:
    return Path(__file__).resolve().parent.parent / "artifacts" / "usage.jsonl"


def default_run_cost_path() -> Path:
    return Path(__file__).resolve().parent.parent / "artifacts" / "run_cost.json"


def default_usage_md_path() -> Path:
    return Path(__file__).resolve().parent.parent / "USAGE.md"


def _pr_number_from_event() -> int | None:
    event_path = os.environ.get("GITHUB_EVENT_PATH")
    if not event_path or not os.path.isfile(event_path):
        return None
    try:
        with open(event_path, encoding="utf-8") as f:
            ev = json.load(f)
    except (OSError, json.JSONDecodeError):
        return None
    pr = ev.get("pull_request") or {}
    if pr.get("number"):
        return pr["number"]
    issue = ev.get("issue") or {}
    if issue.get("pull_request") and issue.get("number"):
        return issue["number"]
    return None


def record_usage(
    *,
    provider: str,
    model: str,
    input_tokens: int,
    output_tokens: int,
    role: str = "reviewer",
    log_path: Path | None = None,
    extra: dict[str, Any] | None = None,
) -> dict[str, Any]:
    """Append one usage record to the JSONL log. Never raises — failures here
    must not break the actual review/generation pipeline."""
    try:
        path = log_path or default_log_path()
        cost = compute_cost(model, input_tokens or 0, output_tokens or 0)
        record: dict[str, Any] = {
            "ts": int(time.time()),
            "iso": datetime.now(timezone.utc).isoformat(timespec="seconds"),
            "provider": provider,
            "model": model,
            "role": role,
            "input_tokens": int(input_tokens or 0),
            "output_tokens": int(output_tokens or 0),
            "cost_usd": round(cost, 6),
            "github_run_id": os.environ.get("GITHUB_RUN_ID"),
            "github_repo": os.environ.get("GITHUB_REPOSITORY"),
            "github_pr": _pr_number_from_event(),
        }
        if extra:
            record.update(extra)
        path.parent.mkdir(parents=True, exist_ok=True)
        with path.open("a", encoding="utf-8") as f:
            f.write(json.dumps(record, sort_keys=True) + "\n")
        return record
    except Exception as exc:  # noqa: BLE001 — usage tracking must never bubble
        # Best-effort log to stderr; never raise.
        try:
            import sys
            print(f"[usage_tracker] failed to record: {exc}", file=sys.stderr)
        except Exception:
            pass
        return {}


# ---------------------------------------------------------------------------
# Summarising
# ---------------------------------------------------------------------------


def _empty_bucket() -> dict[str, float]:
    return {"calls": 0, "input_tokens": 0, "output_tokens": 0, "cost_usd": 0.0}


def _add(bucket: dict[str, float], record: dict[str, Any]) -> None:
    bucket["calls"] += 1
    bucket["input_tokens"] += record.get("input_tokens", 0)
    bucket["output_tokens"] += record.get("output_tokens", 0)
    bucket["cost_usd"] += record.get("cost_usd", 0.0)


def _read_records(path: Path) -> Iterable[dict[str, Any]]:
    if not path.exists():
        return []
    out = []
    with path.open(encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            try:
                out.append(json.loads(line))
            except json.JSONDecodeError:
                continue
    return out


def summarize(
    log_path: Path | None = None,
    *,
    only_run_id: str | None = None,
    month: str | None = None,
) -> dict[str, Any]:
    """Aggregate usage records.

    Filters (applied in order):
      * only_run_id — keep only records whose GITHUB_RUN_ID equals this.
      * month       — keep only records whose iso timestamp starts with this
                      prefix (e.g. "2026-05" for month-to-date).
    """
    path = log_path or default_log_path()
    summary: dict[str, Any] = {
        "total": _empty_bucket(),
        "by_provider": defaultdict(_empty_bucket),
        "by_model": defaultdict(_empty_bucket),
        "by_role": defaultdict(_empty_bucket),
        "by_pr": defaultdict(_empty_bucket),
    }
    for r in _read_records(path):
        if only_run_id and str(r.get("github_run_id")) != str(only_run_id):
            continue
        if month and not str(r.get("iso", "")).startswith(month):
            continue
        _add(summary["total"], r)
        _add(summary["by_provider"][str(r.get("provider") or "unknown")], r)
        _add(summary["by_model"][str(r.get("model") or "unknown")], r)
        _add(summary["by_role"][str(r.get("role") or "unknown")], r)
        pr = r.get("github_pr")
        if pr is not None:
            _add(summary["by_pr"][str(pr)], r)

    # Convert defaultdicts to plain dicts and round costs for cleaner JSON.
    def _finalize(bucket: dict[str, Any]) -> dict[str, Any]:
        bucket["cost_usd"] = round(bucket["cost_usd"], 6)
        return bucket

    out = {
        "total": _finalize(dict(summary["total"])),
        "by_provider": {k: _finalize(dict(v)) for k, v in summary["by_provider"].items()},
        "by_model": {k: _finalize(dict(v)) for k, v in summary["by_model"].items()},
        "by_role": {k: _finalize(dict(v)) for k, v in summary["by_role"].items()},
        "by_pr": {k: _finalize(dict(v)) for k, v in summary["by_pr"].items()},
    }
    return out


# ---------------------------------------------------------------------------
# Rendering
# ---------------------------------------------------------------------------


def _row(name: str, b: dict[str, Any]) -> str:
    return (
        f"| {name} | {int(b['calls'])} | "
        f"{int(b['input_tokens']):,} | {int(b['output_tokens']):,} | "
        f"${b['cost_usd']:.4f} |"
    )


def render_usage_md(
    log_path: Path | None = None,
    *,
    month: str | None = None,
) -> str:
    """Render the month-to-date usage table as Markdown."""
    month = month or datetime.now(timezone.utc).strftime("%Y-%m")
    summary = summarize(log_path, month=month)
    total = summary["total"]

    lines: list[str] = [
        "# Bot LLM usage",
        "",
        f"_Month: **{month}** · auto-updated by the AI Mobile QA Review workflow._",
        "",
        f"**Total calls:** {int(total['calls'])}  ",
        f"**Total input tokens:** {int(total['input_tokens']):,}  ",
        f"**Total output tokens:** {int(total['output_tokens']):,}  ",
        f"**Estimated cost (USD):** ${total['cost_usd']:.4f}",
        "",
        "_Estimates use the static `PRICING_USD_PER_M` table in_",
        "_`mobile-qa-agent/tools/usage_tracker.py`. Update prices there as they change._",
        "",
        "## By provider",
        "",
        "| Provider | Calls | Input tokens | Output tokens | Cost (USD) |",
        "|---|---:|---:|---:|---:|",
    ]
    for p, b in sorted(summary["by_provider"].items()):
        lines.append(_row(p, b))

    lines += [
        "",
        "## By model",
        "",
        "| Model | Calls | Input tokens | Output tokens | Cost (USD) |",
        "|---|---:|---:|---:|---:|",
    ]
    for m, b in sorted(summary["by_model"].items()):
        lines.append(_row(m, b))

    lines += [
        "",
        "## By role",
        "",
        "| Role | Calls | Input tokens | Output tokens | Cost (USD) |",
        "|---|---:|---:|---:|---:|",
    ]
    for r, b in sorted(summary["by_role"].items()):
        lines.append(_row(r, b))

    top_prs = sorted(
        summary["by_pr"].items(),
        key=lambda kv: kv[1]["cost_usd"],
        reverse=True,
    )[:10]
    if top_prs:
        lines += [
            "",
            "## Top PRs by cost (this month)",
            "",
            "| PR | Calls | Input tokens | Output tokens | Cost (USD) |",
            "|---|---:|---:|---:|---:|",
        ]
        for pr, b in top_prs:
            lines.append(_row(f"#{pr}", b))

    return "\n".join(lines) + "\n"


def write_usage_md(
    out_path: Path | None = None,
    *,
    log_path: Path | None = None,
    month: str | None = None,
) -> Path:
    out = out_path or default_usage_md_path()
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(render_usage_md(log_path, month=month), encoding="utf-8")
    return out


def write_run_cost(
    *,
    log_path: Path | None = None,
    run_cost_path: Path | None = None,
    run_id: str | None = None,
) -> Path:
    """Write the cost summary for *this* workflow run to a small JSON file
    that the workflow's PR-comment step can read."""
    run_id = run_id or os.environ.get("GITHUB_RUN_ID")
    summary = summarize(log_path, only_run_id=run_id) if run_id else summarize(log_path)
    out = run_cost_path or default_run_cost_path()
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(
        json.dumps({"run_id": run_id, **summary}, indent=2, sort_keys=True),
        encoding="utf-8",
    )
    return out
