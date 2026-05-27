package com.example.helloworld

/**
 * Sanitizes raw search input for the in-app search bar.
 *
 * Rules (in order):
 *  - Trim surrounding whitespace.
 *  - Collapse runs of internal whitespace into a single space.
 *  - Strip ASCII punctuation (anything in [!"#$%&'()*+,\-./:;<=>?@\[\\\]^_`{|}~]).
 *  - Lowercase using [java.util.Locale.ROOT] for stable, locale-independent behavior.
 *  - Cap the result to [maxLen] characters, preferring a word boundary when the
 *    cut would otherwise split a token (no ellipsis — the search index handles
 *    truncation on its end).
 *
 * Pure JVM logic — unit-testable without instrumentation.
 */
object SearchQuerySanitizer {
    const val DEFAULT_MAX_LEN: Int = 80

    /** Returned when the input is null, empty, or only whitespace/punctuation. */
    const val EMPTY_QUERY: String = ""

    data class SanitizedQuery(
        val text: String,
        val wasTruncated: Boolean,
    )

    private val PUNCTUATION_REGEX = Regex("""[\p{Punct}]""")
    private val WHITESPACE_RUN_REGEX = Regex("""\s+""")

    fun sanitize(raw: String?, maxLen: Int = DEFAULT_MAX_LEN): String {
        require(maxLen > 0) { "maxLen must be positive" }
        if (raw.isNullOrBlank()) return EMPTY_QUERY

        val noPunct = PUNCTUATION_REGEX.replace(raw, " ")
        val collapsed = WHITESPACE_RUN_REGEX.replace(noPunct, " ").trim()
        if (collapsed.isEmpty()) return EMPTY_QUERY

        val lowered = collapsed.lowercase(java.util.Locale.ROOT)
        return truncateToMaxLen(lowered, maxLen)
    }

    fun sanitizeWithMetadata(raw: String?, maxLen: Int = DEFAULT_MAX_LEN): SanitizedQuery {
        require(maxLen > 0) { "maxLen must be positive" }
        if (raw.isNullOrBlank()) return SanitizedQuery(text = EMPTY_QUERY, wasTruncated = false)

        val noPunct = PUNCTUATION_REGEX.replace(raw, " ")
        val collapsed = WHITESPACE_RUN_REGEX.replace(noPunct, " ").trim()
        if (collapsed.isEmpty()) return SanitizedQuery(text = EMPTY_QUERY, wasTruncated = false)

        val lowered = collapsed.lowercase(java.util.Locale.ROOT)
        if (lowered.length <= maxLen) return SanitizedQuery(text = lowered, wasTruncated = false)

        val truncated = truncateToMaxLen(lowered, maxLen)
        return SanitizedQuery(text = truncated, wasTruncated = truncated.length < lowered.length)
    }

    private fun truncateToMaxLen(lowered: String, maxLen: Int): String {
        if (lowered.length <= maxLen) return lowered
        val chunk = lowered.take(maxLen)
        val lastSpace = chunk.lastIndexOf(' ')
        if (lastSpace > 0) {
            return chunk.take(lastSpace)
        }
        return chunk
    }

    /** Returns true if the sanitized form of [raw] is non-empty. */
    fun isValid(raw: String?, maxLen: Int = DEFAULT_MAX_LEN): Boolean =
        sanitize(raw, maxLen).isNotEmpty()
}
