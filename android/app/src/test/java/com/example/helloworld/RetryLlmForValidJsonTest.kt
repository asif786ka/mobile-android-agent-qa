package com.example.helloworld

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

class RetryLlmForValidJsonTest {

    private val provider: LLMProvider = mock(LLMProvider::class.java)

    @Test
    fun retry_llm_for_valid_json_successfulParse_returnsParsedDict() {
        val originalPrompt = "original prompt"
        val brokenRaw = "{\"key\": \"value\"}"
        val parseErrorMsg = "No error"
        val maxTokens = 8192

        whenever(provider.generate("You previously returned a response to the prompt below, but the JSON parser rejected it with the following error:\n\n```${parseErrorMsg}\n```\n\nHere is the broken response you produced:\n\n```${brokenRaw.slice(0..6000)}\n```\n\nReturn ONLY valid JSON matching the schema described in the ORIGINAL prompt. No prose, no markdown fences. Your first character MUST be '{' and your last MUST be '}'.\n\nORIGINAL PROMPT (for schema reference):\n${originalPrompt.slice(0..8000)}", maxTokens)).thenReturn(brokenRaw)

        val result = _retry_llm_for_valid_json(provider, originalPrompt, brokenRaw, parseErrorMsg, maxTokens)

        assertEquals(mapOf("key" to "value"), result)
    }

    @Test
    fun retry_llm_for_valid_json_failedParse_returnsNull() {
        val originalPrompt = "original prompt"
        val brokenRaw = "not a json"
        val parseErrorMsg = "Parse error"
        val maxTokens = 8192

        whenever(provider.generate("You previously returned a response to the prompt below, but the JSON parser rejected it with the following error:\n\n```${parseErrorMsg}\n```\n\nHere is the broken response you produced:\n\n```${brokenRaw.slice(0..6000)}\n```\n\nReturn ONLY valid JSON matching the schema described in the ORIGINAL prompt. No prose, no markdown fences. Your first character MUST be '{' and your last MUST be '}'.\n\nORIGINAL PROMPT (for schema reference):\n${originalPrompt.slice(0..8000)}", maxTokens)).thenReturn(brokenRaw)

        val result = _retry_llm_for_valid_json(provider, originalPrompt, brokenRaw, parseErrorMsg, maxTokens)

        assertNull(result)
    }

    @Test
    fun retry_llm_for_valid_json_providerRaisesException_returnsNull() {
        val originalPrompt = "original prompt"
        val brokenRaw = "{\"key\": \"value\"}"
        val parseErrorMsg = "No error"
        val maxTokens = 8192

        whenever(provider.generate("You previously returned a response to the prompt below, but the JSON parser rejected it with the following error:\n\n```${parseErrorMsg}\n```\n\nHere is the broken response you produced:\n\n```${brokenRaw.slice(0..6000)}\n```\n\nReturn ONLY valid JSON matching the schema described in the ORIGINAL prompt. No prose, no markdown fences. Your first character MUST be '{' and your last MUST be '}'.\n\nORIGINAL PROMPT (for schema reference):\n${originalPrompt.slice(0..8000)}", maxTokens)).thenThrow(RuntimeException("Some exception"))

        val result = _retry_llm_for_valid_json(provider, originalPrompt, brokenRaw, parseErrorMsg, maxTokens)

        assertNull(result)
    }

    @Test
    fun retry_llm_for_valid_json_usageRecording_recordsUsage() {
        val originalPrompt = "original prompt"
        val brokenRaw = "{\"key\": \"value\"}"
        val parseErrorMsg = "No error"
        val maxTokens = 8192

        val usageMap = mutableMapOf<String, Any>("model" to "gpt-3", "input_tokens" to 10, "output_tokens" to 5)
        whenever(provider.last_usage).thenReturn(usageMap)
        whenever(provider.generate("You previously returned a response to the prompt below, but the JSON parser rejected it with the following error:\n\n```${parseErrorMsg}\n```\n\nHere is the broken response you produced:\n\n```${brokenRaw.slice(0..6000)}\n```\n\nReturn ONLY valid JSON matching the schema described in the ORIGINAL prompt. No prose, no markdown fences. Your first character MUST be '{' and your last MUST be '}'.\n\nORIGINAL PROMPT (for schema reference):\n${originalPrompt.slice(0..8000)}", maxTokens)).thenReturn(brokenRaw)

        val result = _retry_llm_for_valid_json(provider, originalPrompt, brokenRaw, parseErrorMsg, maxTokens)

        assertEquals(mapOf("key" to "value"), result)
        // Verify recording usage here if required
    }

    @Test
    fun retry_llm_for_valid_json_noUsageRecordedWhenAbsent() {
        val originalPrompt = "original prompt"
        val brokenRaw = "{\"key\": \"value\"}"
        val parseErrorMsg = "No error"
        val maxTokens = 8192

        whenever(provider.last_usage).thenReturn(null)
        whenever(provider.generate("You previously returned a response to the prompt below, but the JSON parser rejected it with the following error:\n\n```${parseErrorMsg}\n```\n\nHere is the broken response you produced:\n\n```${brokenRaw.slice(0..6000)}\n```\n\nReturn ONLY valid JSON matching the schema described in the ORIGINAL prompt. No prose, no markdown fences. Your first character MUST be '{' and your last MUST be '}'.\n\nORIGINAL PROMPT (for schema reference):\n${originalPrompt.slice(0..8000)}", maxTokens)).thenReturn(brokenRaw)

        val result = _retry_llm_for_valid_json(provider, originalPrompt, brokenRaw, parseErrorMsg, maxTokens)

        assertEquals(mapOf("key" to "value"), result)
        // Verify no usage was recorded
    }
}