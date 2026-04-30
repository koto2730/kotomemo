package com.ictglabo.kotomemo.usecase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JsonPathExtractorTest {

    private val ex = JsonPathExtractor()

    @Test
    fun `extracts top-level string`() {
        assertEquals("hi", ex.extract("""{"msg":"hi"}""", "msg"))
    }

    @Test
    fun `walks nested objects`() {
        val s = """{"a":{"b":{"c":"deep"}}}"""
        assertEquals("deep", ex.extract(s, "a.b.c"))
    }

    @Test
    fun `numeric segment indexes arrays`() {
        val s = """{"choices":[{"text":"first"},{"text":"second"}]}"""
        assertEquals("second", ex.extract(s, "choices.1.text"))
    }

    @Test
    fun `chatgpt-style path works`() {
        val s = """{"choices":[{"message":{"content":"hello"}}]}"""
        assertEquals("hello", ex.extract(s, "choices.0.message.content"))
    }

    @Test
    fun `unknown path returns null`() {
        assertNull(ex.extract("""{"a":1}""", "b"))
    }

    @Test
    fun `invalid json returns null`() {
        assertNull(ex.extract("not json", "a"))
    }

    @Test
    fun `empty path returns raw body`() {
        assertEquals("""{"a":1}""", ex.extract("""{"a":1}""", ""))
    }
}
