package com.ictglabo.kotomemo.usecase

class TemplateRenderer {

    data class Context(
        val selection: String,
        val filename: String,
        val tokens: Map<String, String>,
    )

    private val placeholder = Regex("""\{\{\s*([a-zA-Z0-9_.]+)\s*}}""")

    fun render(template: String, context: Context): String {
        if (template.isEmpty()) return template
        return placeholder.replace(template) { match ->
            val key = match.groupValues[1]
            resolve(key, context) ?: match.value
        }
    }

    private fun resolve(key: String, context: Context): String? = when {
        key == "selection" -> context.selection
        key == "selectionJson" -> jsonEscape(context.selection)
        key == "filename" -> context.filename
        key.startsWith("tokens.") -> context.tokens[key.removePrefix("tokens.")]
        else -> null
    }

    private fun jsonEscape(s: String): String {
        val sb = StringBuilder(s.length + 8)
        for (c in s) {
            when (c) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                '\b' -> sb.append("\\b")
                '' -> sb.append("\\f")
                else -> if (c.code < 0x20) {
                    sb.append("\\u%04x".format(c.code))
                } else {
                    sb.append(c)
                }
            }
        }
        return sb.toString()
    }
}
