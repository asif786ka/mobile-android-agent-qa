package com.example.helloworld

import org.junit.Test
import org.junit.Assert.assertEquals
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
    fun headlineResult_equalsComparison_withSameTextAndWasTruncated() {
        val result1 = ArticleTeaserFormatter.HeadlineResult(text = "Sample Teaser", wasTruncated = true)
        val result2 = ArticleTeaserFormatter.HeadlineResult(text = "Sample Teaser", wasTruncated = true)
        assertEquals(result1, result2)
    }

    @Test
    fun headlineResult_equalsComparison_withDifferentTextOrWasTruncated() {
        val result1 = ArticleTeaserFormatter.HeadlineResult(text = "Sample Teaser", wasTruncated = true)
        val result2 = ArticleTeaserFormatter.HeadlineResult(text = "Sample Teaser", wasTruncated = false)
        assertFalse(result1 == result2)
    }

    @Test
    fun headlineResult_copy_createsIndependentCopy() {
        val original = ArticleTeaserFormatter.HeadlineResult(text = "Sample Teaser", wasTruncated = false)
        val copy = original.copy()
        assertEquals(original, copy)
        assertFalse(original === copy) // Ensure different references
    }

    @Test
    fun headlineResult_equalsComparison_withDifferentText() {
        val result1 = ArticleTeaserFormatter.HeadlineResult(text = "Different text", wasTruncated = false)
        val result2 = ArticleTeaserFormatter.HeadlineResult(text = "Another text", wasTruncated = false)
        assertFalse(result1 == result2)
    }

    @Test
    fun headlineResult_equalsComparison_withTruncatedStatus() {
        val result1 = ArticleTeaserFormatter.HeadlineResult(text = "Sample Teaser", wasTruncated = true)
        val result2 = ArticleTeaserFormatter.HeadlineResult(text = "Sample Teaser", wasTruncated = false)
        assertFalse(result1 == result2)
    }

    @Test
    fun headlineResult_equalsComparison_withSameTextAndDifferentWasTruncated() {
        val result1 = ArticleTeaserFormatter.HeadlineResult(text = "Same text", wasTruncated = false)
        val result2 = ArticleTeaserFormatter.HeadlineResult(text = "Same text", wasTruncated = true)
        assertFalse(result1 == result2)
    }
}