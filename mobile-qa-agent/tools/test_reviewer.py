"""Builds the prompt, calls the provider, parses the response.

Parse-failure handling (per the LangSmith Engine alert
"Review JSON parse failures silently posted to PR as fallback comment"):

1. Default `max_tokens` is now 8192 — the previous 4096 cap was truncating
   review JSON mid-string on large PRs. Override via `MAX_TOKENS` env var.
2. If the raw response still can't be parsed, we try `json_repair` to fix
   common model bugs (unescaped quotes inside strings, trailing commas,
   slightly-truncated tails). If that succeeds we keep going.
3. If repair fails, we do ONE retry LLM call with the broken JSON and the
   parser error message, asking the model to return valid JSON only.
4. Only if all three attempts fail do we set `_parse_error=true`. The
   downstream post node refuses to publish the "Could not parse" fallback
   comment in that case — it logs the failure and lets CI fail loud
   instead of littering the PR with useless review comments.
"""
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
    """Models occasionally wrap JSON in code fences or prose. Be forgiving.

    On parse failure, the returned dict carries `_parse_error=True` and
    `_parse_error_msg` so the caller can decide whether to attempt repair
    or retry.
    """
    raw_stripped = raw.strip()
    # Strip ```json ... ``` if present
    fence = re.search(r"```(?:json)?\s*(\{.*?\})\s*```", raw_stripped, re.DOTALL)
    candidate = fence.group(1) if fence else raw_stripped
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
        return {
            "_raw": raw,
            "_parse_error": True,
            "_parse_error_msg": str(err),
        }


def _attempt_repair(raw: str) -> dict[str, Any] | None:
    """Best-effort JSON repair using the `json_repair` package.

    Handles common model bugs:
      - Unescaped `"` inside string values (the failure mode seen in the
        LangSmith Engine alert).
      - Trailing commas.
      - Slightly-truncated tails (json_repair fills in missing `}` / `]`).

    Returns the repaired dict on success, or None if the package isn't
    installed or the repair didn't produce a parseable object.
    """
    try:
        from json_repair import repair_json
    except ImportError:
        print(
            "[reviewer] json_repair not installed; skipping repair pass.",
            file=sys.stderr, flush=True,
        )
        return None
    try:
        # First isolate the candidate (handles markdown fences + prose).
        raw_stripped = raw.strip()
        fence = re.search(r"```(?:json)?\s*(\{.*?\})\s*```", raw_stripped, re.DOTALL)
        candidate = fence.group(1) if fence else raw_stripped
        if not candidate.startswith("{"):
            m = re.search(r"\{.*\}", candidate, re.DOTALL)
            if m:
                candidate = m.group(0)
        repaired = repair_json(candidate, return_objects=False)
        if not repaired:
            return None
        parsed = json.loads(repaired)
        if isinstance(parsed, dict):
            return parsed
    except (json.JSONDecodeError, ValueError) as exc:
        print(f"[reviewer] json_repair failed: {exc}", file=sys.stderr, flush=True)
    return None


