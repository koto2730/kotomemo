package com.ictglabo.kotomemo.entity

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Path

data class Contents(
    val id: ContentsId,
    val text: String,
    val filePath: Path?,
    val charset: Charset,
    val lineEnding: LineEnding,
    val hasBom: Boolean,
    val isDirty: Boolean,
) {
    val displayName: String
        get() = filePath?.fileName?.toString() ?: UNTITLED

    fun withText(newText: String): Contents =
        copy(text = newText, isDirty = newText != text || isDirty)

    fun markSaved(path: Path): Contents =
        copy(filePath = path, isDirty = false)

    companion object {
        const val UNTITLED = "Untitled"

        fun empty(): Contents = Contents(
            id = ContentsId.generate(),
            text = "",
            filePath = null,
            charset = StandardCharsets.UTF_8,
            lineEnding = LineEnding.platformDefault(),
            hasBom = false,
            isDirty = false,
        )
    }
}
