package com.example.helloworld

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test

class SearchScreenUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun searchSanitizedText_reflectsSanitizedInput() {
        val rawQuery = "  Hello!!  "
        composeRule.setContent { SearchScreen(rawQuery = rawQuery) }
        composeRule.onNodeWithTag(SEARCH_SANITIZED_TAG)
            .assertIsDisplayed()
            .assertTextEquals("hello")
    }

    @Test
    fun searchSanitizedText_displaysPrompt_whenEmptyInput() {
        composeRule.setContent { SearchScreen() }
        composeRule.onNodeWithTag(SEARCH_SANITIZED_TAG)
            .assertIsDisplayed()
            .assertTextEquals("Type to search…")
    }

    @Test
    fun searchSanitizedText_reflectsSanitizedInput_withSpecialCharacters() {
        val rawQuery = "  Breaking!! NEWS,, today??  "
        composeRule.setContent { SearchScreen(rawQuery = rawQuery) }
        composeRule.onNodeWithTag(SEARCH_SANITIZED_TAG)
            .assertIsDisplayed()
            .assertTextEquals("breaking news today")
    }
}