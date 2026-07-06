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
    /**
     * Name of the shared attachments folder placed alongside each edited
     * file. Empty falls back to the default. Users can override to
     * ".attachments", "img", etc.
     */
    val attachmentsFolder: String = DEFAULT_ATTACHMENTS_FOLDER,
) {
    companion object {
        const val DEFAULT_ATTACHMENTS_FOLDER = "attachments"
        val EMPTY = AppConfig()
    }
}
