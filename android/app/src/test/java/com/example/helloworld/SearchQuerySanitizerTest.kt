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
        // Pipeline ends with word-boundary truncation when the maxLen cut lands inside a token.
        val result = SearchQuerySanitizer.sanitize("a very long input string that exceeds the maximum length", 10)
        assertEquals("a very", result)
    }

    @Test
    fun sanitize_blankInput_returnsEmptyQuery() {
        val result = SearchQuerySanitizer.sanitize("   ")
        assertEquals(SearchQuerySanitizer.EMPTY_QUERY, result)
    }

    @Test
    fun sanitize_whitespaceOnly_returnsEmptyQuery() {
        val result = SearchQuerySanitizer.sanitize("     ")
        assertEquals(SearchQuerySanitizer.EMPTY_QUERY, result)
    }

    @Test
    fun sanitize_leadingAndTrailingWhitespace_trimmed() {
        val result = SearchQuerySanitizer.sanitize("  giraffe!   ")
        assertEquals("giraffe", result)
    }

    @Test
    fun sanitize_internalWhitespace_collapsed() {
        val result = SearchQuerySanitizer.sanitize("hello    world")
        assertEquals("hello world", result)
    }

    @Test
    fun sanitize_asciiPunctuation_stripped() {
        val result = SearchQuerySanitizer.sanitize("this is a test!")
        assertEquals("this is a test", result)
    }

    @Test
    fun sanitize_customMaxLen_truncationWithWordBoundary() {
        val result = SearchQuerySanitizer.sanitize("The quick brown fox jumps over the lazy dog", 19)
        assertEquals("the quick brown", result)
    }

    @Test
    fun sanitize_customMaxLen_zeroLength_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            SearchQuerySanitizer.sanitize("input", 0)
        }
    }

    @Test
    fun sanitize_customMaxLen_oneLength_truncatesSingleCharacter() {
        val result = SearchQuerySanitizer.sanitize("a very long input string", 1)
        assertEquals("a", result)
    }

    @Test
    fun sanitize_customMaxLen_truncationWithNoWordBoundary() {
        val result = SearchQuerySanitizer.sanitize("quick brown fox jumps", 5)
        assertEquals("quick", result)
    }

    @Test
    fun sanitize_leadingTrailingSpacesAndPunctuation_returnsSanitizedString() {
        val result = SearchQuerySanitizer.sanitize("!! hello   world!  !!")
        assertEquals("hello world", result)
    }

    @Test
    fun isValid_nonEmptyInput_returnsTrue() {
        val result = SearchQuerySanitizer.isValid("  Hello World!  ")
        assertEquals(true, result)
    }

    @Test
    fun isValid_emptyInput_returnsFalse() {
        val result = SearchQuerySanitizer.isValid("")
        assertEquals(false, result)
    }

    @Test
    fun isValid_nullInput_returnsFalse() {
        val result = SearchQuerySanitizer.isValid(null)
        assertEquals(false, result)
    }

    @Test
    fun isValid_inputWithOnlyPunctuation_returnsFalse() {
        val result = SearchQuerySanitizer.isValid("!!!??")
        assertEquals(false, result)
    }
}