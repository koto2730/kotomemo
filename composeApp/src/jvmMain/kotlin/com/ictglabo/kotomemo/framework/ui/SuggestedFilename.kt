package com.ictglabo.kotomemo.framework.ui

object SuggestedFilename {
    private val INVALID = Regex("""[\\/:*?"<>|\r\n\t]""")
    private const val MAX_LEN = 32
    private const val FALLBACK = "untitled.txt"

    fun from(text: String): String {
        val firstLine = text.lineSequence().firstOrNull().orEmpty().trim()
        val sanitized = firstLine.replace(INVALID, "").take(MAX_LEN).trim()
        return if (sanitized.isEmpty()) FALLBACK else "$sanitized.txt"
    }
}
