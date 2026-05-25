"""Anthropic API provider (direct, not via Bedrock)."""
from __future__ import annotations

import os

from .base import LLMProvider


class AnthropicProvider(LLMProvider):
    name = "anthropic"

    # Populated after each generate() call by the response's `usage` block.
    # Callers (graph.py / generate_tests.py) read this and feed it to
    # tools.usage_tracker.record_usage for cost reporting.
    last_usage: dict | None = None

    def __init__(self, model: str | None = None) -> None:
        import anthropic  # imported lazily so the package isn't required for other providers

        api_key = os.environ.get("ANTHROPIC_API_KEY")
        if not api_key:
            raise RuntimeError("ANTHROPIC_API_KEY is not set.")

        client = anthropic.Anthropic(api_key=api_key)
        # Wrap with LangSmith tracing if langsmith is installed AND tracing is on.
        # Falls through silently if either isn't true.
        if os.environ.get("LANGSMITH_TRACING", "").lower() == "true":
            try:
                from langsmith.wrappers import wrap_anthropic
                client = wrap_anthropic(client)
            except Exception:
                pass
        self._client = client

        self._model = model or os.environ.get(
            "ANTHROPIC_MODEL", "claude-sonnet-4-6"
        )

    def generate(self, prompt: str, *, max_tokens: int = 2048) -> str:
        resp = self._client.messages.create(
            model=self._model,
            max_tokens=max_tokens,
            messages=[{"role": "user", "content": prompt}],
        )
        usage = getattr(resp, "usage", None)
        self.last_usage = {
            "model": self._model,
            "input_tokens": getattr(usage, "input_tokens", 0) or 0,
            "output_tokens": getattr(usage, "output_tokens", 0) or 0,
        }
        parts: list[str] = []
        for block in resp.content:
            text = getattr(block, "text", None)
            if text:
                parts.append(text)
        return "".join(parts).strip()
