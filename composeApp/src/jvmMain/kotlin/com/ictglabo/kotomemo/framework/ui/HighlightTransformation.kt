package com.ictglabo.kotomemo.framework.ui

import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.ui.text.SpanStyle
import com.ictglabo.kotomemo.usecase.HighlightCommand

/**
 * Output overlay for the modern BasicTextField. Does two jobs:
 *
 * 1. Visualises C0 control characters (and DEL) so users can see what
 *    otherwise renders as a width-zero gap. Control characters are
 *    invisible by definition - that's the whole trap when they show
 *    up where you expected a space or got pasted from another tool.
 *    Each control char is substituted with its glyph from the Unicode
 *    "Control Pictures" block (U+2400-U+241F + U+2421). One exception:
 *    LF stays as-is because the editor relies on it for line layout
 *    and we don't want to make every line break a visible glyph.
 *
 *    TAB uses → (U+2192) instead of ␉ because the right-arrow is the
 *    more widely recognised "this is a tab" convention in text editors.
 *
 * 2. Applies syntax highlight SpanStyles to comments/strings/etc. on
 *    top of the visualised text.
 *
 * Every substitution is exactly 1 char in / 1 char out, so offsets in
 * the transformed buffer line up with offsets in the underlying
 * TextFieldState - syntax tokens (computed against the original text)
 * still index correctly.
 *
 * state.text keeps the original control byte; save / copy / paste use
 * state.text directly so the file is unchanged.
 */
class HighlightOutputTransformation(
    private val tokens: List<HighlightCommand.Token>,
    private val controlCharStyle: SpanStyle,
) : OutputTransformation {
    override fun TextFieldBuffer.transformOutput() {
        // Pass 1: walk from the end so 1:1 char replacements never shift
        // earlier offsets (defensive - currently every glyph is single
        // char so this would hold either way).
        val originalLen = length
        for (i in originalLen - 1 downTo 0) {
            val glyph = controlGlyph(charAt(i)) ?: continue
            replace(i, i + 1, glyph)
        }

        // Pass 2: style the substituted glyphs in the muted control-char
        // colour. We re-scan the (now-transformed) buffer rather than
        // tracking offsets from pass 1 because both passes are 1:1 so
        // the offsets are stable.
        for (i in 0 until length) {
            if (isControlGlyph(charAt(i))) {
                addStyle(controlCharStyle, i, i + 1)
            }
        }

        // Pass 3: syntax highlight. tokens are computed against the
        // original text; 1:1 replacement in pass 1 means the same
        // offsets apply to the transformed buffer.
        if (tokens.isEmpty()) return
        val len = length
        for (token in tokens) {
            val s = token.start.coerceIn(0, len)
            val e = token.endExclusive.coerceIn(s, len)
            if (s >= e) continue
            addStyle(SpanStyle(color = SyntaxColors.colorOf(token.kind)), s, e)
        }
    }

    /**
     * Glyph to display in place of a control character.
     *   - null  => leave the character as-is (e.g. LF, regular text)
     *   - other => 1-char substitution (offset-preserving)
     */
    private fun controlGlyph(c: Char): String? = when (val code = c.code) {
        0x09 -> "→"   // TAB -> →
        0x0A -> null       // LF: preserve, drives the line layout
        in 0x00..0x1F -> (code + 0x2400).toChar().toString()  // ␀..␟
        0x7F -> "␡"   // DEL -> ␡
        else -> null
    }

    private fun isControlGlyph(c: Char): Boolean = when (c.code) {
        0x2192 -> true                  // → (our TAB substitute)
        in 0x2400..0x241F -> true       // ␀..␟ Control Pictures C0
        0x2421 -> true                  // ␡ DEL
        else -> false
    }
}
