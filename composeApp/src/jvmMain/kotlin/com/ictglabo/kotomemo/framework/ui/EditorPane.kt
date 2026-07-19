package com.ictglabo.kotomemo.framework.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
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
import androidx.compose.ui.zIndex
import com.ictglabo.kotomemo.usecase.HighlightCommand
import com.ictglabo.kotomemo.usecase.SyntaxRuleRegistry

/**
 * Editor pane using the *legacy* BasicTextField overload (value +
 * onValueChange). We tried the modern state-based overload in PR #7 but
 * it broke desktop IME composition, so we stay on the legacy overload
 * until upstream matures.
 *
 * Scroll preservation across tabs: the legacy field's scroll position is
 * internal state with no public API, so instead of one shared field (or
 * remounting per switch - both lose the position), EVERY open tab keeps
 * its own live BasicTextField stacked in the same Box. The active one is
 * opaque, on top, and focusable; inactive ones are alpha-0, beneath, and
 * excluded from focus traversal so Tab-cycling can never type into a
 * hidden buffer. Returning to a tab therefore shows exactly the viewport
 * you left - unless the caret was off-screen, in which case focus-gain
 * scrolls minimally to bring it back into view.
 *
 * Cost: one live text layout per open tab. Fine for notepad-scale tab
 * counts; revisit if someone opens hundreds of files.
 */
@Composable
fun EditorPane(state: EditorState) {
    Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        if (state.tabs.isEmpty()) {
            Text("No tab", style = MaterialTheme.typography.bodyMedium)
            return@Box
        }
        state.tabs.forEachIndexed { index, tab ->
            key(tab) {
                TabEditorLayer(
                    state = state,
                    tab = tab,
                    selected = index == state.selectedIndex,
                )
            }
        }
    }
}

@Composable
private fun TabEditorLayer(state: EditorState, tab: TabState, selected: Boolean) {
    val focusRequester = remember { FocusRequester() }
    // Focus follows selection (incl. first mount). Keeping the caret
    // "alive" avoids the click-after-scroll jump-to-top that an unfocused
    // field exhibits on its first focus-gain.
    LaunchedEffect(selected) {
        if (selected) runCatching { focusRequester.requestFocus() }
    }

    val text = tab.fieldValue.text
    val path = tab.contents.filePath
    // Faint colour used to render whitespace-marker glyphs (the arrow we
    // substitute for \t and the Control Pictures we substitute for other
    // C0 controls). 0.45 alpha reads on both light and dark themes.
    val controlCharColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    val transformation = remember(text, path, controlCharColor) {
        val ruleSet = SyntaxRuleRegistry.rulesFor(path)
        val tokens = HighlightCommand().execute(HighlightCommand.Input(text, ruleSet))
        HighlightTransformation(tokens, SpanStyle(color = controlCharColor))
    }

    BasicTextField(
        value = tab.fieldValue,
        onValueChange = tab::applyText,
        modifier = Modifier
            .fillMaxSize()
            .alpha(if (selected) 1f else 0f)
            .zIndex(if (selected) 1f else 0f)
            .focusProperties { canFocus = selected }
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when {
                    // Esc-to-close for the Find/Replace bar lives on the
                    // window root (AppWindow), not here - see issue #15.
                    event.isCtrlPressed && event.key == Key.H -> {
                        state.finder.toggleReplace()
                        true
                    }
                    event.isCtrlPressed && event.key == Key.F -> {
                        state.finder.toggleFind()
                        true
                    }
                    // Accept Ctrl+= as an alternative Zoom In on US
                    // layouts where '=' is its own key. JIS users get the
                    // same effect via Ctrl+Shift+- registered on the menu.
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

    // IMAGES view draws over the (still-alive) editor so toggling back to
    // TEXT view also restores the editor's scroll position.
    if (selected && tab.viewMode == TabViewMode.IMAGES) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(2f)
                .background(MaterialTheme.colorScheme.surface),
        ) {
            ImagesView(tab, state)
        }
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
