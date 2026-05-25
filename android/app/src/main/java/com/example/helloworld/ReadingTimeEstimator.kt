package com.example.helloworld

/**
 * Estimates article reading time from raw body text (news app style).
 * Pure JVM logic — unit-testable without instrumentation.
 */
object ReadingTimeEstimator {
    const val DEFAULT_WORDS_PER_MINUTE: Int = 200
    const val MIN_READ_LABEL: String = "< 1 min read"

    fun wordCount(text: String?): Int {
        if (text.isNullOrBlank()) return 0
        return text.trim().split(Regex("""\s+""")).count { it.isNotEmpty() }
    }

    fun minutesToRead(text: String?, wordsPerMinute: Int = DEFAULT_WORDS_PER_MINUTE): Int {
        require(wordsPerMinute > 0) { "wordsPerMinute must be positive" }
        val words = wordCount(text)
        if (words == 0) return 0
        return ((words + wordsPerMinute - 1) / wordsPerMinute).coerceAtLeast(1)
    }

    /**
     * Render a human-friendly reading-time label.
     *
     * Behaviour:
     *  - When [longLabel] is `false` (the default), the result uses the
     *    compact "min" form: `"< 1 min read"`, `"1 min read"`,
     *    `"<n> min read"`. This is the original behaviour, preserved for
     *    backwards compatibility.
     *  - When [longLabel] is `true`, the result uses the long-form
     *    "minute" / "minutes" labels with correct singular/plural:
     *    `"< 1 minute read"`, `"1 minute read"`, `"<n> minutes read"`.
     *
     * @param text raw article body; null/blank → less-than-one-minute label.
     * @param wordsPerMinute must be positive.
     * @param longLabel pick the long-form labels.
     */
    fun format(
        text: String?,
        wordsPerMinute: Int = DEFAULT_WORDS_PER_MINUTE,
        longLabel: Boolean = false,
    ): String {
        val minutes = minutesToRead(text, wordsPerMinute)
        return if (longLabel) {
            when (minutes) {
                0 -> "< 1 minute read"
                1 -> "1 minute read"
                else -> "$minutes minutes read"
            }
        } else {
            when (minutes) {
                0 -> MIN_READ_LABEL
                1 -> "1 min read"
                else -> "$minutes min read"
            }
        }
    }
}
