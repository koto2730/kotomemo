package com.ictglabo.kotomemo.usecase

import kotlin.test.Test
import kotlin.test.assertEquals

class TemplateRendererTest {

    private val r = TemplateRenderer()

    private fun ctx(
        selection: String = "",
        filename: String = "",
        tokens: Map<String, String> = emptyMap(),
    ) = TemplateRenderer.Context(selection, filename, tokens)

    @Test
    fun `replaces selection placeholder`() {
        assertEquals("hi world", r.render("hi {{selection}}", ctx(selection = "world")))
    }

    @Test
    fun `JSON-escapes selectionJson`() {
        val out = r.render("""{"q":"{{selectionJson}}"}""", ctx(selection = "a\"b\nc"))
        assertEquals("""{"q":"a\"b\nc"}""", out)
    }

    @Test
    fun `looks up token by name`() {
        val out = r.render(
            "Bearer {{tokens.openai}}",
            ctx(tokens = mapOf("openai" to "sk-xxx")),
        )
        assertEquals("Bearer sk-xxx", out)
    }

    @Test
    fun `unknown placeholder is left intact`() {
        assertEquals("hi {{unknown}}", r.render("hi {{unknown}}", ctx()))
    }

    @Test
    fun `filename placeholder works`() {
        assertEquals("file is foo.txt", r.render("file is {{filename}}", ctx(filename = "foo.txt")))
    }

    @Test
    fun `tolerates whitespace inside braces`() {
        assertEquals("=hi=", r.render("={{ selection }}=", ctx(selection = "hi")))
    }
}
