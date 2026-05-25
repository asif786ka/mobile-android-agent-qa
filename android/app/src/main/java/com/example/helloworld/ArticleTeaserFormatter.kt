package com.example.helloworld

/**
 * Formats article teaser copy for mobile headline strips (news app style).
 * Pure JVM logic — unit-testable without instrumentation.
 *
 * Shipped without tests on the demo PR so the AI QA agent can flag coverage gaps.
 */
object ArticleTeaserFormatter {
    const val DEFAULT_TEASER: String = "Breaking news"
    private const val ELLIPSIS = "…"

    fun headline(teaser: String, maxLen: Int = 80): String {
        require(maxLen > 0) { "maxLen must be positive" }
        val trimmed = teaser.trim()
        val base = if (trimmed.isEmpty()) DEFAULT_TEASER else trimmed
        if (base.length <= maxLen) {
            return base
        }
        val cutoff = (maxLen - 1).coerceAtLeast(1)
        return base.take(cutoff) + ELLIPSIS
    }
}
