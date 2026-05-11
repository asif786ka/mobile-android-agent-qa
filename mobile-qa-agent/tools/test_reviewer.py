"""Builds the prompt, calls the provider, parses the response."""
from __future__ import annotations

import json
import os
import re
from pathlib import Path
from typing import Any

from providers.base import LLMProvider


_PROMPT_DIR = Path(__file__).resolve().parent.parent / "prompts"


def _load_prompt(platform: str) -> str:
    name = {
        "android": "android_prompt.md",
        "ios": "ios_prompt.md",
    }.get(platform, "android_prompt.md")
    return (_PROMPT_DIR / name).read_text(encoding="utf-8")


def _truncate(text: str, max_chars: int = 40_000) -> str:
    """Avoid blowing past context limits on huge PRs."""
    if len(text) <= max_chars:
        return text
    head = text[: max_chars - 200]
    return head + "\n\n... [diff truncated for length] ...\n"


def _extract_json(raw: str) -> dict[str, Any]:
    """Models occasionally wrap JSON in code fences or prose. Be forgiving."""
    raw = raw.strip()
    # Strip ```json ... ``` if present
    fence = re.search(r"```(?:json)?\s*(\{.*?\})\s*```", raw, re.DOTALL)
    candidate = fence.group(1) if fence else raw
    # Find first {...} block
    if not candidate.startswith("{"):
        m = re.search(r"\{.*\}", candidate, re.DOTALL)
        if m:
            candidate = m.group(0)
    try:
        return json.loads(candidate)
    except json.JSONDecodeError:
        return {"_raw": raw, "_parse_error": True}


def review(provider: LLMProvider, platform: str, detection: dict, diff: str) -> dict:
    prompt_template = _load_prompt(platform)
    diff = _truncate(diff)

    prompt = (
        f"{prompt_template}\n\n"
        f"## Detection summary\n"
        f"```json\n{json.dumps(detection, indent=2)}\n```\n\n"
        f"## PR diff\n"
        f"```diff\n{diff}\n```\n"
    )

    raw = provider.generate(prompt, max_tokens=int(os.environ.get("MAX_TOKENS", "2048")))
    parsed = _extract_json(raw)
    parsed.setdefault("provider", provider.name)
    parsed.setdefault("platform", platform)
    return parsed


def format_comment(result: dict) -> str:
    """Render the JSON result into a Markdown comment body."""
    if result.get("_parse_error"):
        return (
            "### 🤖 Mobile QA Agent\n\n"
            "Could not parse a structured response. Raw model output:\n\n"
            "```\n" + (result.get("_raw") or "")[:4000] + "\n```"
        )

    lines: list[str] = [f"### 🤖 Mobile QA Agent ({result.get('platform', '?')})"]
    summary = result.get("summary")
    if summary:
        lines.append("")
        lines.append(summary)

    sections = [
        ("missing_unit_tests",   "Missing unit tests"),
        ("missing_ui_tests",     "Missing UI tests"),
        ("weak_assertions",      "Weak assertions"),
        ("flaky_selectors",      "Flaky selectors"),
        ("missing_accessibility","Missing accessibility checks"),
        ("missing_negative_cases","Missing negative test cases"),
    ]
    for key, title in sections:
        items = result.get(key) or []
        if items:
            lines.append(f"\n**{title}**")
            for item in items:
                if isinstance(item, dict):
                    where = item.get("file") or item.get("location") or "?"
                    note = item.get("note") or item.get("description") or ""
                    lines.append(f"- `{where}` — {note}")
                else:
                    lines.append(f"- {item}")

    suggestions = result.get("suggested_tests") or []
    if suggestions:
        lines.append("\n**Suggested tests**")
        for s in suggestions:
            if isinstance(s, dict):
                lines.append(f"- {s.get('name', '?')}: {s.get('description', '')}")
            else:
                lines.append(f"- {s}")

    lines.append(f"\n_Provider: `{result.get('provider', '?')}`_")
    return "\n".join(lines)
