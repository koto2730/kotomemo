package com.ictglabo.kotomemo.framework.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class CursorPositionTest {

    @Test
    fun `start of empty text is line 1 col 1`() {
        assertEquals(CursorPosition(1, 1), CursorPosition.of("", 0))
    }

    @Test
    fun `column counts from previous line break`() {
        assertEquals(CursorPosition(2, 3), CursorPosition.of("ab\ncd", 5))
    }

    @Test
    fun `multiple lines tracked`() {
        assertEquals(CursorPosition(3, 1), CursorPosition.of("a\nb\nc", 4))
    }

    @Test
    fun `out of range offset is clamped`() {
        assertEquals(CursorPosition(1, 4), CursorPosition.of("abc", 99))
    }
}
