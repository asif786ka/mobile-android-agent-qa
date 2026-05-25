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
    p.add_argument(
        "--save-findings",
        metavar="PATH",
        help="After review, write the structured findings JSON to this path "
             "so a downstream step (test generator) can consume it directly.",
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

    if args.save_findings:
        out_path = Path(args.save_findings)
        out_path.parent.mkdir(parents=True, exist_ok=True)
        out_path.write_text(
            json.dumps(final.get("result", {}), indent=2),
            encoding="utf-8",
        )
        print(f"Saved findings to {out_path}")

    body = final.get("comment_body", "")
    if args.dry_run or args.diff_file:
        # Local / dry-run: just print the rendered comment.
        print(body)
    else:
        # In Actions, the post step already ran; emit a short log line.
        print(f"Posted review on {final['pr_context'].repo}#{final['pr_context'].number}")

    # Refresh USAGE.md (month-to-date) + this run's cost summary. Best-effort.
    try:
        from tools.usage_tracker import write_usage_md, write_run_cost
        write_usage_md()
        write_run_cost()
    except Exception as exc:  # noqa: BLE001
        print(f"[main] usage rollup failed (non-fatal): {exc}")

    return 0


if __name__ == "__main__":
    sys.exit(main())