def _retry_llm_for_valid_json(
    provider: LLMProvider,
    original_prompt: str,
    broken_raw: str,
    parse_error_msg: str,
    max_tokens: int,
) -> dict[str, Any] | None:
    """One retry LLM call asking the model to fix its own malformed JSON.

    Records its own usage entry under role="reviewer_retry" so the cost
    is attributed correctly in USAGE.md.
    """
    retry_prompt = (
        "You previously returned a response to the prompt below, but the "
        "JSON parser rejected it with the following error:\n\n"
        f"```\n{parse_error_msg}\n```\n\n"
        "Here is the broken response you produced:\n\n"
        f"```\n{broken_raw[:6000]}\n```\n\n"
        "Return ONLY valid JSON matching the schema described in the "
        "ORIGINAL prompt. No prose, no markdown fences. Your first "
        "character MUST be `{` and your last MUST be `}`.\n\n"
        "ORIGINAL PROMPT (for schema reference):\n"
        f"{original_prompt[:8000]}"
    )
    try:
        retry_raw = provider.generate(retry_prompt, max_tokens=max_tokens)
    except Exception as exc:  # noqa: BLE001
        print(f"[reviewer] retry LLM call failed: {exc}", file=sys.stderr, flush=True)
        return None

    # Record usage for the retry call so we don't undercount cost.
    try:
        from tools.usage_tracker import record_usage
        usage = getattr(provider, "last_usage", None) or {}
        if usage:
            record_usage(
                provider=provider.name,
                model=usage.get("model", ""),
                input_tokens=usage.get("input_tokens", 0),
                output_tokens=usage.get("output_tokens", 0),
                role="reviewer_retry",
            )
    except Exception:
        pass

    # Try a plain parse first.
    candidate = retry_raw.strip()
    if candidate.startswith("```"):
        m = re.search(r"\{.*\}", candidate, re.DOTALL)
        if m:
            candidate = m.group(0)
    try:
        parsed = json.loads(candidate)
        if isinstance(parsed, dict):
            return parsed
    except json.JSONDecodeError:
        pass
    # Final repair pass on the retry response too.
    return _attempt_repair(retry_raw)


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

    # 8192 covers thorough review JSON on multi-file PRs. The previous 4096
    # cap was triggering mid-string truncation on the LangGraph traces
    # flagged by the LangSmith Engine alert. Override via MAX_TOKENS env var.
    max_tokens = int(os.environ.get("MAX_TOKENS", "8192"))
    raw = provider.generate(prompt, max_tokens=max_tokens)

    # Attempt 1 — direct parse with our existing regex extraction.
    parsed = _extract_json(raw)
    if not parsed.get("_parse_error"):
        parsed.setdefault("provider", provider.name)
        parsed.setdefault("platform", platform)
        return parsed

    # Attempt 2 — permissive repair of the raw output (handles unescaped
    # quotes, trailing commas, mildly-truncated tails).
    repaired = _attempt_repair(raw)
    if repaired:
        print(
            "[reviewer] recovered from parse failure via json_repair.",
            file=sys.stderr, flush=True,
        )
        repaired.setdefault("provider", provider.name)
        repaired.setdefault("platform", platform)
        repaired["_recovered_via"] = "json_repair"
        return repaired

    # Attempt 3 — one retry LLM call passing the broken JSON + parser error
    # back to the model. This is bounded to a single retry so a stuck model
    # can't burn unbounded tokens.
    retry_parsed = _retry_llm_for_valid_json(
        provider,
        prompt,
        raw,
        parsed.get("_parse_error_msg", "JSON parse failed."),
        max_tokens=max_tokens,
    )
    if retry_parsed:
        print(
            "[reviewer] recovered from parse failure via LLM retry.",
            file=sys.stderr, flush=True,
        )
        retry_parsed.setdefault("provider", provider.name)
        retry_parsed.setdefault("platform", platform)
        retry_parsed["_recovered_via"] = "llm_retry"
        return retry_parsed

    # All three attempts failed — return the broken result. The post node
    # will refuse to publish a fallback comment for this case.
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

    Note: when `result._parse_error` is true, we still render a body for
    local --dry-run inspection, but the post node refuses to publish it to
    the PR.
    """
    if result.get("_parse_error"):
        raw = result.get("_raw") or ""
        return (
            "### 🤖 Mobile QA Agent (parse failure)\n\n"
            "_This message is for local inspection only — the post step "
            "refuses to publish parse-failure comments to the PR. CI will "
            "fail loud instead._\n\n"
            "Parser error: "
            f"`{result.get('_parse_error_msg', 'unknown')}`\n\n"
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
    recovered = result.get("_recovered_via")
    if recovered:
        lines.append(
            f"_Recovered from a parse failure via `{recovered}` — review "
            "may be slightly less precise than usual._"
        )
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
