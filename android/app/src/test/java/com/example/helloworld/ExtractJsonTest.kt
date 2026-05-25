package com.example.helloworld

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows

class ExtractJsonTest {
    @Test
    fun extractJson_validJson_returnsParsedDict() {
        val raw = """{\"key\": \"value\"}"""
        val expected = mapOf("key" to "value")
        val result = _extract_json(raw)
        assertEquals(expected, result)
    }

    @Test
    fun extractJson_invalidJson_returnsParseError() {
        val raw = "invalid json string"
        val result = _extract_json(raw)
        assertTrue(result["_parse_error"] as Boolean)
        assertEquals(raw, result["_raw"])
        assertTrue((result["_parse_error_msg"] as String).contains("Expecting value"))
    }

    @Test
    fun extractJson_jsonWithMarkdownFences_returnsParsedDict() {
        val raw = """```json\n{\"key\": \"value\"}\n```"""
        val expected = mapOf("key" to "value")
        val result = _extract_json(raw)
        assertEquals(expected, result)
    }

    @Test
    fun extractJson_jsonWithTrailingComma_returnsParsedDict() {
        val raw = """{\"key\": \"value\",}"""
        val expected = mapOf("key" to "value")
        val result = _extract_json(raw)
        assertEquals(expected, result)
    }

    @Test
    fun extractJson_emptyInput_returnsParseError() {
        val raw = ""
        val result = _extract_json(raw)
        assertTrue(result["_parse_error"] as Boolean)
        assertEquals(raw, result["_raw"])
        assertTrue((result["_parse_error_msg"] as String).contains("Expecting value"))
    }

    @Test
    fun extractJson_inputNotJson_failsWithParseError() {
        val raw = "This is not JSON"
        val result = _extract_json(raw)
        assertTrue(result["_parse_error"] as Boolean)
        assertEquals(raw, result["_raw"])
        assertEquals("Expecting value", result["_parse_error_msg"])
    }
}