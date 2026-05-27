package com.example.helloworld

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test

class ArticleTeaserScreenUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun articleTeaser_displaysTeaserText() {
        val teaser = "Breaking news headline"
        composeRule.setContent { ArticleTeaserScreen(teaser = teaser) }
        composeRule.onNodeWithTag(ARTICLE_TEASER_TAG)
            .assertIsDisplayed()
            .assertTextEquals(teaser)
    }

    @Test
    fun articleTeaser_displaysDefaultTeaserForEmptyInput() {
        // The empty/whitespace fallback is the production constant.
        composeRule.setContent { ArticleTeaserScreen(teaser = "") }
        composeRule.onNodeWithTag(ARTICLE_TEASER_TAG)
            .assertIsDisplayed()
            .assertTextEquals(ArticleTeaserFormatter.DEFAULT_TEASER)
    }

    @Test
    fun articleTeaser_truncatesLongTeaser() {
        // ArticleTeaserFormatter cuts at (maxLen - 1) chars and appends the
        // single-char ellipsis "…" (NOT three dots). For maxLen=80 the result
        // is take(79) of the input + "…".
        val longTeaser =
            "This is a very long headline that exceeds the maximum length imposed by the application and should be truncated."
        val expectedTeaser =
            "This is a very long headline that exceeds the maximum length imposed by the app…"
        composeRule.setContent { ArticleTeaserScreen(teaser = longTeaser, maxHeadlineLength = 80) }
        composeRule.onNodeWithTag(ARTICLE_TEASER_TAG)
            .assertIsDisplayed()
            .assertTextEquals(expectedTeaser)
    }

    @Test
    fun articleTeaser_displaysTruncatedTagWhenTeaserExceedsMaxLength() {
        val longTeaser = "This is a long headline that will be truncated"
        composeRule.setContent { ArticleTeaserScreen(teaser = longTeaser, maxHeadlineLength = 50) }
        composeRule.onNodeWithTag(ARTICLE_TEASER_TRUNCATED_TAG)
            .assertIsDisplayed()
            .assertTextEquals("Headline shortened to 50 characters.")
    }

    @Test
    fun articleTeaser_doesNotDisplayTruncatedTagWhenTeaserFits() {
        val shortTeaser = "This fits"
        composeRule.setContent { ArticleTeaserScreen(teaser = shortTeaser, maxHeadlineLength = 50) }
        composeRule.onNodeWithTag(ARTICLE_TEASER_TRUNCATED_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun articleTeaser_doesNotDisplayTruncatedTagForExactLength() {
        val exactLengthTeaser = "Exactly maximum length teaser text, which is 50 ch..."
        composeRule.setContent { ArticleTeaserScreen(teaser = exactLengthTeaser, maxHeadlineLength = 50) }
        composeRule.onNodeWithTag(ARTICLE_TEASER_TRUNCATED_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun articleTeaser_displaysTruncatedTagWithCorrectTextWhenTeaserIsLongerThanMaxLength() {
        val longTeaser = "This teaser is definitely more than eighty characters long and should be truncated."
        composeRule.setContent { ArticleTeaserScreen(teaser = longTeaser, maxHeadlineLength = 80) }
        composeRule.onNodeWithTag(ARTICLE_TEASER_TRUNCATED_TAG)
            .assertIsDisplayed()
            .assertTextEquals("Headline shortened to 80 characters.")
    }

    @Test
    fun articleTeaser_doesNotDisplayTruncatedTagWhenTeaserExactlyEqualsMaxLength() {
        val exactLengthTeaser = "This teaser exactly matches eighty characters in length and should not show the truncated tag."
        composeRule.setContent { ArticleTeaserScreen(teaser = exactLengthTeaser, maxHeadlineLength = 80) }
        composeRule.onNodeWithTag(ARTICLE_TEASER_TRUNCATED_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun articleTeaser_displaysTruncatedTagWithCorrectTextWhenTeaserExceedsMaxLengthBoundary() {
        val longTeaser = "This is a long headline that exceeds the maximum length imposed by the application and should be truncated."
        composeRule.setContent { ArticleTeaserScreen(teaser = longTeaser, maxHeadlineLength = 80) }
        composeRule.onNodeWithTag(ARTICLE_TEASER_TRUNCATED_TAG)
            .assertIsDisplayed()
            .assertTextEquals("Headline shortened to 80 characters.")
    }

    @Test
    fun articleTeaser_doesNotDisplayTruncatedTagOnBoundaryLength() {
        val boundaryTeaser = "This teaser is exactly eighty characters long and should not show the truncated tag."
        composeRule.setContent { ArticleTeaserScreen(teaser = boundaryTeaser, maxHeadlineLength = 80) }
        composeRule.onNodeWithTag(ARTICLE_TEASER_TRUNCATED_TAG)
            .assertDoesNotExist()
    }

    @Test
    fun articleTeaser_displaysDefaultTeaserForEmptyInputAndNoTruncatedTag() {
        composeRule.setContent { ArticleTeaserScreen(teaser = "") }
        composeRule.onNodeWithTag(ARTICLE_TEASER_TAG)
            .assertIsDisplayed()
            .assertTextEquals(ArticleTeaserFormatter.DEFAULT_TEASER)
        composeRule.onNodeWithTag(ARTICLE_TEASER_TRUNCATED_TAG)
            .assertDoesNotExist()
    }
}