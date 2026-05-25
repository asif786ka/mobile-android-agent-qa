package com.example.helloworld

/**
 * Sanitizes raw search input for the in-app search bar.
 *
 * Rules (in order):
 *  - Trim surrounding whitespace.
 *  - Collapse runs of internal whitespace into a single space.
 *  - Strip ASCII punctuation (anything in [!"#$%&'()*+,\-./:;<=>?@\[\\\]^_`{|}~]).
 *  - Lowercase using [java.util.Locale.ROOT] for stable, locale-independent behavior.
 *  - Cap the result to [maxLen] characters (no ellipsis — the search index handles
 *    truncation on its end).
 *
 * Pure JVM logic — unit-testable without instrumentation.
 */
object SearchQuerySanitizer {
    const val DEFAULT_MAX_LEN: Int = 64

    /** Returned when the input is null, empty, or only whitespace/punctuation. */
    const val EMPTY_QUERY: String = ""

    private val PUNCTUATION_REGEX = Regex("""[\p{Punct}]""")
    private val WHITESPACE_RUN_REGEX = Regex("""\s+""")

    fun sanitize(raw: String?, maxLen: Int = DEFAULT_MAX_LEN): String {
        require(maxLen > 0) { "maxLen must be positive" }
        if (raw.isNullOrBlank()) return EMPTY_QUERY

        val noPunct = PUNCTUATION_REGEX.replace(raw, " ")
        val collapsed = WHITESPACE_RUN_REGEX.replace(noPunct, " ").trim()
        if (collapsed.isEmpty()) return EMPTY_QUERY

        val lowered = collapsed.lowercase(java.util.Locale.ROOT)
        return if (lowered.length <= maxLen) lowered else lowered.take(maxLen)
    }

    /** Returns true if the sanitized form of [raw] is non-empty. */
    fun isValid(raw: String?, maxLen: Int = DEFAULT_MAX_LEN): Boolean =
        sanitize(raw, maxLen).isNotEmpty()
}
