package com.example.helloworld

/**
 * Formats article teaser copy for mobile headline strips (news app style).
 * Pure JVM logic — unit-testable without instrumentation.
 */
object ArticleTeaserFormatter {
    const val DEFAULT_TEASER: String = "Breaking news"
    private const val ELLIPSIS = "…"

    data class HeadlineResult(
        val text: String,
        val wasTruncated: Boolean,
    )

    fun headline(teaser: String, maxLen: Int = 80): String =
        headlineWithMetadata(teaser, maxLen).text

    fun headlineWithMetadata(teaser: String, maxLen: Int = 80): HeadlineResult {
        require(maxLen > 0) { "maxLen must be positive" }
        val trimmed = teaser.trim()
        val base = if (trimmed.isEmpty()) DEFAULT_TEASER else trimmed
        if (base.length <= maxLen) {
            return HeadlineResult(text = base, wasTruncated = false)
        }
        val cutoff = (maxLen - 1).coerceAtLeast(1)
        return HeadlineResult(text = base.take(cutoff) + ELLIPSIS, wasTruncated = true)
    }
}
