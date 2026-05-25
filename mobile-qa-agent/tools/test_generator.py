"""OpenAI-powered Kotlin test generator.

Takes structured findings from the reviewer (the missing-test entries) and
produces one Kotlin test file per finding. The agent itself uses OpenAI here
so we deliberately separate this from the reviewer's provider choice.

Two modes per finding:

* **create** — no existing test file matches the target. The generator writes
  a brand-new test file. `write_tests(overwrite=False)` will refuse to clobber
  any human-written file that happens to share the path.
* **amend** — an existing test file is found for the target. The generator
  passes the existing file's contents to the LLM as additional context, asks
  it to *preserve* the existing test methods and add new ones for any
  uncovered or modified behavior, and returns the COMPLETE merged file.
  `write_tests` always overwrites in this mode (the existing content was the
  LLM's input, so overwriting is the intended outcome).
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


# Appended to the base prompt when an existing test file is found. Tells the
# LLM exactly how amend-mode output should look and gives it the schema rules
# we already enforce for create-mode output.
_AMEND_INSTRUCTION = """\

## Amendment mode — IMPORTANT

An existing test file is provided below under `## Existing test file`. You are
**amending** that file, not replacing it. Follow these rules:

- Preserve every existing test method exactly as written, unless the diff has
  made one of them factually wrong — in that case, fix only what must change
  and keep the same method name.
- Add new `@Test` methods for any uncovered or modified behavior described in
  the target's `note`. The note is the reviewer's specific instruction for
  what to add.
- Keep the same package, class name, import order, and existing helper
  utilities. Do not rename the class.
- Return the **COMPLETE merged file** in `code` — full `package` line, all
  imports (existing + any new ones you need), and the full class body with
  both pre-existing and new test methods.
- The `path` you return MUST be exactly the path of the existing test file.

Do NOT return only the new tests. Do NOT delete existing tests. Do NOT change
the class name.
"""


@dataclass
class GeneratedTest:
    path: str
    code: str
    source_finding: dict
    reason: str | None = None
    # "create" (new test file) or "amend" (merged on top of an existing one).
    # write_tests() uses this to decide whether to overwrite.
    mode: str = "create"
    # If mode == "amend", this is the path of the existing test we passed to
    # the LLM. write_tests() refuses to overwrite unless `path` matches this.
    existing_path: str | None = None

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


def _predict_test_path(source_path: str, kind: str) -> str | None:
    """Map a production-source path to the conventional test path for `kind`.

    Examples:
        unit: android/app/src/main/java/com/example/helloworld/Greeting.kt
           -> android/app/src/test/java/com/example/helloworld/GreetingTest.kt
        ui:   android/app/src/main/java/com/example/helloworld/HelloScreen.kt
           -> android/app/src/androidTest/java/com/example/helloworld/HelloScreenUiTest.kt

    Returns None if the source path doesn't look like a `src/main/` Android file
    (in which case we have no convention to apply).
    """
    if not source_path or not source_path.endswith(".kt"):
        return None
    if "/src/main/" not in source_path:
        return None

    if kind == "unit":
        swapped = source_path.replace("/src/main/", "/src/test/", 1)
        return swapped[: -len(".kt")] + "Test.kt"
    if kind == "ui":
        swapped = source_path.replace("/src/main/", "/src/androidTest/", 1)
        return swapped[: -len(".kt")] + "UiTest.kt"
    return None


def _read_existing_test(
    repo_root: Path,
    predicted: str | None,
    max_chars: int = 12000,
) -> tuple[str | None, str | None]:
    """Return (existing_test_path, existing_test_text) if the predicted file
    exists on disk, otherwise (None, None)."""
    if not predicted:
        return None, None
    candidate = repo_root / predicted
    if not candidate.exists() or not candidate.is_file():
        return None, None
    text = candidate.read_text(encoding="utf-8", errors="replace")
    return predicted, text[:max_chars]


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
    """Generate one Kotlin test file per missing-test finding.

    If an existing test file is found for a target, the generator switches to
    amend mode for that target: the existing content is passed to the LLM
    along with an instruction to preserve existing tests and merge in new
    coverage. The resulting `GeneratedTest.mode` is "amend".
    """
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

        predicted = _predict_test_path(source_path, kind)
        existing_path, existing_text = _read_existing_test(repo_root, predicted)
        is_amend = existing_text is not None

        prompt_parts = [
            base_prompt,
            "## Target",
            f"```json\n{json.dumps(target, indent=2)}\n```",
            f"## Production source (`{source_path}`)",
            f"```kotlin\n{excerpt}\n```",
        ]
        if is_amend:
            prompt_parts.extend([
                _AMEND_INSTRUCTION,
                f"## Existing test file (`{existing_path}`)",
                f"```kotlin\n{existing_text}\n```",
            ])
        prompt = "\n\n".join(prompt_parts) + "\n"

        resp = client.chat.completions.create(
            model=model,
            max_tokens=int(os.environ.get("MAX_TOKENS", "2048")),
            messages=[{"role": "user", "content": prompt}],
        )

        # Record token usage for this generator call. Best-effort — never
        # fail generation if the tracker can't write.
        try:
            from .usage_tracker import record_usage
            usage = getattr(resp, "usage", None)
            record_usage(
                provider="openai",
                model=model,
                input_tokens=getattr(usage, "prompt_tokens", 0) or 0,
                output_tokens=getattr(usage, "completion_tokens", 0) or 0,
                role="generator",
                extra={
                    "test_kind": kind,
                    "mode": "amend" if is_amend else "create",
                    "target_file": source_path,
                },
            )
        except Exception:
            pass

        raw = (resp.choices[0].message.content or "").strip()
        parsed = _extract_json(raw)

        out.append(
            GeneratedTest(
                path=parsed.get("path", ""),
                code=parsed.get("code", ""),
                source_finding=target,
                reason=parsed.get("reason"),
                mode="amend" if is_amend else "create",
                existing_path=existing_path,
            )
        )

    return out


def write_tests(
    tests: Iterable[GeneratedTest],
    repo_root: Path,
    overwrite: bool = False,
) -> list[Path]:
    """Write generated test files to disk. Returns the paths actually written.

    Behavior:

    * `mode == "create"` (default): write only if the target file does not
      exist, unless `overwrite=True` is passed (used by the CI fix-retry loop
      when its own previous output is what we want to replace).
    * `mode == "amend"`: always overwrite, but only if the returned `path`
      matches the `existing_path` we identified during generation. This
      guards against the LLM going off-script and emitting amend-mode output
      that targets a different file.
    """
    written: list[Path] = []
    for t in tests:
        if not t.is_valid:
            continue
        target = repo_root / t.path

        if t.mode == "amend":
            if t.existing_path and t.path == t.existing_path:
                # Amending the file we showed the LLM — overwrite is intended.
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_text(t.code, encoding="utf-8")
                written.append(target)
                continue
            # LLM deviated from the existing path; fall through to create-mode
            # safety rules so we don't accidentally clobber an unrelated file.

        if target.exists() and not overwrite:
            # Avoid clobbering an unrelated existing file in create mode.
            continue
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(t.code, encoding="utf-8")
        written.append(target)
    return written
