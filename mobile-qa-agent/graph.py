"""LangGraph state machine for the mobile QA review.

Nodes:
  fetch_diff  → detect_platform → review → format_comment → (optional) post

State is a TypedDict so each node only writes its own keys.
"""
from __future__ import annotations

from typing import Any, TypedDict

from langgraph.graph import END, StateGraph

from providers import get_provider
from tools import github_diff
from tools.android_detector import detect_android
from tools.ios_detector import detect_ios
from tools.test_reviewer import format_comment, review


class AgentState(TypedDict, total=False):
    # Inputs
    diff_file: str | None
    dry_run: bool
    provider_name: str | None

    # Populated by nodes
    pr_context: Any           # github_diff.PRContext
    detection: dict
    platform: str
    result: dict
    comment_body: str
    post_failed: bool         # set true when the post node refuses to publish


def _node_fetch_diff(state: AgentState) -> AgentState:
    if state.get("diff_file"):
        ctx = github_diff.read_local_diff(state["diff_file"])
    else:
        ctx = github_diff.fetch_pr_context()
    return {"pr_context": ctx}


def _node_detect(state: AgentState) -> AgentState:
    ctx = state["pr_context"]
    android = detect_android(ctx.changed_files, ctx.diff)
    ios = detect_ios(ctx.changed_files, ctx.diff)

    if android["is_android"] and not ios["is_ios"]:
        platform, detection = "android", android
    elif ios["is_ios"] and not android["is_android"]:
        platform, detection = "ios", ios
    elif android["is_android"] and ios["is_ios"]:
        # Cross-platform PR: review against whichever has more files
        if len(android["kotlin_files"]) + len(android["java_files"]) >= len(ios["swift_files"]):
            platform, detection = "android", android
        else:
            platform, detection = "ios", ios
        detection["note"] = "Cross-platform PR detected; reviewing dominant platform."
    else:
        platform, detection = "android", {**android, "note": "No mobile signal; defaulting to Android prompt."}

    return {"platform": platform, "detection": detection}


def _node_review(state: AgentState) -> AgentState:
    provider = get_provider(state.get("provider_name"))
    ctx = state["pr_context"]
    result = review(provider, state["platform"], state["detection"], ctx.diff)

    # Record this provider call's token usage + estimated cost.
    usage = getattr(provider, "last_usage", None) or {}
    if usage:
        try:
            from tools.usage_tracker import record_usage
            record_usage(
                provider=provider.name,
                model=usage.get("model", ""),
                input_tokens=usage.get("input_tokens", 0),
                output_tokens=usage.get("output_tokens", 0),
                role="reviewer",
            )
        except Exception:
            # Usage tracking is best-effort; never fail the review on it.
            pass

    return {"result": result}


def _node_format(state: AgentState) -> AgentState:
    return {"comment_body": format_comment(state["result"])}


def _node_post(state: AgentState) -> AgentState:
    if state.get("dry_run"):
        return {}

    # Refuse to publish parse-failure comments to the PR. The previous
    # behaviour (silently posting a "Could not parse a structured response"
    # comment with the raw model output) just littered PRs with broken
    # reviews — see the LangSmith Engine alert
    # "Review JSON parse failures silently posted to PR as fallback comment".
    # If parsing genuinely failed even after repair + retry, fail the CI run
    # loudly so the dev knows to re-trigger or escalate.
    if (state.get("result") or {}).get("_parse_error"):
        import sys
        print(
            "[post] Reviewer produced unparseable JSON even after json_repair "
            "and one LLM retry. NOT publishing fallback comment to the PR. "
            "Failing the CI run instead.",
            file=sys.stderr, flush=True,
        )
        return {"post_failed": True}

    github_diff.post_review_comment(state["pr_context"], state["comment_body"])
    return {}


def build_graph():
    g = StateGraph(AgentState)
    g.add_node("fetch_diff", _node_fetch_diff)
    g.add_node("detect", _node_detect)
    g.add_node("review", _node_review)
    g.add_node("format", _node_format)
    g.add_node("post", _node_post)

    g.set_entry_point("fetch_diff")
    g.add_edge("fetch_diff", "detect")
    g.add_edge("detect", "review")
    g.add_edge("review", "format")
    g.add_edge("format", "post")
    g.add_edge("post", END)
    return g.compile()
