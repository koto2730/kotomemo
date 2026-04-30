package com.ictglabo.kotomemo.adapter.http

import com.ictglabo.kotomemo.entity.HttpMethod
import com.ictglabo.kotomemo.usecase.port.HttpClient
import java.net.URI
import java.net.http.HttpClient as JdkClient
import java.net.http.HttpRequest as JdkRequest
import java.net.http.HttpResponse as JdkResponse
import java.time.Duration

class JdkHttpClient(
    private val timeout: Duration = Duration.ofSeconds(60),
) : HttpClient {

    private val client: JdkClient = JdkClient.newBuilder()
        .connectTimeout(timeout)
        .followRedirects(JdkClient.Redirect.NORMAL)
        .build()

    override fun execute(request: HttpClient.Request): HttpClient.Response {
        val builder = JdkRequest.newBuilder()
            .uri(URI.create(request.url))
            .timeout(timeout)
        request.headers.forEach { (k, v) -> builder.header(k, v) }

        val publisher = if (request.body != null) {
            JdkRequest.BodyPublishers.ofString(request.body)
        } else {
            JdkRequest.BodyPublishers.noBody()
        }
        when (request.method) {
            HttpMethod.GET -> builder.GET()
            HttpMethod.POST -> builder.POST(publisher)
            HttpMethod.PUT -> builder.PUT(publisher)
            HttpMethod.DELETE -> builder.DELETE()
            HttpMethod.PATCH -> builder.method("PATCH", publisher)
        }
        val response = client.send(builder.build(), JdkResponse.BodyHandlers.ofString())
        return HttpClient.Response(status = response.statusCode(), body = response.body() ?: "")
    }
}
