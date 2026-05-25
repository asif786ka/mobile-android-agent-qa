package com.example.helloworld

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test

class ReadingTimeScreenUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun readingTime_displaysFormattedLabel() {
        val article = "One two three four five six seven eight nine ten."
        composeRule.setContent { ReadingTimeScreen(articleText = article) }
        composeRule.onNodeWithTag(READING_TIME_TAG)
            .assertIsDisplayed()
            .assertTextEquals("1 min read")
    }

    @Test
    fun readingTime_emptyArticle_showsMinReadLabel() {
        composeRule.setContent { ReadingTimeScreen(articleText = "") }
        composeRule.onNodeWithTag(READING_TIME_TAG)
            .assertIsDisplayed()
            .assertTextEquals(ReadingTimeEstimator.MIN_READ_LABEL)
    }
}
