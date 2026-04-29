package com.ictglabo.kotomemo.framework.ui

import androidx.compose.ui.graphics.Color
import com.ictglabo.kotomemo.usecase.TokenKind

object SyntaxColors {
    private val Comment = Color(0xFF6A737D)
    private val StringLit = Color(0xFFA31515)

    fun colorOf(kind: TokenKind): Color = when (kind) {
        TokenKind.Comment -> Comment
        TokenKind.StringLit -> StringLit
    }
}
