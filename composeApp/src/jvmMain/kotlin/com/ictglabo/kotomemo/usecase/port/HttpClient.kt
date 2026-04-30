package com.ictglabo.kotomemo.usecase.port

import com.ictglabo.kotomemo.entity.HttpMethod

interface HttpClient {
    data class Request(
        val url: String,
        val method: HttpMethod,
        val headers: Map<String, String>,
        val body: String?,
    )

    data class Response(
        val status: Int,
        val body: String,
    )

    fun execute(request: Request): Response
}
