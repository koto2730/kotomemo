package com.ictglabo.kotomemo.usecase

import com.ictglabo.kotomemo.entity.ApiPreset
import com.ictglabo.kotomemo.entity.HttpMethod
import com.ictglabo.kotomemo.entity.ResponseTarget
import com.ictglabo.kotomemo.usecase.port.HttpClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SendRequestCommandTest {

    private class FakeHttp(
        private val response: HttpClient.Response,
        var captured: HttpClient.Request? = null,
    ) : HttpClient {
        override fun execute(request: HttpClient.Request): HttpClient.Response {
            captured = request
            return response
        }
    }

    @Test
    fun `expands template, calls HTTP, extracts JSON path`() {
        val fake = FakeHttp(HttpClient.Response(200, """{"choices":[{"message":{"content":"OK"}}]}"""))
        val preset = ApiPreset(
            name = "echo",
            url = "https://example.com/api",
            method = HttpMethod.POST,
            headers = mapOf("Authorization" to "Bearer {{tokens.t}}"),
            bodyTemplate = """{"q":"{{selectionJson}}"}""",
            responseJsonPath = "choices.0.message.content",
            responseTarget = ResponseTarget.NewTab,
        )
        val cmd = SendRequestCommand(fake)
        val out = cmd.execute(
            SendRequestCommand.Input(
                preset = preset,
                selection = "hi\nthere",
                filename = "x.txt",
                tokens = mapOf("t" to "secret"),
            ),
        )
        assertTrue(out is SendRequestCommand.Output.Success)
        assertEquals("OK", out.extracted)
        assertEquals("Bearer secret", fake.captured?.headers?.get("Authorization"))
        assertEquals("""{"q":"hi\nthere"}""", fake.captured?.body)
    }

    @Test
    fun `returns failure on non-2xx status`() {
        val fake = FakeHttp(HttpClient.Response(500, "boom"))
        val out = SendRequestCommand(fake).execute(
            SendRequestCommand.Input(
                preset = ApiPreset(name = "t", url = "https://example/"),
                selection = "",
                filename = "",
                tokens = emptyMap(),
            ),
        )
        assertTrue(out is SendRequestCommand.Output.Failure)
        assertEquals(500, out.status)
    }

    @Test
    fun `returns failure when JSON path misses`() {
        val fake = FakeHttp(HttpClient.Response(200, """{"a":1}"""))
        val out = SendRequestCommand(fake).execute(
            SendRequestCommand.Input(
                preset = ApiPreset(name = "t", url = "https://example/", responseJsonPath = "b.c"),
                selection = "",
                filename = "",
                tokens = emptyMap(),
            ),
        )
        assertTrue(out is SendRequestCommand.Output.Failure)
        assertTrue(out.message.contains("JSON path"))
    }

    @Test
    fun `no JSON path returns raw body`() {
        val fake = FakeHttp(HttpClient.Response(200, "plain text"))
        val out = SendRequestCommand(fake).execute(
            SendRequestCommand.Input(
                preset = ApiPreset(name = "t", url = "https://example/"),
                selection = "",
                filename = "",
                tokens = emptyMap(),
            ),
        )
        assertTrue(out is SendRequestCommand.Output.Success)
        assertEquals("plain text", out.extracted)
    }
}
