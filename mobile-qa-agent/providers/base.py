"""Abstract base class for LLM providers."""
from __future__ import annotations

from abc import ABC, abstractmethod


class LLMProvider(ABC):
    """All providers expose the same simple `generate(prompt) -> str` API.

    The agent is intentionally synchronous: a PR review is one inference per run.
    """

    name: str = "base"

    @abstractmethod
    def generate(self, prompt: str, *, max_tokens: int = 2048) -> str:
        """Send a prompt; return raw model text."""
        raise NotImplementedError
