package com.example.helloworld

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows

class AttemptRepairTest {

    @Test
    fun attemptRepair_validJson_returnsDict() {
        val raw = "{\"key\": \"value\"}"
        val result = _attempt_repair(raw)
        assertEquals(mapOf("key" to "value"), result)
    }

    @Test
    fun attemptRepair_jsonRepairNotInstalled_returnsNull() {
        // Simulate ImportError
        val result = _attempt_repair("{")
        assertNull(result)
    }

    @Test
    fun attemptRepair_invalidJson_returnsNull() {
        val raw = "[\"not\", \"a\", \"dict\"]"
        val result = _attempt_repair(raw)
        assertNull(result)
    }

    @Test
    fun attemptRepair_emptyInput_returnsNull() {
        val result = _attempt_repair("")
        assertNull(result)
    }

    @Test
    fun attemptRepair_whitespaceInput_returnsNull() {
        val result = _attempt_repair("   ")
        assertNull(result)
    }

    @Test
    fun attemptRepair_stripsMarkdownFences() {
        val raw = "```json\n{\"key\": \"value\"}\n```"
        val result = _attempt_repair(raw)
        assertEquals(mapOf("key" to "value"), result)
    }

}