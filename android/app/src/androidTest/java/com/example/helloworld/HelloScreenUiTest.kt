package com.example.helloworld

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test

class HelloScreenUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun helloText_isDisplayed_withDefaultName() {
        composeRule.setContent { HelloScreen() }

        composeRule
            .onNodeWithTag(HELLO_TEXT_TAG)
            .assertIsDisplayed()
            .assertTextEquals("Hello World")
    }

    @Test
    fun helloText_reflectsProvidedName() {
        composeRule.setContent { HelloScreen(name = "Asif") }

        composeRule
            .onNodeWithTag(HELLO_TEXT_TAG)
            .assertIsDisplayed()
            .assertTextEquals("Hello Asif")
    }
}
