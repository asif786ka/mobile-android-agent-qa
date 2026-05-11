package com.example.helloworld

/**
 * Pure logic for the greeting text. Kept separate from Compose so it can
 * be unit-tested without instrumentation.
 */
object Greeting {
    const val DEFAULT_NAME: String = "World"

    fun greet(name: String = DEFAULT_NAME): String {
        val trimmed = name.trim()
        val who = if (trimmed.isEmpty()) DEFAULT_NAME else trimmed
        return "Hello $who"
    }
}
