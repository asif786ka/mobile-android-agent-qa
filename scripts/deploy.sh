#!/usr/bin/env bash
# One-shot deploy to GitHub using a Personal Access Token + curl.
# No `gh` CLI required.
#
# Usage:
#   GH_TOKEN="github_pat_..." bash scripts/deploy.sh
#
# Optional env vars:
#   GH_USER       Your GitHub username (default: asif786ka)
#   REPO_NAME     Repo to create / push to (default: mobile-android-agent-qa)
#   DEMO_BRANCH   Branch name for the demo PR (default: demo/add-farewell)
#
# After this finishes:
#   1. Revoke the PAT at https://github.com/settings/tokens — it has been
#      exposed in chat and shell history.
#   2. Add ANTHROPIC_API_KEY (or OPENAI_API_KEY / AWS_*) as a repo Action secret.

set -euo pipefail

: "${GH_TOKEN:?Set GH_TOKEN to a GitHub PAT with 'repo' scope}"
GH_USER="${GH_USER:-asif786ka}"
REPO_NAME="${REPO_NAME:-mobile-android-agent-qa}"
DEMO_BRANCH="${DEMO_BRANCH:-demo/add-farewell}"
FULL_REPO="$GH_USER/$REPO_NAME"

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT_DIR"
echo "▸ Working in: $ROOT_DIR"

# ----------------------------------------------------------------------------
# 1. Reset local git state (the .git left over from the sandbox has stale
#    macOS file-lock state we can't easily repair). Safe to nuke — the source
#    files in your folder are untouched.
# ----------------------------------------------------------------------------
echo "▸ Resetting local .git..."
rm -rf .git
git init -q -b main
git config user.email "${GIT_EMAIL:-asif786ka@gmail.com}"
git config user.name  "${GIT_NAME:-Asif}"

# ----------------------------------------------------------------------------
# 2. Stage everything and make the initial commit.
# ----------------------------------------------------------------------------
git add -A
git commit -q -m "Initial commit: Android Hello World + AI QA agent + CI"

# ----------------------------------------------------------------------------
# 3. Create the GitHub repo (idempotent).
# ----------------------------------------------------------------------------
GH_API="https://api.github.com"
auth_header=(-H "Authorization: Bearer $GH_TOKEN" -H "Accept: application/vnd.github+json")

echo "▸ Verifying token..."
WHOAMI=$(curl -sS "${auth_header[@]}" "$GH_API/user" \
  | python3 -c "import json,sys;d=json.load(sys.stdin);print(d.get('login',''))")
if [ -z "$WHOAMI" ]; then
  echo "❌ Token did not authenticate. Check it has 'repo' scope."; exit 1
fi
echo "  → authenticated as $WHOAMI"

EXISTS=$(curl -sS -o /dev/null -w "%{http_code}" "${auth_header[@]}" \
  "$GH_API/repos/$FULL_REPO")

if [ "$EXISTS" = "200" ]; then
  echo "▸ Repo already exists: $FULL_REPO"
else
  echo "▸ Creating public repo $FULL_REPO..."
  curl -sS -X POST "${auth_header[@]}" "$GH_API/user/repos" \
    -d "{
      \"name\": \"$REPO_NAME\",
      \"description\": \"Android Hello World + AI QA agent (Anthropic/OpenAI/Bedrock) + CircleCI + GitHub Actions\",
      \"private\": false,
      \"auto_init\": false
    }" > /tmp/gh_create.json
  if ! grep -q '"html_url"' /tmp/gh_create.json; then
    echo "❌ Repo create failed:"; cat /tmp/gh_create.json; exit 1
  fi
  echo "  → created"
fi

# ----------------------------------------------------------------------------
# 4. Push main. Use the token via the URL so we don't write it to git config.
# ----------------------------------------------------------------------------
echo "▸ Pushing main..."
git remote remove origin 2>/dev/null || true
git remote add origin "https://x-access-token:${GH_TOKEN}@github.com/${FULL_REPO}.git"
git push -u -q origin main
echo "  → pushed"

