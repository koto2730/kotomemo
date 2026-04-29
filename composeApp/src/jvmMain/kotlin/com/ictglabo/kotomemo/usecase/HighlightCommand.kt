package com.ictglabo.kotomemo.usecase

class HighlightCommand : Command<HighlightCommand.Input, List<HighlightCommand.Token>> {

    data class Input(val text: String, val ruleSet: SyntaxRuleSet?)
    data class Token(val start: Int, val endExclusive: Int, val kind: TokenKind)

    override fun execute(input: Input): List<Token> {
        val ruleSet = input.ruleSet ?: return emptyList()
        val text = input.text
        if (text.isEmpty()) return emptyList()
        val tokens = ArrayList<Token>()
        var i = 0
        while (i < text.length) {
            var matched = false
            for (rule in ruleSet.rules) {
                val m = rule.pattern.matchAt(text, i) ?: continue
                if (m.range.isEmpty()) continue
                tokens += Token(m.range.first, m.range.last + 1, rule.kind)
                i = m.range.last + 1
                matched = true
                break
            }
            if (!matched) i++
        }
        return tokens
    }
}
