package com.example.helloworld

import org.junit.Test
import org.junit.Assert.assertEquals

class WelcomeOnboardTest {
    @Test
    fun welcome_happyPath_returnsWelcomeMessage() {
        val result = WelcomeOnboard.welcome("Alice")
        assertEquals("Welcome aboard, Alice!", result)
    }

    @Test
    fun welcome_blankName_fallsBackToDefaultName() {
        val result = WelcomeOnboard.welcome("")
        assertEquals("Welcome aboard, ${Greeting.DEFAULT_NAME}!", result)
    }

    @Test
    fun welcome_whitespaceOnly_fallsBackToDefaultName() {
        val result = WelcomeOnboard.welcome("   ")
        assertEquals("Welcome aboard, ${Greeting.DEFAULT_NAME}!", result)
    }

    @Test
    fun welcome_multispaceInput_returnsTrimmedMessage() {
        val result = WelcomeOnboard.welcome("    Bob    ")
        assertEquals("Welcome aboard, Bob!", result)
    }
}