@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.ictglabo.kotomemo.framework.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Left-side line number column. Shares the editor's ScrollState so it
 * scrolls in lockstep with the BasicTextField (the gutter itself is
 * non-interactive - wheel / drag pass through to the editor).
 *
 * Layout:
 *   - Monospace font, right-aligned via space-padding into a 4-char slot.
 *   - Lines 1..9999 render as their number.
 *   - Lines >= 10000 render as "····" - the actual line number is still
 *     visible on the status bar (Ln/Col). 4 digits cover almost any text
 *     file a Notepad-style editor will reasonably hold; widening the
 *     gutter beyond that wastes horizontal space.
 *
 * Caveat: a single multi-line Text is used (not one Text per line) for
 * cheapness on big files. Right-alignment therefore relies on the
 * monospace font: each padded label is exactly 4 character cells wide.
 */
@Composable
fun LineNumberGutter(tab: TabState, fontSize: Int) {
    val text = tab.text
    val lineCount = remember(text) {
        var n = 1
        for (c in text) if (c == '\n') n++
        n
    }
    val labelText = remember(lineCount) {
        buildString {
            for (i in 1..lineCount) {
                if (i > 1) append('\n')
                val s = if (i <= 9999) i.toString() else "····"
                append(s.padStart(4, ' '))
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(tab.scrollState, enabled = false)
            .padding(end = 8.dp),
    ) {
        Text(
            text = labelText,
            style = LocalTextStyle.current.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = fontSize.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }
}
