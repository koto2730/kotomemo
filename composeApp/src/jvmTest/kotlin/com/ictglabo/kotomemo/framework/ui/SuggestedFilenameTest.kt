package com.ictglabo.kotomemo.framework.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class SuggestedFilenameTest {

    @Test
    fun `falls back when text is empty`() {
        assertEquals("untitled.txt", SuggestedFilename.from(""))
    }

    @Test
    fun `uses first line trimmed`() {
        assertEquals("hello.txt", SuggestedFilename.from("  hello  \nworld"))
    }

    @Test
    fun `strips invalid filename chars`() {
        assertEquals("abcd.txt", SuggestedFilename.from("ab/cd"))
        assertEquals("name.txt", SuggestedFilename.from("name?<>"))
    }

    @Test
    fun `caps length at 32`() {
        val long = "a".repeat(80)
        val out = SuggestedFilename.from(long)
        assertEquals("${"a".repeat(32)}.txt", out)
    }

    @Test
    fun `falls back when first line is only whitespace or symbols`() {
        assertEquals("untitled.txt", SuggestedFilename.from("\n\nbody"))
        assertEquals("untitled.txt", SuggestedFilename.from("///"))
    }
}
