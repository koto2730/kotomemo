package com.ictglabo.kotomemo.usecase

class BulkIndentCommand(
    private val indent: String = "\t",
    private val spaceWidth: Int = 4,
) : Command<BulkIndentCommand.Input, BulkIndentCommand.Result> {

    enum class Mode { Indent, Outdent }

    data class Input(
        val text: String,
        val selectionStart: Int,
        val selectionEnd: Int,
        val mode: Mode,
    )

    data class Result(
        val text: String,
        val selectionStart: Int,
        val selectionEnd: Int,
    )

    override fun execute(input: Input): Result {
        val text = input.text
        val a = minOf(input.selectionStart, input.selectionEnd).coerceIn(0, text.length)
        val b = maxOf(input.selectionStart, input.selectionEnd).coerceIn(0, text.length)

        val firstLineStart = text.lastIndexOf('\n', (a - 1).coerceAtLeast(0)) + 1
        val effectiveEnd = if (b > a && text.getOrNull(b - 1) == '\n') b - 1 else b
        val nextLf = text.indexOf('\n', effectiveEnd.coerceAtMost(text.length))
        val lastLineEnd = if (nextLf == -1) text.length else nextLf

        val before = text.substring(0, firstLineStart)
        val middle = text.substring(firstLineStart, lastLineEnd)
        val after = text.substring(lastLineEnd)

        val lines = middle.split('\n')
        var deltaFirst = 0
        var deltaTotal = 0
        val newLines = lines.mapIndexed { i, line ->
            val processed = when (input.mode) {
                Mode.Indent -> indent + line
                Mode.Outdent -> outdentLine(line)
            }
            val delta = processed.length - line.length
            if (i == 0) deltaFirst = delta
            deltaTotal += delta
            processed
        }

        val newText = before + newLines.joinToString("\n") + after
        val newStart = (a + deltaFirst).coerceAtLeast(firstLineStart)
        val newEnd = (b + deltaTotal).coerceAtLeast(newStart)
        return Result(newText, newStart, newEnd)
    }

    private fun outdentLine(line: String): String = when {
        line.startsWith("\t") -> line.drop(1)
        else -> {
            val leading = line.takeWhile { it == ' ' }.length.coerceAtMost(spaceWidth)
            line.drop(leading)
        }
    }
}
