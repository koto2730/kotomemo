package com.ictglabo.kotomemo.framework.ui

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.ictglabo.kotomemo.usecase.HighlightCommand

/**
 * Visual overlay for the legacy BasicTextField (value / onValueChange API).
 * Two jobs:
 *
 * 1. Substitute C0 control characters (and DEL) with their Unicode Control
 *    Pictures glyph (U+2400..U+241F + U+2421). TAB uses → (U+2192) instead
 *    of ␉ because the right-arrow is the more widely recognised "this is
 *    a tab" convention in text editors. LF is left alone so line layout
 *    stays natural.
 *
 * 2. Apply syntax-highlight SpanStyles on top.
 *
 * Each substitution is exactly 1 char in / 1 char out, so OffsetMapping
 * stays Identity and cursor positions / selection / IME composition
 * indices line up with the underlying state.
 */
class HighlightTransformation(
    private val tokens: List<HighlightCommand.Token>,
    private val controlCharStyle: SpanStyle,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        val originalLen = original.length

        // Pass 1: substitute control characters into the displayed text.
        val displayBuilder = StringBuilder(originalLen)
        val controlPositions = ArrayList<Int>()
        for (i in 0 until originalLen) {
            val c = original[i]
            val glyph = controlGlyph(c)
            if (glyph != null) {
                displayBuilder.append(glyph)
                controlPositions += i
            } else {
                displayBuilder.append(c)
            }
        }

        val annotatedBuilder = AnnotatedString.Builder(displayBuilder.toString())

        // Pass 2: syntax-highlight spans (token offsets index the original
        // text; 1:1 substitution above means they index the display too).
        for (token in tokens) {
            val s = token.start.coerceIn(0, originalLen)
            val e = token.endExclusive.coerceIn(s, originalLen)
            if (s >= e) continue
            annotatedBuilder.addStyle(SpanStyle(color = SyntaxColors.colorOf(token.kind)), s, e)
        }

        // Pass 3: control character style (faint) - applied last so it sits
        // on top of any syntax colour that might also cover the position.
        for (i in controlPositions) {
            annotatedBuilder.addStyle(controlCharStyle, i, i + 1)
        }

        return TransformedText(annotatedBuilder.toAnnotatedString(), OffsetMapping.Identity)
    }

    private fun controlGlyph(c: Char): String? = when (val code = c.code) {
        0x09 -> "→"            // TAB -> →
        0x0A -> null           // LF: preserve, drives line layout
        in 0x00..0x1F -> (code + 0x2400).toChar().toString()  // ␀..␟
        0x7F -> "␡"            // DEL -> ␡
        else -> null
    }
}
