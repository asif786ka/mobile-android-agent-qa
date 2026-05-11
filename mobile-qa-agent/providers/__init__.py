"""LLM provider factory."""
from __future__ import annotations

import os

from .base import LLMProvider


def get_provider(name: str | None = None) -> LLMProvider:
    """Return an LLMProvider for the given name (or PROVIDER env, or default)."""
    name = (name or os.environ.get("PROVIDER") or "anthropic").lower().strip()

    if name == "anthropic":
        from .anthropic_direct import AnthropicProvider
        return AnthropicProvider()
    if name == "openai":
        from .openai_provider import OpenAIProvider
        return OpenAIProvider()
    if name == "bedrock":
        from .bedrock_claude import BedrockProvider
        return BedrockProvider()

    raise ValueError(
        f"Unknown PROVIDER={name!r}. Expected one of: anthropic, openai, bedrock."
    )


__all__ = ["LLMProvider", "get_provider"]
