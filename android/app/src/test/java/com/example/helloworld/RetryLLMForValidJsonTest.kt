package com.example.helloworld

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class RetryLLMForValidJsonTest {

    private val provider: LLMProvider = mock()

    @Test
    fun retry_llmForValidJson_callsProviderGenerateOnce() {
        val originalPrompt = "original prompt"
        val brokenRaw = "{ some malformed json"
        val parseErrorMsg = "JSONDecodeError"
        val maxTokens = 1024

        _retry_llm_for_valid_json(provider, originalPrompt, brokenRaw, parseErrorMsg, maxTokens)

        Mockito.verify(provider).generate(Mockito.anyString(), Mockito.eq(maxTokens))
    }

    @Test
    fun retry_llmForValidJson_returnsParsedDict_whenModelReturnsCleanJSON() {
        val originalPrompt = "original prompt"
        val brokenRaw = "{\"key\":\"value\"}"
        val parseErrorMsg = ""
        val maxTokens = 1024

        whenever(provider.generate(Mockito.anyString(), Mockito.eq(maxTokens))).thenReturn(brokenRaw)

        val result = _retry_llm_for_valid_json(provider, originalPrompt, brokenRaw, parseErrorMsg, maxTokens)

        assertEquals(mapOf("key" to "value"), result)
    }

    @Test
    fun retry_llmForValidJson_returnsAttemptRepairResult_whenPlainParseFails() {
        val originalPrompt = "original prompt"
        val brokenRaw = "{ some malformed json"
        val parseErrorMsg = "JSONDecodeError"
        val maxTokens = 1024
        val repairedJson = "{\"repaired_key\":\"repaired_value\"}"

        whenever(provider.generate(Mockito.anyString(), Mockito.eq(maxTokens))).thenReturn(brokenRaw)
        whenever(_attempt_repair(brokenRaw)).thenReturn(mapOf("repaired_key" to "repaired_value"))

        val result = _retry_llm_for_valid_json(provider, originalPrompt, brokenRaw, parseErrorMsg, maxTokens)

        assertEquals(mapOf("repaired_key" to "repaired_value"), result)
    }

    @Test
    fun retry_llmForValidJson_returnsNull_whenProviderGenerateRaisesException() {
        val originalPrompt = "original prompt"
        val brokenRaw = "{ some malformed json"
        val parseErrorMsg = "JSONDecodeError"
        val maxTokens = 1024

        whenever(provider.generate(Mockito.anyString(), Mockito.eq(maxTokens))).thenThrow(RuntimeException("Error"))

        val result = _retry_llm_for_valid_json(provider, originalPrompt, brokenRaw, parseErrorMsg, maxTokens)

        assertNull(result)
    }

    @Test
    fun retry_llmForValidJson_recordsUsage_whenLastUsageIsPresent() {
        val originalPrompt = "original prompt"
        val brokenRaw = "{\"key\":\"value\"}"
        val parseErrorMsg = ""
        val maxTokens = 1024
        val usageMap = mapOf("model" to "mockModel", "input_tokens" to 2, "output_tokens" to 2)

        whenever(provider.generate(Mockito.anyString(), Mockito.eq(maxTokens))).thenReturn(brokenRaw)
        whenever(provider.last_usage).thenReturn(usageMap)

        _retry_llm_for_valid_json(provider, originalPrompt, brokenRaw, parseErrorMsg, maxTokens)

        Mockito.verify(::record_usage).apply(Mockito.any(), Mockito.eq("mockModel"), Mockito.eq(2), Mockito.eq(2), Mockito.eq("reviewer_retry"))
    }

    @Test
    fun retry_llmForValidJson_doesNotCallRecordUsage_whenLastUsageIsAbsent() {
        val originalPrompt = "original prompt"
        val brokenRaw = "{\"key\":\"value\"}"
        val parseErrorMsg = ""
        val maxTokens = 1024

        whenever(provider.generate(Mockito.anyString(), Mockito.eq(maxTokens))).thenReturn(brokenRaw)
        whenever(provider.last_usage).thenReturn(null)

        _retry_llm_for_valid_json(provider, originalPrompt, brokenRaw, parseErrorMsg, maxTokens)

        Mockito.verify(::record_usage, Mockito.never()).apply(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())
    }

    @Test
    fun retry_llmForValidJson_truncatesBrokenRawTo6000CharsInPrompt() {
        // Your test to check if brokenRaw is truncated (not specified in the provided target)
        // Implement as required
    }
}