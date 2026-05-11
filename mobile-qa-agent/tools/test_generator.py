"""OpenAI-powered Kotlin test generator.

Takes structured findings from the reviewer (the missing-test entries) and
produces one Kotlin test file per finding. The agent itself uses OpenAI here
so we deliberately separate this from the reviewer's provider choice.
"""
from __future__ import annotations

import json
import os
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable


_PROMPTS_DIR = Path(__file__).resolve().parent.parent / "prompts"
_PROMPT_FILES = {
    "unit": _PROMPTS_DIR / "android_test_gen_unit.md",
    "ui":   _PROMPTS_DIR / "android_test_gen_ui.md",
}


@dataclass
class GeneratedTest:
    path: str
    code: str
    source_finding: dict
    reason: str | None = None

    @property
    def is_valid(self) -> bool:
        return bool(self.path and self.code)


def _load_prompt(kind: str) -> str:
    path = _PROMPT_FILES.get(kind)
    if path is None:
        raise ValueError(
            f"Unknown test kind {kind!r}. Expected 'unit' or 'ui'."
        )
    return path.read_text(encoding="utf-8")


def _read_source_excerpt(repo_root: Path, file_path: str, max_chars: int = 8000) -> str:
    """Best-effort read of the production source the finding refers to."""
    candidate = repo_root / file_path
    if not candidate.exists() or not candidate.is_file():
        return f"// Source not found at {file_path}"
    text = candidate.read_text(encoding="utf-8", errors="replace")
    return text[:max_chars]


def _extract_json(raw: str) -> dict:
    raw = raw.strip()
    fence = re.search(r"```(?:json)?\s*(\{.*?\})\s*```", raw, re.DOTALL)
    if fence:
        candidate = fence.group(1)
    else:
        m = re.search(r"\{.*\}", raw, re.DOTALL)
        candidate = m.group(0) if m else raw
    try:
        return json.loads(candidate)
    except json.JSONDecodeError:
        return {}


def _iter_targets(findings: dict) -> Iterable[dict]:
    """Yield each missing-test finding, tagging the test kind."""
    for entry in findings.get("missing_unit_tests") or []:
        if isinstance(entry, dict):
            yield {**entry, "_kind": "unit"}
    for entry in findings.get("missing_ui_tests") or []:
        if isinstance(entry, dict):
            yield {**entry, "_kind": "ui"}


def generate_tests(
    findings: dict,
    repo_root: Path,
    *,
    openai_api_key: str | None = None,
    openai_model: str | None = None,
    max_targets: int = 5,
) -> list[GeneratedTest]:
    """Generate one Kotlin test file per missing-test finding."""
    from openai import OpenAI

    api_key = openai_api_key or os.environ.get("OPENAI_API_KEY")
    if not api_key:
        raise RuntimeError("OPENAI_API_KEY is required for the test generator.")

    client = OpenAI(api_key=api_key)
    # LangSmith tracing for each generated test, if enabled.
    if os.environ.get("LANGSMITH_TRACING", "").lower() == "true":
        try:
            from langsmith.wrappers import wrap_openai
            client = wrap_openai(client)
        except Exception:
            pass

    model = openai_model or os.environ.get("OPENAI_MODEL", "gpt-4o-mini")

    out: list[GeneratedTest] = []

    targets = list(_iter_targets(findings))[:max_targets]
    for target in targets:
        kind = target.get("_kind", "unit")
        base_prompt = _load_prompt(kind)
        source_path = target.get("file") or ""
        excerpt = _read_source_excerpt(repo_root, source_path)

        prompt = (
            f"{base_prompt}\n\n"
            f"## Target\n"
            f"```json\n{json.dumps(target, indent=2)}\n```\n\n"
            f"## Production source (`{source_path}`)\n"
            f"```kotlin\n{excerpt}\n```\n"
        )

        resp = client.chat.completions.create(
            model=model,
            max_tokens=int(os.environ.get("MAX_TOKENS", "2048")),
            messages=[{"role": "user", "content": prompt}],
        )
        raw = (resp.choices[0].message.content or "").strip()
        parsed = _extract_json(raw)

        out.append(
            GeneratedTest(
                path=parsed.get("path", ""),
                code=parsed.get("code", ""),
                source_finding=target,
                reason=parsed.get("reason"),
            )
        )

    return out


def write_tests(
    tests: Iterable[GeneratedTest],
    repo_root: Path,
    overwrite: bool = False,
) -> list[Path]:
    """Write generated test files to disk. Returns the paths actually written."""
    written: list[Path] = []
    for t in tests:
        if not t.is_valid:
            continue
        target = repo_root / t.path
        if target.exists() and not overwrite:
            # Avoid clobbering existing tests
            continue
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(t.code, encoding="utf-8")
        written.append(target)
    return written
