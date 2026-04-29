package com.ictglabo.kotomemo.framework.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.ictglabo.kotomemo.usecase.HighlightCommand

class HighlightTransformation(
    private val tokens: List<HighlightCommand.Token>,
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        if (tokens.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        val builder = AnnotatedString.Builder(text)
        val len = text.length
        for (token in tokens) {
            val s = token.start.coerceIn(0, len)
            val e = token.endExclusive.coerceIn(s, len)
            if (s >= e) continue
            builder.addStyle(SpanStyle(color = SyntaxColors.colorOf(token.kind)), s, e)
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}
