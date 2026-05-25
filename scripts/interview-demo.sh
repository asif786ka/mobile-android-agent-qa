#!/usr/bin/env bash
# Interview demo runner. Three phases, all offline (PROVIDER=mock), takes <60s.
#
# Usage:
#   bash scripts/interview-demo.sh           # review → generate → unit tests
#   bash scripts/interview-demo.sh review
#   bash scripts/interview-demo.sh gen
#   bash scripts/interview-demo.sh tests

set -e

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

PHASE="${1:-all}"

review() {
  echo "════════════════════════════════════════════════════"
  echo "  PHASE 1 — Review article-teaser PR diff (mock LLM)"
  echo "════════════════════════════════════════════════════"
  cd "$ROOT/mobile-qa-agent"
  PROVIDER=mock python main.py \
    --diff-file samples/demo-pr.diff \
    --dry-run \
    --save-findings /tmp/findings.json \
    --print-json
  echo ""
  echo "→ Findings saved to /tmp/findings.json"
}

generate() {
  echo ""
  echo "════════════════════════════════════════════════════"
  echo "  PHASE 2 — Generate missing unit tests (mock OpenAI)"
  echo "════════════════════════════════════════════════════"
  cd "$ROOT/mobile-qa-agent"
  PROVIDER=mock OPENAI_API_KEY=mock python generate_tests.py \
    --findings-file /tmp/findings.json \
    --dry-run \
    --max 5
}

run_tests() {
  echo ""
  echo "════════════════════════════════════════════════════"
  echo "  PHASE 3 — Verify with gradle (existing unit tests)"
  echo "════════════════════════════════════════════════════"
  cd "$ROOT/android"
  if [ ! -f ./gradlew ]; then
    echo "⚠️  No gradle wrapper. Run: gradle wrapper --gradle-version 8.7"
    exit 1
  fi
  ./gradlew testDebugUnitTest --quiet
  echo "✅ All unit tests passed."
}

case "$PHASE" in
  review)   review ;;
  gen|generate) generate ;;
  tests)    run_tests ;;
  all|"")   review; generate; run_tests ;;
  *)        echo "Unknown phase: $PHASE"; exit 1 ;;
esac
