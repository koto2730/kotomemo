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
            val opts = buildSet {
                if (!input.caseSensitive) add(RegexOption.IGNORE_CASE)
                // Editor convention (VS Code, Notepad++, CodeMirror): ^ and $
                // anchor to line starts/ends, not just the buffer edges.
                // Without MULTILINE, "^foo" only ever matches on line 1.
                // DOTALL stays off - "." not crossing newlines is the same
                // convention those editors default to.
                if (input.regex) add(RegexOption.MULTILINE)
            }
            val raw = if (input.regex) input.query else Regex.escape(input.query)
            return runCatching { Regex(raw, opts) }.getOrNull()
        }
    }
}
