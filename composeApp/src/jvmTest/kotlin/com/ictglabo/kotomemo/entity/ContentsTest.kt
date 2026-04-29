package com.ictglabo.kotomemo.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ContentsTest {

    @Test
    fun `empty creates an untitled non-dirty contents`() {
        val c = Contents.empty()
        assertEquals("", c.text)
        assertNull(c.filePath)
        assertFalse(c.isDirty)
        assertEquals(Contents.UNTITLED, c.displayName)
    }

    @Test
    fun `withText flips dirty when text changes`() {
        val c = Contents.empty().withText("hello")
        assertTrue(c.isDirty)
        assertEquals("hello", c.text)
    }

    @Test
    fun `withText keeps clean state when text is identical`() {
        val c = Contents.empty()
        val same = c.withText("")
        assertFalse(same.isDirty)
    }
}
