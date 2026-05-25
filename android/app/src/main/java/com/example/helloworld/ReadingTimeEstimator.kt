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

    fun format(
        text: String?,
        wordsPerMinute: Int = DEFAULT_WORDS_PER_MINUTE,
    ): String {
        val minutes = minutesToRead(text, wordsPerMinute)
        return when (minutes) {
            0 -> MIN_READ_LABEL
            1 -> "1 min read"
            else -> "$minutes min read"
        }
    }
}
