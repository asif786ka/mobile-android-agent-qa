package com.example.helloworld

import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingTimeEstimatorTest {
    @Test
    fun wordCount_blankText_returnsZero() {
        assertEquals(0, ReadingTimeEstimator.wordCount(""))
        assertEquals(0, ReadingTimeEstimator.wordCount("   "))
        assertEquals(0, ReadingTimeEstimator.wordCount(null))
    }

    @Test
    fun wordCount_collapsesInternalWhitespace() {
        assertEquals(3, ReadingTimeEstimator.wordCount("  one   two\tthree  "))
    }

    @Test
    fun minutesToRead_shortArticle_returnsOneMinute() {
        assertEquals(1, ReadingTimeEstimator.minutesToRead("Ten word article here today now."))
    }

    @Test
    fun minutesToRead_longArticle_roundsUp() {
        val words = List(401) { "word" }.joinToString(" ")
        assertEquals(3, ReadingTimeEstimator.minutesToRead(words, wordsPerMinute = 200))
    }

    @Test
    fun format_emptyText_returnsMinReadLabel() {
        assertEquals(ReadingTimeEstimator.MIN_READ_LABEL, ReadingTimeEstimator.format(""))
    }

    @Test
    fun format_singleMinute_usesSingularLabel() {
        assertEquals("1 min read", ReadingTimeEstimator.format("short piece"))
    }

    @Test
    fun format_multipleMinutes_usesPluralLabel() {
        val words = List(450) { "w" }.joinToString(" ")
        assertEquals("3 min read", ReadingTimeEstimator.format(words))
    }

    @Test(expected = IllegalArgumentException::class)
    fun minutesToRead_zeroWpm_throwsException() {
        ReadingTimeEstimator.minutesToRead("hello", wordsPerMinute = 0)
    }
}
