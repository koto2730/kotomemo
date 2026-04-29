package com.ictglabo.kotomemo.entity

import kotlin.test.Test
import kotlin.test.assertEquals

class LineEndingTest {

    @Test
    fun `detects CRLF`() {
        assertEquals(LineEnding.CRLF, LineEnding.detect("a\r\nb"))
    }

    @Test
    fun `detects LF when no CRLF present`() {
        assertEquals(LineEnding.LF, LineEnding.detect("a\nb"))
    }

    @Test
    fun `detects LF on empty input`() {
        assertEquals(LineEnding.LF, LineEnding.detect(""))
    }
}
