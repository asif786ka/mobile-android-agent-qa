# mobile-qa-agent

An AI QA agent that reviews mobile PRs and posts structured feedback on test
coverage gaps, weak assertions, flaky selectors, and accessibility misses.

## Architecture

```
GitHub PR event
      │
      ▼
┌────────────────────┐
│ main.py            │  reads GITHUB_EVENT_PATH, env, args
└─────────┬──────────┘
          ▼
┌────────────────────┐
│ graph.py (LangGraph)
│  ┌──────────────┐  │
│  │ fetch_diff   │  │  tools/github_diff.py
│  └──────┬───────┘  │
│         ▼          │
│  ┌──────────────┐  │
│  │ detect       │  │  tools/{android,ios}_detector.py
│  └──────┬───────┘  │
│         ▼          │
│  ┌──────────────┐  │
│  │ review       │  │  tools/test_reviewer.py + providers/*
│  └──────┬───────┘  │
│         ▼          │
│  ┌──────────────┐  │
│  │ post_comment │  │  tools/github_diff.post_review_comment
│  └──────────────┘  │
└────────────────────┘
```

## Providers

Selected via `PROVIDER` env var (default: `anthropic`):

| `PROVIDER`  | Class                                        | Required env                                                                 |
|-------------|----------------------------------------------|------------------------------------------------------------------------------|
| `anthropic` | `providers.anthropic_direct.AnthropicProvider` | `ANTHROPIC_API_KEY`                                                          |
| `openai`    | `providers.openai_provider.OpenAIProvider`     | `OPENAI_API_KEY`                                                             |
| `bedrock`   | `providers.bedrock_claude.BedrockClaudeProvider` | `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION` (default us-east-1) |

Swap providers without touching graph or tool code.

## Local run

```bash
pip install -r requirements.txt

# Dry run against a saved diff (no GitHub calls, prints JSON):
export PROVIDER=anthropic
export ANTHROPIC_API_KEY=sk-ant-...
python main.py --diff-file ../sample.diff --dry-run
```

## In CI

The workflow at `.github/workflows/ai-qa-review.yml` runs the agent on every
PR. The PR event payload at `$GITHUB_EVENT_PATH` is read automatically; no
flags needed.
