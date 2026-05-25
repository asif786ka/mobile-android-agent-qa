package com.example.helloworld

import androidx.compose.ui.test.assertDoesNotExist
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
            .assertTextEquals("Start typing to search…")
    }

    @Test
    fun searchSanitizedText_reflectsSanitizedInput_withSpecialCharacters() {
        val rawQuery = "  Breaking!! NEWS,, today??  "
        composeRule.setContent { SearchScreen(rawQuery = rawQuery) }
        composeRule.onNodeWithTag(SEARCH_SANITIZED_TAG)
            .assertIsDisplayed()
            .assertTextEquals("breaking news today")
    }

    @Test
    fun searchSanitizedText_doesNotShowOldPrompt_whenEmptyInput() {
        composeRule.setContent { SearchScreen() }
        composeRule.onNodeWithTag(SEARCH_SANITIZED_TAG)
            .assertIsDisplayed()
            .assertTextEquals("Start typing to search…")
        composeRule.onNodeWithTag(SEARCH_SANITIZED_TAG)
            .assertTextEquals("Type to search…")
            .assertIsDisplayed().assertDoesNotExist()
    }

    @Test
    fun searchSanitizedText_truncatesLongQueriesCorrectly() {
        val rawQuery = "This is a long search query that is meant to exceed limits"
        composeRule.setContent { SearchScreen(rawQuery = rawQuery, maxLen = 30) }
        composeRule.onNodeWithTag(SEARCH_SANITIZED_TAG)
            .assertIsDisplayed()
            .assertTextEquals("This is a long search")
    }

    @Test
    fun searchSanitizedText_doesNotShowOldPrompt_whenQueryIsProvided() {
        val rawQuery = "Hello"
        composeRule.setContent { SearchScreen(rawQuery = rawQuery) }
        composeRule.onNodeWithTag(SEARCH_SANITIZED_TAG)
            .assertIsDisplayed()
            .assertTextEquals("hello")
        composeRule.onNodeWithTag(SEARCH_SANITIZED_TAG)
            .assertTextEquals("Type to search…")
            .assertDoesNotExist()
    }

    @Test
    fun searchSanitizedText_doesNotShowOldPrompt_whenEmptyRawQuery() {
        composeRule.setContent { SearchScreen(rawQuery = "") }
        composeRule.onNodeWithTag(SEARCH_SANITIZED_TAG)
            .assertIsDisplayed()
            .assertTextEquals("Start typing to search…")
        composeRule.onNodeWithTag(SEARCH_SANITIZED_TAG)
            .assertTextEquals("Type to search…")
            .assertDoesNotExist()
    }

    @Test
    fun searchSanitizedText_doesNotDisplayPlaceholder_whenValidQueryProvided() {
        val rawQuery = "Valid Input"
        composeRule.setContent { SearchScreen(rawQuery = rawQuery) }
        composeRule.onNodeWithTag(SEARCH_SANITIZED_TAG)
            .assertIsDisplayed()
            .assertTextEquals("valid input")
        composeRule.onNodeWithTag(SEARCH_SANITIZED_TAG)
            .assertTextEquals("Start typing to search…")
            .assertDoesNotExist()
    }
}