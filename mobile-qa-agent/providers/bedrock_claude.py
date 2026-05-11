"""AWS Bedrock provider — works for any Bedrock model that supports the
converse() API, including Anthropic Claude, Amazon Nova, Meta Llama, etc.

Defaults to Amazon Nova Pro because it doesn't require any third-party
use-case gate (unlike Anthropic models on Bedrock). To use Claude on Bedrock
instead, set BEDROCK_MODEL_ID=us.anthropic.claude-sonnet-4-5-20250929-v1:0
after completing Anthropic's use-case form in the Bedrock console.
"""
from __future__ import annotations

import os

from .base import LLMProvider


class BedrockProvider(LLMProvider):
    name = "bedrock"

    def __init__(self, model_id: str | None = None, region: str | None = None) -> None:
        import boto3

        region = region or os.environ.get("AWS_REGION", "us-east-1")
        self._client = boto3.client("bedrock-runtime", region_name=region)
        # Default to Nova Pro via the US cross-region inference profile.
        # Switch via BEDROCK_MODEL_ID env var — for example:
        #   us.amazon.nova-pro-v1:0                            (Amazon Nova Pro)
        #   us.amazon.nova-lite-v1:0                           (Nova Lite, cheaper)
        #   us.anthropic.claude-sonnet-4-5-20250929-v1:0       (Claude Sonnet 4.5)
        #   us.meta.llama3-3-70b-instruct-v1:0                 (Llama 3.3 70B)
        self._model_id = model_id or os.environ.get(
            "BEDROCK_MODEL_ID",
            "us.amazon.nova-pro-v1:0",
        )

    def generate(self, prompt: str, *, max_tokens: int = 2048) -> str:
        resp = self._client.converse(
            modelId=self._model_id,
            messages=[
                {
                    "role": "user",
                    "content": [{"text": prompt}],
                }
            ],
            inferenceConfig={"maxTokens": max_tokens},
        )
        blocks = resp["output"]["message"]["content"]
        return "".join(b.get("text", "") for b in blocks).strip()


# Backwards-compat alias for callers that imported the old name
BedrockClaudeProvider = BedrockProvider
