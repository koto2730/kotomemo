package com.ictglabo.kotomemo.usecase

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class JsonPathExtractor(
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true },
) {
    /**
     * Extracts a value from a JSON document using dot-separated keys.
     * Numeric segments are treated as array indexes.
     * Returns the textual representation of the value, or null if the path is invalid
     * or the input is not parseable JSON.
     */
    fun extract(jsonText: String, path: String): String? {
        if (path.isEmpty()) return jsonText
        val root = runCatching { json.parseToJsonElement(jsonText) }.getOrNull() ?: return null
        var cursor: JsonElement? = root
        for (segment in path.split('.')) {
            cursor = step(cursor, segment) ?: return null
        }
        return when (val v = cursor) {
            is JsonPrimitive -> v.content
            null -> null
            else -> v.toString()
        }
    }

    private fun step(node: JsonElement?, segment: String): JsonElement? {
        if (node == null) return null
        return when (node) {
            is JsonObject -> node[segment]
            is JsonArray -> segment.toIntOrNull()?.let { node.getOrNull(it) }
            else -> null
        }
    }
}
