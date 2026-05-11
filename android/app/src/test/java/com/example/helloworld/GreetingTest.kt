package com.example.helloworld

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class GreetingTest {

    @Test
    fun greet_defaultsToWorld() {
        assertEquals("Hello World", Greeting.greet())
    }

    @Test
    fun greet_withName_includesName() {
        assertEquals("Hello Asif", Greeting.greet("Asif"))
    }

    @Test
    fun greet_blankName_fallsBackToWorld() {
        assertEquals("Hello World", Greeting.greet("   "))
    }

    @Test
    fun greet_trimsName() {
        assertEquals("Hello Asif", Greeting.greet("  Asif  "))
    }

    @Test
    fun greet_neverReturnsEmpty() {
        assertTrue(Greeting.greet("").isNotEmpty())
    }
}
