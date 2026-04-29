package com.ictglabo.kotomemo.usecase

class ReplaceAllCommand : Command<ReplaceAllCommand.Input, ReplaceAllCommand.Result> {

    data class Input(
        val text: String,
        val query: String,
        val replacement: String,
        val regex: Boolean,
        val caseSensitive: Boolean,
    )

    data class Result(val text: String, val count: Int)

    override fun execute(input: Input): Result {
        if (input.query.isEmpty()) return Result(input.text, 0)
        val pattern = FindMatchesCommand.compile(
            FindMatchesCommand.Input(input.text, input.query, input.regex, input.caseSensitive),
        ) ?: return Result(input.text, 0)
        var count = 0
        val replaced = pattern.replace(input.text) { match ->
            count++
            if (input.regex) {
                expandBackreferences(input.replacement, match)
            } else {
                input.replacement
            }
        }
        return Result(replaced, count)
    }

    private fun expandBackreferences(template: String, match: MatchResult): String {
        val sb = StringBuilder()
        var i = 0
        while (i < template.length) {
            val c = template[i]
            if (c == '\\' && i + 1 < template.length) {
                sb.append(template[i + 1])
                i += 2
            } else if (c == '$' && i + 1 < template.length && template[i + 1].isDigit()) {
                val end = (i + 2 until template.length).firstOrNull { !template[it].isDigit() } ?: template.length
                val idx = template.substring(i + 1, end).toInt()
                sb.append(match.groupValues.getOrNull(idx).orEmpty())
                i = end
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }
}
