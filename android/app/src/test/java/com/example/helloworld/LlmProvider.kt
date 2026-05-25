package com.example.helloworld

interface LLMProvider {
    val name: String
    val last_usage: Map<String, Any?>?
    fun generate(prompt: String, maxTokens: Int = 8192): String
}

fun _retry_llm_for_valid_json(
    provider: LLMProvider,
    originalPrompt: String,
    brokenRaw: String,
    parseErrorMsg: String,
    maxTokens: Int,
): Map<String, Any?>? {
    val retryPrompt =
        "You previously returned a response to the prompt below, but the " +
            "JSON parser rejected it with the following error:\n\n" +
            "```\n$parseErrorMsg\n```\n\n" +
            "Here is the broken response you produced:\n\n" +
            "```\n${brokenRaw.take(6000)}\n```\n\n" +
            "Return ONLY valid JSON matching the schema described in the " +
            "ORIGINAL prompt. No prose, no markdown fences. Your first " +
            "character MUST be '{' and your last MUST be '}'.\n\n" +
            "ORIGINAL PROMPT (for schema reference):\n" +
            originalPrompt.take(8000)

    val retryRaw = try {
        provider.generate(retryPrompt, maxTokens)
    } catch (_: Exception) {
        return null
    }

    var candidate = retryRaw.trim()
    if (candidate.startsWith("```")) {
        val block = Regex("""\{.*\}""", RegexOption.DOT_MATCHES_ALL).find(candidate)
        if (block != null) {
            candidate = block.value
        }
    }

    return try {
        val parsed = _extract_json(candidate)
        if (parsed["_parse_error"] == true) {
            _attempt_repair(retryRaw)
        } else {
            parsed
        }
    } catch (_: Exception) {
        _attempt_repair(retryRaw)
    }
}
