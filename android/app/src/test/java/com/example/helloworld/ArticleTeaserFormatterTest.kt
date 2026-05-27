package com.example.helloworld

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse

class ArticleTeaserFormatterTest {
    @Test
    fun defaultTeaser_isBreakingNews() {
        assertEquals("Breaking news", ArticleTeaserFormatter.DEFAULT_TEASER)
    }

    @Test
    fun headline_blankTeaser_returnsDefaultTeaser() {
        val result = ArticleTeaserFormatter.headline("")
        assertEquals("Breaking news", result)
    }

    @Test
    fun headline_oversizedTeaser_trimsAndAddsEllipsis() {
        // headline takes(maxLen - 1) and appends a single-char ellipsis "…".
        // maxLen=20 → take(19)="This is a very long" + "…" → 20 chars total.
        val result = ArticleTeaserFormatter.headline("This is a very long teaser that exceeds the max length set.", 20)
        assertEquals("This is a very long…", result)
    }

    @Test(expected = IllegalArgumentException::class)
    fun headline_zeroMaxLength_throwsException() {
        ArticleTeaserFormatter.headline("Valid teaser", 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun headline_negativeMaxLength_throwsException() {
        ArticleTeaserFormatter.headline("Valid teaser", -5)
    }

    @Test
    fun headline_validTeaser_returnsTeaser() {
        val result = ArticleTeaserFormatter.headline("Hello World!", 50)
        assertEquals("Hello World!", result)
    }

    @Test
    fun headlineWithMetadata_shortTeaser_notTruncated() {
        val result = ArticleTeaserFormatter.headlineWithMetadata("Hello World!", 50)
        assertEquals("Hello World!", result.text)
        assertEquals(false, result.wasTruncated)
    }

    @Test
    fun headlineWithMetadata_oversizedTeaser_isTruncated() {
        val result = ArticleTeaserFormatter.headlineWithMetadata(
            "This is a very long teaser that exceeds the max length set.",
            20,
        )
        assertEquals("This is a very long…", result.text)
        assertEquals(true, result.wasTruncated)
    }

    @Test
    fun headlineResult_equalsHashCodeContract() {
        val result1 = ArticleTeaserFormatter.HeadlineResult("a", false)
        val result2 = ArticleTeaserFormatter.HeadlineResult("a", false)
        val result3 = ArticleTeaserFormatter.HeadlineResult("a", true)

        assertTrue(result1 == result2)
        assertFalse(result1 == result3)
        assertFalse(result2 == result3)
    }
}
