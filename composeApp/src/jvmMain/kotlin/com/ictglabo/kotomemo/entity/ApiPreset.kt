package com.ictglabo.kotomemo.entity

import kotlinx.serialization.Serializable

@Serializable
enum class ResponseTarget {
    /** Open the response (or extracted text) in a brand-new tab. */
    NewTab,

    /** Insert the response right after the current selection, on its own line. */
    AfterSelection,

    /** Do not modify any buffer; only surface success/failure in the status bar. */
    StatusOnly,
}

@Serializable
enum class HttpMethod { GET, POST, PUT, PATCH, DELETE }

@Serializable
data class ApiPreset(
    val name: String,
    val url: String,
    val method: HttpMethod = HttpMethod.POST,
    val headers: Map<String, String> = emptyMap(),
    val bodyTemplate: String = "",
    val responseJsonPath: String? = null,
    val responseTarget: ResponseTarget = ResponseTarget.NewTab,
)

@Serializable
data class AppConfig(
    val presets: List<ApiPreset> = emptyList(),
    val tokens: Map<String, String> = emptyMap(),
) {
    companion object {
        val EMPTY = AppConfig()
    }
}
