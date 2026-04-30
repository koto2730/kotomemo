package com.ictglabo.kotomemo.usecase

import com.ictglabo.kotomemo.entity.ApiPreset
import com.ictglabo.kotomemo.usecase.port.HttpClient

class SendRequestCommand(
    private val httpClient: HttpClient,
    private val templateRenderer: TemplateRenderer = TemplateRenderer(),
    private val jsonPathExtractor: JsonPathExtractor = JsonPathExtractor(),
) : Command<SendRequestCommand.Input, SendRequestCommand.Output> {

    data class Input(
        val preset: ApiPreset,
        val selection: String,
        val filename: String,
        val tokens: Map<String, String>,
    )

    sealed class Output {
        data class Success(val rawBody: String, val extracted: String, val status: Int) : Output()
        data class Failure(val message: String, val status: Int? = null, val rawBody: String? = null) : Output()
    }

    override fun execute(input: Input): Output {
        val context = TemplateRenderer.Context(
            selection = input.selection,
            filename = input.filename,
            tokens = input.tokens,
        )
        val url = templateRenderer.render(input.preset.url, context)
        val body = if (input.preset.bodyTemplate.isEmpty()) null
        else templateRenderer.render(input.preset.bodyTemplate, context)
        val headers = input.preset.headers.mapValues { (_, v) -> templateRenderer.render(v, context) }

        val request = HttpClient.Request(
            url = url,
            method = input.preset.method,
            headers = headers,
            body = body,
        )
        val response = runCatching { httpClient.execute(request) }
            .getOrElse { return Output.Failure("HTTP error: ${it.message}") }

        if (response.status !in 200..299) {
            return Output.Failure("HTTP ${response.status}", response.status, response.body)
        }
        val extracted = if (input.preset.responseJsonPath.isNullOrBlank()) {
            response.body
        } else {
            jsonPathExtractor.extract(response.body, input.preset.responseJsonPath)
                ?: return Output.Failure(
                    "JSON path '${input.preset.responseJsonPath}' did not match",
                    response.status,
                    response.body,
                )
        }
        return Output.Success(response.body, extracted, response.status)
    }
}
