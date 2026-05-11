# mobile-android-agent-qa

End-to-end mobile QA automation: a tiny Hello World app on Android (Kotlin/Compose)
and iOS (SwiftUI), normal unit + UI tests, and an AI QA agent that reviews PRs
and posts structured feedback on missing tests, weak assertions, flaky selectors,
and accessibility gaps.

## Layout

```
.
├── android/                  # Hello World Android app (Kotlin + Compose)
├── ios/                      # Hello World iOS app (SwiftUI)
├── mobile-qa-agent/          # Python AI QA agent (LangGraph + 3 providers)
└── .github/workflows/        # CI: android, ios, AI QA review
```

## Build order

1. **Android Hello World** — Compose UI, single `Text` with `testTag("hello_text")`.
2. **Android tests** — JUnit (`src/test`) + Compose UI test (`src/androidTest`).
3. **Android CI** — `.github/workflows/android-ci.yml` (unit + emulator job).
4. **AI QA agent** — `mobile-qa-agent/` with pluggable providers
   (`anthropic`, `bedrock`, `openai`) selected via `PROVIDER` env var.
5. **AI review CI** — `.github/workflows/ai-qa-review.yml` runs the agent on
   every PR and posts a review comment.
6. **iOS mirror** — SwiftUI Hello World, XCTest + XCUITest, iOS CI workflow,
   iOS detector + prompt in the agent.

## Required GitHub secrets

Set whichever your chosen provider needs (the agent skips ones not configured):

| Secret              | Provider                |
|---------------------|-------------------------|
| `ANTHROPIC_API_KEY` | `anthropic` (default)   |
| `OPENAI_API_KEY`    | `openai`                |
| `AWS_ACCESS_KEY_ID` + `AWS_SECRET_ACCESS_KEY` + `AWS_REGION` | `bedrock` |

`GITHUB_TOKEN` is provided automatically by Actions.

## Running locally

```bash
# Android unit tests
cd android && ./gradlew testDebugUnitTest

# iOS tests
cd ios && xcodebuild test -scheme HelloWorld -destination 'platform=iOS Simulator,name=iPhone 15'

# AI agent against a local diff
cd mobile-qa-agent
export PROVIDER=anthropic
export ANTHROPIC_API_KEY=sk-ant-...
python main.py --diff-file ../sample.diff --dry-run
```
