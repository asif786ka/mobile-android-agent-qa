package com.example.helloworld

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
        val defaultTeaser = "No teaser available"
        composeRule.setContent { ArticleTeaserScreen(teaser = "") }
        composeRule.onNodeWithTag(ARTICLE_TEASER_TAG)
            .assertIsDisplayed()
            .assertTextEquals(defaultTeaser)
    }

    @Test
    fun articleTeaser_truncatesLongTeaser() {
        val longTeaser = "This is a very long headline that exceeds the maximum length imposed by the application and should be truncated."
        val expectedTeaser = "This is a very long headline that exceeds the maximum length imposed by..."
        composeRule.setContent { ArticleTeaserScreen(teaser = longTeaser, maxHeadlineLength = 80) }
        composeRule.onNodeWithTag(ARTICLE_TEASER_TAG)
            .assertIsDisplayed()
            .assertTextEquals(expectedTeaser)
    }

    @Test
    fun articleTeaser_hasContentDescription() {
        val teaser = "Important update on the situation"
        composeRule.setContent { ArticleTeaserScreen(teaser = teaser) }
        composeRule.onNodeWithTag(ARTICLE_TEASER_TAG)
            .assertIsDisplayed()
            .assertHasContentDescription(teaser)
    }
}