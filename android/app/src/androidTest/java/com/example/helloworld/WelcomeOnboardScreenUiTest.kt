package com.example.helloworld

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test

class WelcomeOnboardScreenUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun welcomeText_displaysCorrectMessageForAlice() {
        composeRule.setContent { WelcomeOnboardScreen(name = "Alice") }
        composeRule.onNodeWithTag(WELCOME_TEXT_TAG)
            .assertIsDisplayed()
            .assertTextEquals("Welcome aboard, Alice!")
    }

    @Test
    fun welcomeText_displaysDefaultMessageWhenNameIsBlank() {
        composeRule.setContent { WelcomeOnboardScreen(name = "") }
        composeRule.onNodeWithTag(WELCOME_TEXT_TAG)
            .assertIsDisplayed()
            .assertTextEquals("Welcome aboard, ${Greeting.DEFAULT_NAME}!")
    }
}
