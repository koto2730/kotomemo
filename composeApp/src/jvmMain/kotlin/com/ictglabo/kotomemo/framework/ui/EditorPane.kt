package com.ictglabo.kotomemo.framework.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ictglabo.kotomemo.usecase.HighlightCommand
import com.ictglabo.kotomemo.usecase.SyntaxRuleRegistry

/**
 * Editor pane using the *legacy* BasicTextField overload (value +
 * onValueChange). We tried the modern state-based overload in PR #7 but
 * it broke IME composition on desktop in Compose Multiplatform 1.10.x
 * (Japanese conversion, NSTextInputClient on macOS, TSF on Windows -
 * all funnelled through the same broken path) and also dropped chars
 * mid-input. Until the new API's desktop IME story matures upstream,
 * stay on the legacy overload.
 *
 * Trade-off: no externally observable scroll state, so we lose the
 * gutter scroll-sync and need to leave BasicTextField's internal
 * scroller alone (no Modifier.verticalScroll wrap - that wrap was the
 * root cause of the original click-jump-to-top bug).
 */
@Composable
fun EditorPane(state: EditorState, tab: TabState?) {
    Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        if (tab == null) {
            Text("No tab", style = MaterialTheme.typography.bodyMedium)
            return@Box
        }
        // Per-tab pane switch: TEXT is the editor, IMAGES is a thumbnail
        // grid of the shared attachments folder for the current file.
        if (tab.viewMode == TabViewMode.IMAGES) {
            ImagesView(tab, state)
            return@Box
        }
        val text = tab.fieldValue.text
        val path = tab.contents.filePath
        // Faint colour used to render whitespace-marker glyphs (the arrow
        // we substitute for \t and the Control Pictures we substitute for
        // other C0 controls). 0.45 alpha is "visible but de-emphasised"
        // against both light and dark themes.
        val controlCharColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
        val transformation = remember(text, path, controlCharColor) {
            val ruleSet = SyntaxRuleRegistry.rulesFor(path)
            val tokens = HighlightCommand().execute(HighlightCommand.Input(text, ruleSet))
            HighlightTransformation(tokens, SpanStyle(color = controlCharColor))
        }
        // Pull focus into the field whenever the active tab switches (incl.
        // first mount on file open). Without this BasicTextField stays
        // unfocused after a file load, so the user's first click - which
        // happens after they wheel-scrolled to find an edit position -
        // triggers the field's initial focus-gain, and the internal
        // scroll-to-caret runs against caret position 0, snapping the
        // view to the top. Forcing focus up-front keeps the caret "alive"
        // so wheel-scroll and subsequent clicks behave naturally.
        val focusRequester = remember(tab) { FocusRequester() }
        LaunchedEffect(tab) {
            runCatching { focusRequester.requestFocus() }
        }
        BasicTextField(
            value = tab.fieldValue,
            onValueChange = tab::applyText,
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when {
                        event.isCtrlPressed && event.key == Key.H -> {
                            state.finder.toggleReplace()
                            true
                        }
                        event.isCtrlPressed && event.key == Key.F -> {
                            state.finder.toggleFind()
                            true
                        }
                        // Accept Ctrl+= as an alternative Zoom In on US
                        // layouts where '=' is its own key. JIS users get
                        // the same effect via Ctrl+Shift+- registered on
                        // the menu.
                        event.isCtrlPressed && event.key == Key.Equals -> {
                            state.zoomIn()
                            true
                        }
                        // Intercept Ctrl+V so a clipboard image goes to the
                        // attachments folder (saved to disk + [img:] ref
                        // inserted) instead of being ignored by
                        // BasicTextField's text-only paste.
                        event.isCtrlPressed && event.key == Key.V -> {
                            state.pasteAtCursor()
                            true
                        }
                        event.key == Key.Tab -> handleTab(tab, state, outdent = event.isShiftPressed)
                        else -> false
                    }
                },
            textStyle = LocalTextStyle.current.copy(
                fontFamily = state.editorFont.toFontFamily(),
                fontSize = state.effectiveFontSize.sp,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            visualTransformation = transformation,
        )
    }
}

private fun handleTab(tab: TabState, state: EditorState, outdent: Boolean): Boolean {
    val sel = tab.fieldValue.selection
    val multiLine = !sel.collapsed &&
        tab.fieldValue.text.substring(sel.min, sel.max).contains('\n')
    return if (multiLine) {
        state.bulkIndent(outdent)
    } else if (!outdent) {
        val text = tab.fieldValue.text
        val newText = text.substring(0, sel.min) + "\t" + text.substring(sel.max)
        tab.applyText(TextFieldValue(newText, TextRange(sel.min + 1)))
        true
    } else {
        false
    }
}
