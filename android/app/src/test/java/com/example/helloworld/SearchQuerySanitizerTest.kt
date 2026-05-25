package com.example.helloworld

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows

class SearchQuerySanitizerTest {

    @Test
    fun sanitize_validInput_returnsSanitizedString() {
        val result = SearchQuerySanitizer.sanitize("   Hello, World!   ")
        assertEquals("hello world", result)
    }

    @Test
    fun sanitize_emptyInput_returnsEmptyQuery() {
        val result = SearchQuerySanitizer.sanitize("")
        assertEquals(SearchQuerySanitizer.EMPTY_QUERY, result)
    }

    @Test
    fun sanitize_nullInput_returnsEmptyQuery() {
        val result = SearchQuerySanitizer.sanitize(null)
        assertEquals(SearchQuerySanitizer.EMPTY_QUERY, result)
    }

    @Test
    fun sanitize_inputWithOnlyPunctuation_returnsEmptyQuery() {
        val result = SearchQuerySanitizer.sanitize("!!!??")
        assertEquals(SearchQuerySanitizer.EMPTY_QUERY, result)
    }

    @Test
    fun sanitize_tooShortMaxLen_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            SearchQuerySanitizer.sanitize("valid input", 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SearchQuerySanitizer.sanitize("valid input", -1)
        }
    }

    @Test
    fun sanitize_maxLenLimitsOutput() {
        // Sanitizer pipeline: trim → strip punct → collapse whitespace → lowercase → take(maxLen).
        // Input has no punctuation and is already lowercase, so the truncation is exact: 10 chars.
        val result = SearchQuerySanitizer.sanitize("a very long input string that exceeds the maximum length", 10)
        assertEquals("a very lon", result)
    }
}