# Interview demo playbook

For the 75-minute interview at News UK with Jack + 1 engineer.
Two parts: agentic system design (35 min) + AI-assisted coding (35 min).

## Pre-call setup (do this 30 min before)

- Close every app except: terminal, browser (Excalidraw + GitHub repo tabs),
  VS Code / Cursor.
- Open Excalidraw at https://excalidraw.com — fresh canvas, libraries
  pre-loaded (System Design + AWS).
- Open this repo in your IDE.
- Open the GitHub PR page in a browser tab for the demo PR.
- Pre-warm gradle so the first `./gradlew testDebugUnitTest` is fast:
    `cd android && ./gradlew testDebugUnitTest --quiet`
- Pre-warm python venv:
    `cd mobile-qa-agent && pip install -r requirements.txt -q`
- Verify the demo runs end-to-end offline (mock provider):
    `bash scripts/interview-demo.sh`
  Should complete in <60s with no API key.
- Open LangSmith dashboard tab — even if the live demo uses the mock
  provider, you can show prior real traces from your dev runs.
- Wifi backup: enable phone hotspot, know how to switch in 10 seconds.

## Part 1 — Agentic system design (35 min)

### Opening (3 min)
- Listen for the prompt. Don't draw yet. Restate the problem:
  "So you want an agent that does X for Y users at Z scale. Is that right?"
- Clarify 3 things before you sketch:
    1. Scale (users, requests/sec, data volume)
    2. Latency target (real-time, near-real-time, async)
    3. Failure tolerance (any downtime acceptable?)

### Sketch high-level (8 min on Excalidraw)
- Top-down: user → API gateway → service mesh → LLM provider → data layer
- Label every box with its responsibility in 3 words max
- Draw arrows with the data type on them ("PR diff", "JSON findings")
- Don't draw infra (k8s, vpc) until they ask

### Drill into 2 components (12 min)
Pick the two most interesting components and go deep:
- Show the internal state machine of the agent (LangGraph node tree)
- Show the data model (entities, relationships)
- Show the prompt template structure
- Discuss the provider abstraction (Claude / OpenAI / Bedrock pluggable)

### Discuss tradeoffs (8 min)
Be ready for these questions:
- "How do you handle hallucinations?" → validation step + structured output
  + grade-then-commit pattern, exactly like the auto-commit guard in this repo.
- "How do you scale to 10K req/sec?" → queue-based architecture, batching,
  caching frequently-asked prompts, semantic dedup.
- "What about cost?" → tiered model routing (cheap classifier first, then
  expensive reasoner only on filtered subset).
- "How do you observe it?" → LangSmith for traces, structured logging,
  per-node latency, cost-per-request dashboards.
- "How do you prevent prompt injection?" → input sanitization, output
  validation, system prompts in a separate scope, never trust user content
  as instructions.

### Close (4 min)
- "If I had another 30 min, I'd dig into: ____ and ____"
- Volunteer the next two things you'd build — shows you're thinking
  beyond the prompt.

## Part 2 — AI-assisted coding (35 min)

### Likely formats
1. "Here's a small task. Use AI to help you solve it. We'll watch."
2. "Take this existing code, extend it with AI assistance."
3. "Pair-program with us on a new feature."

### Bring your IDE setup
- Cursor with Claude Sonnet 4.5 as model
- Or VS Code + GitHub Copilot Chat
- Whichever you use daily — don't switch tools for the interview

### Talking-out-loud playbook
When you prompt the AI, narrate:
- "I'm asking Claude to scaffold the data model first because the schema
  drives the API surface."
- "I'm being specific about the test framework (JUnit 4) so it doesn't
  default to JUnit 5."
- "I'm rejecting that suggestion — it's using Thread.sleep, which we
  don't allow in this repo."

### When AI gives bad output
This is the most important thing. Interviewers want to see:
- You read the output critically before accepting.
- You know what good Kotlin/Python/architecture looks like.
- You iterate on the prompt rather than fight the code.

If Claude/Copilot writes something wrong, say:
- "This is close but it's missing X. Let me ask it to fix that."
- Don't just accept-and-move-on. Don't pretend bad output is good.

### Demo opportunity — this repo
If the coding task is open-ended ("show us something you've built with
agents"), open this repo and walk through:

1. `bash scripts/interview-demo.sh review` (10 sec)
   "Here's Claude reviewing a real PR diff (Times-style
   `ArticleTeaserFormatter` + Compose screen). Mock provider so we're
   offline, but the real run uses Anthropic. Watch — structured JSON
   with unit + UI test gaps."

2. `bash scripts/interview-demo.sh gen` (5 sec)
   "Now OpenAI takes the findings and writes the actual Kotlin test
   file. Two prompts in the repo: one for unit tests, one for Compose
   UI tests. Different patterns, different framework conventions."

3. `bash scripts/interview-demo.sh tests` (20 sec, real gradle run)
   "Finally we verify by actually running ./gradlew testDebugUnitTest.
   If it passes, the bot commits to the PR branch. If not, posts a
   comment and skips. That's the auto-commit guard."

Show the LangGraph node code in `mobile-qa-agent/graph.py` (~50 lines).
Show the workflow YAML.
Show a real LangSmith trace from a previous run (if you have an
internet connection that day).

## If something breaks live

| Failure | Backup |
|---|---|
| Excalidraw won't load | tldraw.com (same shape, same vibes) |
| Wifi drops | Phone hotspot, demo script is offline anyway |
| Gradle fails | Skip phase 3, show the test code in the IDE and explain |
| Mock provider returns weird JSON | Hit Ctrl-C, fall back to walking through the code |
| AI generates bad code | Narrate why it's bad, prompt again with constraints |
| You blank on a question | "Let me think for 30 seconds" — silence is fine |

## What Jack is likely watching for

This role wants a Principal/Architect, so the signals are:
- Can you reason about tradeoffs out loud?
- Do you ask clarifying questions before drawing?
- Do you scope the problem (MVP vs scaled)?
- Do you talk about failure modes, observability, cost?
- Are you opinionated but humble?
- Do you mentor while coding (explain WHY, not just WHAT)?

Avoid:
- Drawing before clarifying.
- Naming AWS services without justification.
- "It depends" without showing the dimensions it depends on.
- Defensiveness when challenged — they're testing how you handle pushback.
- Going over time — leave 5 min for Q&A in each part.

## News UK / media-specific angles to volunteer

If the conversation is open-ended, lean into mobile + AI for media:
- Push notification copy generation per audience segment
- Article-to-mobile-format conversion (long-form → mobile snippet)
- Paywall personalization decisioning agent
- AI QA agent for high-stakes paywall + ad-SDK code paths
- Accessibility audit agent (UK regulation drives this)
- App Store review responder agent (community ops time saver)

These show you're not just a generalist — you've thought about media
product specifically.
