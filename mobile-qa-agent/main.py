"""Entrypoint for the mobile QA agent.

Two modes:
  • Inside GitHub Actions: reads $GITHUB_EVENT_PATH, fetches PR diff, posts a
    review comment.
  • Locally (--diff-file): reads a diff from disk, prints the formatted comment
    and (by default) does NOT post anything.
"""
from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path

# Load .env from this folder if present (no-op in CI, where vars are set
# directly by the workflow). Imported defensively so the agent still runs
# even if python-dotenv isn't installed.
try:
    from dotenv import load_dotenv
    load_dotenv(Path(__file__).resolve().parent / ".env")
except ImportError:
    pass

from graph import build_graph


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="Mobile QA review agent.")
    p.add_argument(
        "--diff-file",
        help="Path to a local .diff file to review (skips GitHub fetch).",
    )
    p.add_argument(
        "--dry-run",
        action="store_true",
        help="Do not post a comment; print the result instead.",
    )
    p.add_argument(
        "--provider",
        choices=["anthropic", "openai", "bedrock"],
        help="LLM provider to use. Overrides $PROVIDER.",
    )
    p.add_argument(
        "--print-json",
        action="store_true",
        help="Also print the raw structured result as JSON.",
    )
    return p.parse_args()


def main() -> int:
    args = parse_args()

    graph = build_graph()
    final = graph.invoke(
        {
            "diff_file": args.diff_file,
            "dry_run": bool(args.dry_run or args.diff_file),
            "provider_name": args.provider or os.environ.get("PROVIDER"),
        }
    )

    if args.print_json:
        print(json.dumps(final.get("result", {}), indent=2))

    body = final.get("comment_body", "")
    if args.dry_run or args.diff_file:
        # Local / dry-run: just print the rendered comment.
        print(body)
    else:
        # In Actions, the post step already ran; emit a short log line.
        print(f"Posted review on {final['pr_context'].repo}#{final['pr_context'].number}")

    return 0


if __name__ == "__main__":
    sys.exit(main())
