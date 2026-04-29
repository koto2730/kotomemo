package com.ictglabo.kotomemo.usecase

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HighlightCommandTest {

    private val cmd = HighlightCommand()

    @Test
    fun `c-family tokenizes line comment and string`() {
        val rs = SyntaxRuleRegistry.rulesFor(Path.of("foo.kt"))
        assertNotNull(rs)
        val text = """val x = "hello" // greet"""
        val tokens = cmd.execute(HighlightCommand.Input(text, rs))
        val kinds = tokens.map { it.kind }
        assertTrue(TokenKind.StringLit in kinds)
        assertTrue(TokenKind.Comment in kinds)
    }

    @Test
    fun `c-family tokenizes block comment spanning lines`() {
        val rs = SyntaxRuleRegistry.rulesFor(Path.of("a.java"))!!
        val text = "code /* multi\nline */ more"
        val tokens = cmd.execute(HighlightCommand.Input(text, rs))
        val comment = tokens.single { it.kind == TokenKind.Comment }
        assertEquals(5, comment.start)
        assertEquals(text.indexOf("*/") + 2, comment.endExclusive)
    }

    @Test
    fun `hash family treats # as comment`() {
        val rs = SyntaxRuleRegistry.rulesFor(Path.of("script.py"))!!
        val text = "x = 1 # comment"
        val tokens = cmd.execute(HighlightCommand.Input(text, rs))
        assertEquals(TokenKind.Comment, tokens.last().kind)
    }

    @Test
    fun `string with escaped quote is one token`() {
        val rs = SyntaxRuleRegistry.rulesFor(Path.of("a.js"))!!
        val text = """val s = "a \"b\" c""""
        val tokens = cmd.execute(HighlightCommand.Input(text, rs))
        val strs = tokens.filter { it.kind == TokenKind.StringLit }
        assertEquals(1, strs.size)
    }

    @Test
    fun `unknown extension yields no tokens`() {
        val rs = SyntaxRuleRegistry.rulesFor(Path.of("a.unknownext"))
        assertNull(rs)
        val tokens = cmd.execute(HighlightCommand.Input("anything", rs))
        assertTrue(tokens.isEmpty())
    }

    @Test
    fun `null path yields no rule set`() {
        assertNull(SyntaxRuleRegistry.rulesFor(null))
    }

    @Test
    fun `Makefile basename matches hash family`() {
        val rs = SyntaxRuleRegistry.rulesFor(Path.of("Makefile"))
        assertNotNull(rs)
    }

    @Test
    fun `xml comment`() {
        val rs = SyntaxRuleRegistry.rulesFor(Path.of("a.html"))!!
        val text = "<p>x</p><!-- hi -->"
        val tokens = cmd.execute(HighlightCommand.Input(text, rs))
        assertEquals(1, tokens.count { it.kind == TokenKind.Comment })
    }
}
