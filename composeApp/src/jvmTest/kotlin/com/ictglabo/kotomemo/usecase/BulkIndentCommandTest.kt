package com.ictglabo.kotomemo.usecase

import kotlin.test.Test
import kotlin.test.assertEquals

class BulkIndentCommandTest {

    private val cmd = BulkIndentCommand(indent = "\t")

    @Test
    fun `indent single full selection adds tab to each line`() {
        val text = "a\nb\nc"
        val r = cmd.execute(BulkIndentCommand.Input(text, 0, text.length, BulkIndentCommand.Mode.Indent))
        assertEquals("\ta\n\tb\n\tc", r.text)
    }

    @Test
    fun `indent two-line selection ignores untouched third line`() {
        val text = "a\nb\nc"
        val r = cmd.execute(BulkIndentCommand.Input(text, 0, 3, BulkIndentCommand.Mode.Indent))
        assertEquals("\ta\n\tb\nc", r.text)
    }

    @Test
    fun `indent shifts both selection ends by inserted chars`() {
        val text = "a\nb"
        val r = cmd.execute(BulkIndentCommand.Input(text, 0, 3, BulkIndentCommand.Mode.Indent))
        assertEquals(1, r.selectionStart)
        assertEquals(5, r.selectionEnd)
    }

    @Test
    fun `outdent removes leading tab`() {
        val text = "\ta\n\tb"
        val r = cmd.execute(BulkIndentCommand.Input(text, 0, text.length, BulkIndentCommand.Mode.Outdent))
        assertEquals("a\nb", r.text)
    }

    @Test
    fun `outdent removes up to spaceWidth leading spaces`() {
        val text = "    a\n  b\nc"
        val r = cmd.execute(BulkIndentCommand.Input(text, 0, text.length, BulkIndentCommand.Mode.Outdent))
        assertEquals("a\nb\nc", r.text)
    }

    @Test
    fun `selection ending right at newline does not include next line`() {
        val text = "a\nb\nc"
        val r = cmd.execute(BulkIndentCommand.Input(text, 0, 2, BulkIndentCommand.Mode.Indent))
        assertEquals("\ta\nb\nc", r.text)
    }
}
