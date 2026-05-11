"""Heuristics for detecting Android changes and recommending test frameworks."""
from __future__ import annotations

from typing import Iterable


_ANDROID_FILE_HINTS = (".kt", ".kts", ".java", ".xml")
_ANDROID_PATH_HINTS = ("android/", "/AndroidManifest.xml", "src/main/", "src/test/", "src/androidTest/")


def detect_android(files: Iterable[str], diff: str = "") -> dict:
    """Return a structured report about Android-ness of the change set."""
    files = list(files)
    kt = [f for f in files if f.endswith(".kt")]
    java = [f for f in files if f.endswith(".java")]
    xml = [f for f in files if f.endswith(".xml") and "android" in f.lower()]
    gradle = [f for f in files if f.endswith((".gradle", ".gradle.kts"))]

    has_android = bool(
        kt
        or java
        or gradle
        or any(any(h in f for h in _ANDROID_PATH_HINTS) for f in files)
    )

    has_compose = any("@Composable" in diff for _ in [0]) or any(
        "androidx.compose" in diff for _ in [0]
    )
    has_unit_tests = any("/src/test/" in f for f in files)
    has_ui_tests = any("/src/androidTest/" in f for f in files)

    return {
        "platform": "android" if has_android else "unknown",
        "is_android": has_android,
        "kotlin_files": kt,
        "java_files": java,
        "android_xml_files": xml,
        "gradle_files": gradle,
        "signals": {
            "uses_compose": has_compose,
            "has_unit_tests": has_unit_tests,
            "has_ui_tests": has_ui_tests,
        },
        "frameworks": [
            "JUnit",
            "Mockito" if (kt or java) else None,
            "Compose UI Test" if has_compose else "Espresso",
        ],
    }
