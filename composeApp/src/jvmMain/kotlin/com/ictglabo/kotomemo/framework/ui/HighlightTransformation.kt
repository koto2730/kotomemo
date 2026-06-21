package com.ictglabo.kotomemo.framework.ui

import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.ui.text.SpanStyle
import com.ictglabo.kotomemo.usecase.HighlightCommand

/**
 * Syntax-highlight overlay for the modern BasicTextField (state-based API).
 * Applies SpanStyles to the displayed text without modifying its characters,
 * so offsets in the buffer stay 1:1 with the underlying TextFieldState.
 *
 * Replaces the older HighlightTransformation, which targeted the legacy
 * VisualTransformation API.
 */
class HighlightOutputTransformation(
    private val tokens: List<HighlightCommand.Token>,
) : OutputTransformation {
    override fun TextFieldBuffer.transformOutput() {
        if (tokens.isEmpty()) return
        val len = length
        for (token in tokens) {
            val s = token.start.coerceIn(0, len)
            val e = token.endExclusive.coerceIn(s, len)
            if (s >= e) continue
            addStyle(SpanStyle(color = SyntaxColors.colorOf(token.kind)), s, e)
        }
    }
}
