"""Heuristics for detecting iOS changes and recommending test frameworks."""
from __future__ import annotations

from typing import Iterable


def detect_ios(files: Iterable[str], diff: str = "") -> dict:
    files = list(files)
    swift = [f for f in files if f.endswith(".swift")]
    obj_c = [f for f in files if f.endswith((".m", ".mm", ".h"))]
    pbxproj = [f for f in files if f.endswith(".pbxproj")]
    plist = [f for f in files if f.endswith(".plist")]

    has_ios = bool(
        swift
        or obj_c
        or pbxproj
        or any(f.startswith("ios/") for f in files)
    )

    has_swiftui = "import SwiftUI" in diff or "@main" in diff
    has_xctest = any("Tests/" in f and f.endswith(".swift") for f in files)
    has_ui_tests = any("UITests" in f for f in files)

    return {
        "platform": "ios" if has_ios else "unknown",
        "is_ios": has_ios,
        "swift_files": swift,
        "objc_files": obj_c,
        "project_files": pbxproj,
        "plist_files": plist,
        "signals": {
            "uses_swiftui": has_swiftui,
            "has_unit_tests": has_xctest,
            "has_ui_tests": has_ui_tests,
        },
        "frameworks": [
            "XCTest",
            "XCUITest" if has_swiftui or has_ui_tests else None,
        ],
    }
