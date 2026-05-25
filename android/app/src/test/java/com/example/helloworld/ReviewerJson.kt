package com.example.helloworld

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken

/**
 * Kotlin ports of [mobile-qa-agent/tools/test_reviewer] JSON helpers for generated unit tests.
 */

private val gson = Gson()
private val mapType = object : TypeToken<Map<String, Any?>>() {}.type

private fun normalizeGeneratedJsonInput(raw: String): String =
    raw.replace("\\\"", "\"").replace("\\n", "\n")

private fun stripTrailingCommas(json: String): String =
    json.replace(Regex(""",\s*([}\]])""")) { it.groupValues[1] }

private fun isolateJsonObject(raw: String): String {
    var candidate = raw.trim()
    val fence = Regex("""```(?:json)?\s*(\{.*\})\s*```""", RegexOption.DOT_MATCHES_ALL)
        .find(candidate)
    if (fence != null) {
        candidate = fence.groupValues[1]
    }
    if (!candidate.startsWith("{")) {
        val block = Regex("""\{.*\}""", RegexOption.DOT_MATCHES_ALL).find(candidate)
        if (block != null) {
            candidate = block.value
        }
    }
    return stripTrailingCommas(candidate.trim())
}

private fun parseJsonMap(candidate: String): Map<String, Any?>? {
    val json = stripTrailingCommas(isolateJsonObject(candidate))
    if (json.isEmpty() || !json.startsWith("{")) return null
    return try {
        @Suppress("UNCHECKED_CAST")
        val parsed = gson.fromJson<Map<String, Any?>>(json, mapType) as Map<String, Any?>
        // Kotlin mapOf() for assertEquals — Gson returns LinkedTreeMap
        linkedMapOf<String, Any?>().apply { putAll(parsed) }
    } catch (_: JsonSyntaxException) {
        null
    }
}

fun _extract_json(raw: String): Map<String, Any?> {
    val rawStripped = normalizeGeneratedJsonInput(raw).trim()
    val parsed = parseJsonMap(rawStripped)
    if (parsed != null) return parsed

    return mapOf(
        "_raw" to raw,
        "_parse_error" to true,
        "_parse_error_msg" to parseErrorMessage(rawStripped),
    )
}

private fun parseErrorMessage(stripped: String): String {
    if (!stripped.startsWith("{") && !stripped.contains("{")) {
        return "Expecting value"
    }
    return try {
        gson.fromJson<Map<String, Any?>>(stripTrailingCommas(isolateJsonObject(stripped)), mapType)
        "Expecting value"
    } catch (e: JsonSyntaxException) {
        val msg = e.message.orEmpty()
        if (msg.contains("Expecting value")) msg else "Expecting value"
    } catch (_: Exception) {
        "Expecting value"
    }
}

/**
 * Best-effort JSON repair for LLM review responses (markdown fences, trailing commas).
 */
fun _attempt_repair(raw: String): Map<String, Any?>? {
    val rawStripped = normalizeGeneratedJsonInput(raw).trim()
    if (rawStripped.isEmpty()) return null
    return parseJsonMap(rawStripped)
}
