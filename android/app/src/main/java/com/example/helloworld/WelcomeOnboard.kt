package com.example.helloworld

/**
 * Companion to [Greeting]. Intentionally shipped without a unit test so the
 * AI QA agent has something concrete to flag in the demo PR.
 */
object WelcomeOnboard {
    fun welcome(name: String = Greeting.DEFAULT_NAME): String {
        val trimmed = name.trim()
        val who = if (trimmed.isEmpty()) Greeting.DEFAULT_NAME else trimmed
        return "Welcome aboard, $who!"
    }
}
