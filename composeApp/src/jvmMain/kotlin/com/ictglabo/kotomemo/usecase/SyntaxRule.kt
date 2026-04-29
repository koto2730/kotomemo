package com.ictglabo.kotomemo.usecase

enum class TokenKind { Comment, StringLit }

data class SyntaxRule(
    val kind: TokenKind,
    val pattern: Regex,
)

data class SyntaxRuleSet(
    val name: String,
    val rules: List<SyntaxRule>,
)
