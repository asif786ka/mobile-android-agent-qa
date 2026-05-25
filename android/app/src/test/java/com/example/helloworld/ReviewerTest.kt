package com.example.helloworld

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import providers.base.LLMProvider

class ReviewerTest {
    private val provider = mock(LLMProvider::class.java)

    @Test
    fun review_successfulParse_returnsProviderAndPlatformSet() {
        val detection = mapOf("key" to "value")
        val diff = "some diff content"
        val expectedResponse = mapOf("provider" to "mockProvider", "platform" to "android")

        whenever(provider.generate(any(), any())).thenReturn("{\"key\": \"value\"}")

        val result = review(provider, "android", detection, diff)

        assertEquals(expectedResponse["provider"], result["provider"])
        assertEquals(expectedResponse["platform"], result["platform"])
        assertFalse(result.containsKey("_parse_error"))
    }

    @Test
    fun review_firstParseFails_repairSucceeds_returnsRecoveredViaJsonRepair() {
        val detection = mapOf("key" to "value")
        val diff = "some diff content"
        val rawResponse = "{\"key\": \"value\"}" // simulate first parse failure

        whenever(provider.generate(any(), any())).thenReturn("Invalid JSON")
        whenever(provider.generate(any())).thenReturn(rawResponse)

        val result = review(provider, "android", detection, diff)

        assertEquals("json_repair", result["_recovered_via"])
        assertFalse(result.containsKey("_parse_error"))
    }

    @Test
    fun review_firstParseAndRepairFails_llmRetrySucceeds_returnsRecoveredViaLlmRetry() {
        val detection = mapOf("key" to "value")
        val diff = "some diff content"

        whenever(provider.generate(any(), any())).thenReturn("Invalid JSON")
        whenever(provider.generate(any())).thenReturn("Malformed JSON")
        whenever(provider.generate(any())).thenReturn("{\"key\": \"value\"}") // simulate LLM retry success

        val result = review(provider, "android", detection, diff)

        assertEquals("llm_retry", result["_recovered_via"])
        assertFalse(result.containsKey("_parse_error"))
    }

    @Test
    fun review_allAttemptsFail_setsParseErrorTrue() {
        val detection = mapOf("key" to "value")
        val diff = "some diff content"

        whenever(provider.generate(any(), any())).thenReturn("Invalid JSON")

        val result = review(provider, "android", detection, diff)

        assertTrue(result.get("_parse_error") as Boolean)
        assertEquals("mockProvider", result["provider"])
        assertEquals("android", result["platform"])
    }

    @Test
    fun review_maxTokensEnvVarRespect_forwardedToCalls() {
        val detection = mapOf("key" to "value")
        val diff = "some diff content"
        System.setProperty("MAX_TOKENS", "4096")

        whenever(provider.generate(any(), any())).thenReturn("{\"key\": \"value\"}")

        val result = review(provider, "android", detection, diff)

        assertEquals(4096, result["max_tokens"])
    }
}
