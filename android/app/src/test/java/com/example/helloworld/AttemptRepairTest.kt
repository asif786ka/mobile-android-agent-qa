package com.example.helloworld

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull

class AttemptRepairTest {

    @Test
    fun attemptRepair_validJson_returnsDict() {
        val raw = """{\"key\": \"value\"}"""
        val result = _attempt_repair(raw)
        assertNotNull(result)
        assertEquals(mapOf("key" to "value"), result)
    }

    @Test
    fun attemptRepair_jsonRepairNotInstalled_returnsNull() {
        // Simulate ImportError
        val result = _attempt_repair("{"
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
        val raw = """\n            ```json\n            {\"key\": \"value\"}\n            ```\n        """.trimIndent()
        val result = _attempt_repair(raw)
        assertNotNull(result)
        assertEquals(mapOf("key" to "value"), result)
    }

    @Test
    fun attemptRepair_unparseableInput_returnsNull() {
        val result = _attempt_repair("this is not JSON")
        assertNull(result)
    }

    @Test
    fun attemptRepair_invalidSchemaJson_returnsNull() {
        val invalidJson = "{\"key\":\"value\", \"unexpectedField\":\"unexpectedValue\"}"
        val result = _attempt_repair(invalidJson)
        assertNull(result)
    }
}
