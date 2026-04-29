package com.ictglabo.kotomemo.usecase

import kotlin.test.Test
import kotlin.test.assertEquals

class FindReplaceTest {

    private val find = FindMatchesCommand()
    private val replace = ReplaceAllCommand()

    @Test
    fun `literal find returns all match ranges`() {
        val r = find.execute(FindMatchesCommand.Input("abcabc", "ab", regex = false, caseSensitive = true))
        assertEquals(listOf(0..1, 3..4), r)
    }

    @Test
    fun `case insensitive literal find`() {
        val r = find.execute(FindMatchesCommand.Input("AbcABC", "abc", regex = false, caseSensitive = false))
        assertEquals(listOf(0..2, 3..5), r)
    }

    @Test
    fun `regex find returns matched ranges`() {
        val r = find.execute(FindMatchesCommand.Input("a1 b22 c333", """\d+""", regex = true, caseSensitive = true))
        assertEquals(listOf(1..1, 4..5, 8..10), r)
    }

    @Test
    fun `invalid regex returns empty`() {
        val r = find.execute(FindMatchesCommand.Input("abc", "[unterminated", regex = true, caseSensitive = true))
        assertEquals(emptyList(), r)
    }

    @Test
    fun `replace all literal counts replacements`() {
        val r = replace.execute(
            ReplaceAllCommand.Input("foo foo foo", "foo", "bar", regex = false, caseSensitive = true),
        )
        assertEquals("bar bar bar", r.text)
        assertEquals(3, r.count)
    }

    @Test
    fun `replace all regex with backreference`() {
        val r = replace.execute(
            ReplaceAllCommand.Input("name=John age=30", """(\w+)=(\w+)""", "$2:$1", regex = true, caseSensitive = true),
        )
        assertEquals("John:name 30:age", r.text)
        assertEquals(2, r.count)
    }

    @Test
    fun `non-regex replacement treats dollar signs literally`() {
        val r = replace.execute(
            ReplaceAllCommand.Input("xxx", "x", "$1", regex = false, caseSensitive = true),
        )
        assertEquals("$1$1$1", r.text)
    }
}
