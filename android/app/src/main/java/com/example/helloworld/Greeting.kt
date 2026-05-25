package com.example.helloworld

/**
 * Pure logic for the greeting text. Kept separate from Compose so it can
 * be unit-tested without instrumentation.
 */
object Greeting {
    const val DEFAULT_NAME: String = "World"
    const val DEFAULT_GREETING: String = "Hello"

    /**
     * Build the rendered greeting line.
     *
     * Behaviour:
     *  - [name] is trimmed; blank/empty falls back to [DEFAULT_NAME].
     *  - [greeting] is trimmed; blank/empty falls back to [DEFAULT_GREETING].
     *  - The result is always `"<greeting> <name>"` with exactly one space
     *    between the two parts and no surrounding whitespace.
     *
     * The default-args call shapes (`greet()` and `greet("Asif")`) are
     * source-compatible with the previous single-argument version, so
     * existing callers continue to render `"Hello World"` / `"Hello Asif"`.
     */
    fun greet(
        name: String = DEFAULT_NAME,
        greeting: String = DEFAULT_GREETING,
    ): String {
        val trimmedName = name.trim()
        val who = if (trimmedName.isEmpty()) DEFAULT_NAME else trimmedName
        val trimmedGreeting = greeting.trim()
        val verb = if (trimmedGreeting.isEmpty()) DEFAULT_GREETING else trimmedGreeting
        return "$verb $who"
    }
}
