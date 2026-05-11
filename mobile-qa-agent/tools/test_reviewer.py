"""Builds the prompt, calls the provider, parses the response."""
from __future__ import annotations

import json
import os
import re
import sys
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
    # Find first {...} block (greedy: matches outer braces)
    if not candidate.startswith("{"):
        m = re.search(r"\{.*\}", candidate, re.DOTALL)
        if m:
            candidate = m.group(0)
    try:
        return json.loads(candidate)
    except json.JSONDecodeError as err:
        # Visibility: dump the raw response to stderr so CI logs show what
        # actually came back. Truncate to 1500 chars so we don't spam logs
        # on huge responses.
        print(
            f"[reviewer] JSON parse failed ({err}). "
            f"Raw response (first 1500 chars):\n{raw[:1500]}",
            file=sys.stderr,
            flush=True,
        )
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

    # 4096 is enough for a thorough reviewer response on a multi-file diff.
    # Override with MAX_TOKENS env var if you hit truncation on huge PRs.
    raw = provider.generate(prompt, max_tokens=int(os.environ.get("MAX_TOKENS", "4096")))
    parsed = _extract_json(raw)
    parsed.setdefault("provider", provider.name)
    parsed.setdefault("platform", platform)
    return parsed


# Marker used by the test generator to find the agent's structured findings
# inside a PR comment. Format:
#   <!-- AI-QA-FINDINGS-JSON: {...json...} -->
FINDINGS_MARKER = "AI-QA-FINDINGS-JSON"


def format_comment(result: dict) -> str:
    """Render the JSON result into a Markdown comment body.

    Also appends a hidden HTML comment containing the raw findings JSON so
    downstream tools (like the OpenAI test generator) can recover the
    structured data from the PR thread.
    """
    if result.get("_parse_error"):
        raw = result.get("_raw") or ""
        truncated_note = (
            "\n\n⚠️ Response appears truncated (no closing `}`). "
            "Try bumping `MAX_TOKENS` (currently defaulting to 4096)."
            if "{" in raw and "}" not in raw
            else ""
        )
        return (
            "### 🤖 Mobile QA Agent\n\n"
            "Could not parse a structured response — the reviewer "
            f"will need re-running.{truncated_note}\n\n"
            "<details><summary>Raw model output (first 4000 chars)</summary>\n\n"
            "```\n" + raw[:4000] + "\n```\n\n"
            "</details>"
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
    lines.append(
        "\n_Tip: comment `/generate-tests` on this PR to auto-generate "
        "missing tests via OpenAI._"
    )

    # Hidden machine-readable copy of the findings for downstream tools.
    findings_json = json.dumps(result, separators=(",", ":"))
    lines.append(f"\n<!-- {FINDINGS_MARKER}: {findings_json} -->")

    return "\n".join(lines)


def extract_findings_from_comment(comment_body: str) -> dict | None:
    """Recover the structured findings JSON embedded by format_comment().

    Returns None if no marker is found.
    """
    pattern = rf"<!--\s*{re.escape(FINDINGS_MARKER)}:\s*(\{{.*?\}})\s*-->"
    m = re.search(pattern, comment_body, re.DOTALL)
    if not m:
        return None
    try:
        return json.loads(m.group(1))
    except json.JSONDecodeError:
        return None
