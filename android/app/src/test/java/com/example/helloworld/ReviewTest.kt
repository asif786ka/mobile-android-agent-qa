package com.example.helloworld

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.mockito.Mockito
import org.mockito.kotlin.mock
 import com.example.helloworld.YourProviderStub
 import com.example.helloworld.LLMProvider

class ReviewTest {
    private val provider: LLMProvider = mock()
    private val platform = "android"
    private val detection = mapOf("key" to "value")
    private val diff = "some diff text"

    @Test
    fun review_successfulParse_returnsParsedResult() {
        val expected = mapOf("provider" to "providerName", "platform" to "android")
        Mockito.`when`(provider.generate(Mockito.anyString(), Mockito.anyInt())).thenReturn("{\"key\":\"value\"}")
        val result = review(provider, platform, detection, diff)
        assertEquals(expected, result)
    }

    @Test
    fun review_firstParseFails_repairSuccess_returnsRepairedResult() {
        val expected = mapOf("_recovered_via" to "json_repair", "provider" to "providerName", "platform" to "android")
        Mockito.`when`(provider.generate(Mockito.anyString(), Mockito.anyInt())).thenReturn("Invalid JSON")
        Mockito.`when`(_attempt_repair(Mockito.anyString())).thenReturn(mapOf("key" to "value"))
        val result = review(provider, platform, detection, diff)
        assertEquals(expected, result)
    }

    @Test
    fun review_repairFails_LLMRetrySuccess_returnsLLMResult() {
        val expected = mapOf("_recovered_via" to "llm_retry", "provider" to "providerName", "platform" to "android")
        Mockito.`when`(provider.generate(Mockito.anyString(), Mockito.anyInt())).thenReturn("Invalid JSON")
        Mockito.`when`(_attempt_repair(Mockito.anyString())).thenReturn(null)
        Mockito.`when`(_retry_llm_for_valid_json(Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyInt())).thenReturn(expected)
        val result = review(provider, platform, detection, diff)
        assertEquals(expected, result)
    }

    @Test
    fun review_allAttemptsFail_setsParseError() {
        val expected = mapOf("_parse_error" to true, "provider" to "providerName", "platform" to "android")
        Mockito.`when`(provider.generate(Mockito.anyString(), Mockito.anyInt())).thenReturn("Invalid JSON")
        Mockito.`when`(_attempt_repair(Mockito.anyString())).thenReturn(null)
        Mockito.`when`(_retry_llm_for_valid_json(Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyInt())).thenReturn(null)
        val result = review(provider, platform, detection, diff)
        assertEquals(expected, result)
    }

    @Test
    fun review_respectsMaxTokensEnvVar() {
        System.setProperty("MAX_TOKENS", "4096")
        val maxTokens = Integer.parseInt(System.getProperty("MAX_TOKENS"))
        assertEquals(4096, maxTokens)
        Mockito.verify(provider).generate(Mockito.anyString(), Mockito.eq(maxTokens))
    }

    @Test
    fun review_providerNameIsAlwaysSet() {
        Mockito.`when`(provider.generate(Mockito.anyString(), Mockito.anyInt())).thenReturn("{\"key\":\"value\"}")
        val result = review(provider, platform, detection, diff)
        assertTrue(result.containsKey("provider"))
        assertEquals("providerName", result["provider"])
    }
}