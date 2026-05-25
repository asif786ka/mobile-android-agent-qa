"""Mock LLM provider — returns canned responses without hitting any API.

Used for live demos / interviews where you don't want network or rate-limit
risk. Set PROVIDER=mock and the agent runs end-to-end offline.
"""
from __future__ import annotations

import json

from .base import LLMProvider


class MockProvider(LLMProvider):
    name = "mock"

    _CANNED_REVIEW = json.dumps({
        "platform": "android",
        "summary": (
            "New Times-style article teaser formatter and Compose headline screen "
            "landed without unit or UI tests — high-risk path for paywall/teaser copy."
        ),
        "missing_unit_tests": [
            {
                "file": (
                    "android/app/src/main/java/com/example/helloworld/"
                    "ArticleTeaserFormatter.kt"
                ),
                "symbol": "ArticleTeaserFormatter.headline()",
                "note": (
                    "Truncation, blank teaser fallback, and maxLen edge cases "
                    "need JUnit coverage."
                ),
            }
        ],
        "missing_ui_tests": [
            {
                "file": (
                    "android/app/src/main/java/com/example/helloworld/"
                    "ArticleTeaserScreen.kt"
                ),
                "note": (
                    "Composable exposes ARTICLE_TEASER_TAG but has no Compose UI "
                    "test (createComposeRule + assertTextEquals)."
                ),
            }
        ],
        "weak_assertions": [],
        "flaky_selectors": [],
        "missing_accessibility": [
            {
                "file": "android/app/src/main/java/com/example/helloworld/ArticleTeaserScreen.kt",
                "note": "Headline Text has no contentDescription for TalkBack.",
            }
        ],
        "missing_negative_cases": [
            {
                "file": "android/app/src/main/java/com/example/helloworld/ArticleTeaserFormatter.kt",
                "note": "No test for invalid maxLen (require throws).",
            }
        ],
        "suggested_tests": [
            {
                "name": "ArticleTeaserFormatterTest.headline_longTeaser_truncatesWithEllipsis",
                "description": "Assert teaser longer than maxLen ends with ellipsis.",
            }
        ],
    })

    _CANNED_TEST_FILE = json.dumps({
        "path": (
            "android/app/src/test/java/com/example/helloworld/"
            "ArticleTeaserFormatterTest.kt"
        ),
        "code": (
            "package com.example.helloworld\n\n"
            "import org.junit.Test\n"
            "import org.junit.Assert.assertEquals\n"
            "import org.junit.Assert.assertTrue\n\n"
            "class ArticleTeaserFormatterTest {\n"
            "    @Test\n"
            "    fun headline_blankTeaser_usesDefault() {\n"
            "        assertEquals(\n"
            "            ArticleTeaserFormatter.DEFAULT_TEASER,\n"
            "            ArticleTeaserFormatter.headline(\"   \"),\n"
            "        )\n"
            "    }\n\n"
            "    @Test\n"
            "    fun headline_shortTeaser_unchanged() {\n"
            "        val teaser = \"Live: Budget speech underway\"\n"
            "        assertEquals(teaser, ArticleTeaserFormatter.headline(teaser, 80))\n"
            "    }\n\n"
            "    @Test\n"
            "    fun headline_longTeaser_truncatesWithEllipsis() {\n"
            "        val long = \"A\".repeat(100)\n"
            "        val result = ArticleTeaserFormatter.headline(long, 20)\n"
            "        assertTrue(result.endsWith(\"…\"))\n"
            "        assertTrue(result.length <= 20)\n"
            "    }\n\n"
            "    @Test\n"
            "    fun headline_trimsWhitespace() {\n"
            "        assertEquals(\n"
            "            \"Headline\",\n"
            "            ArticleTeaserFormatter.headline(\"  Headline  \"),\n"
            "        )\n"
            "    }\n"
            "}\n"
        ),
    })

    def __init__(self, model: str | None = None) -> None:
        self._model = model or "mock-canned-v1"

    def generate(self, prompt: str, *, max_tokens: int = 2048) -> str:
        if "test-automation engineer" in prompt or (
            "Kotlin" in prompt and "JUnit" in prompt
        ):
            return self._CANNED_TEST_FILE
        return self._CANNED_REVIEW
