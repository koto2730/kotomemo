package com.ictglabo.kotomemo.usecase

class FindMatchesCommand : Command<FindMatchesCommand.Input, List<IntRange>> {

    data class Input(
        val text: String,
        val query: String,
        val regex: Boolean,
        val caseSensitive: Boolean,
    )

    override fun execute(input: Input): List<IntRange> {
        if (input.query.isEmpty()) return emptyList()
        val pattern = compile(input) ?: return emptyList()
        return pattern.findAll(input.text).map { it.range }.toList()
    }

    companion object {
        fun compile(input: Input): Regex? {
            val opts = if (input.caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
            val raw = if (input.regex) input.query else Regex.escape(input.query)
            return runCatching { Regex(raw, opts) }.getOrNull()
        }
    }
}