# ----------------------------------------------------------------------------
# 5. Create demo branch with Farewell.kt (no test) so the AI agent has
#    something to flag. The file is already on main from the initial commit
#    if you ran this after Claude's setup, so this branch's only diff is
#    intentional — we re-add it from main to make the PR meaningful.
# ----------------------------------------------------------------------------
echo "▸ Creating demo branch $DEMO_BRANCH..."
# If Farewell.kt was already on main, move it to the demo branch only so
# main → demo diff actually shows the new file.
FAREWELL=android/app/src/main/java/com/example/helloworld/Farewell.kt
if git ls-files --error-unmatch "$FAREWELL" >/dev/null 2>&1; then
  # Already on main — drop from main, push, then add back on demo branch
  git rm -q "$FAREWELL"
  git commit -q -m "Remove Farewell stub from main (will live on demo branch)"
  git push -q origin main
fi

git checkout -q -b "$DEMO_BRANCH"

# Re-create Farewell.kt on the demo branch
mkdir -p "$(dirname "$FAREWELL")"
cat > "$FAREWELL" <<'EOF'
package com.example.helloworld

/**
 * Companion to Greeting. Intentionally shipped without a unit test so the
 * AI QA agent has something concrete to flag in the demo PR.
 */
object Farewell {
    fun goodbye(name: String = Greeting.DEFAULT_NAME): String {
        val trimmed = name.trim()
        val who = if (trimmed.isEmpty()) Greeting.DEFAULT_NAME else trimmed
        return "Goodbye $who"
    }
}
EOF

git add "$FAREWELL"
git commit -q -m "Add Farewell.goodbye (no test — for AI agent demo)"
git push -u -q origin "$DEMO_BRANCH"
echo "  → pushed $DEMO_BRANCH"

# ----------------------------------------------------------------------------
# 6. Open the demo PR.
# ----------------------------------------------------------------------------
echo "▸ Opening demo PR..."
PR_BODY='This PR intentionally adds a new public function (`Farewell.goodbye`) without a unit test so the AI QA agent has something concrete to flag.\n\nExpect a review comment from the **AI Mobile QA Review** workflow listing:\n- missing unit test for `Farewell.goodbye()`\n- missing negative cases (blank input, trimming)\n- a suggested `FarewellTest` mirroring `GreetingTest`'

PR_RESP=$(curl -sS -X POST "${auth_header[@]}" \
  "$GH_API/repos/$FULL_REPO/pulls" \
  -d "{
    \"title\": \"Demo: add Farewell.goodbye (no test)\",
    \"head\": \"$DEMO_BRANCH\",
    \"base\": \"main\",
    \"body\": \"$PR_BODY\"
  }")

PR_URL=$(echo "$PR_RESP" | python3 -c "import json,sys;d=json.load(sys.stdin);print(d.get('html_url',''))")
if [ -z "$PR_URL" ]; then
  # PR may already exist
  PR_URL=$(curl -sS "${auth_header[@]}" \
    "$GH_API/repos/$FULL_REPO/pulls?head=$GH_USER:$DEMO_BRANCH&state=open" \
    | python3 -c "import json,sys;d=json.load(sys.stdin);print(d[0]['html_url'] if d else '')")
fi

# Strip the token from the remote so subsequent local git ops don't leak it
git remote set-url origin "https://github.com/${FULL_REPO}.git"

echo ""
echo "✅ Done."
echo "   Repo:        https://github.com/$FULL_REPO"
echo "   Demo PR:     ${PR_URL:-<failed to open; check Actions tab>}"
echo "   Actions tab: https://github.com/$FULL_REPO/actions"
echo ""
echo "⚠️  ROTATE YOUR TOKEN NOW:"
echo "   https://github.com/settings/tokens"
echo ""
echo "Next: in repo Settings → Secrets and variables → Actions, add:"
echo "   ANTHROPIC_API_KEY   (or OPENAI_API_KEY, or AWS_* for Bedrock)"
echo "Then re-run the failed 'AI Mobile QA Review' workflow on the PR."
