package com.ictglabo.kotomemo.framework.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ictglabo.kotomemo.usecase.HighlightCommand
import com.ictglabo.kotomemo.usecase.SyntaxRuleRegistry

@Composable
fun EditorPane(state: EditorState, tab: TabState?) {
    Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        if (tab == null) {
            Text("No tab", style = MaterialTheme.typography.bodyMedium)
            return@Box
        }
        // Reading tab.text registers a snapshot read on textState.text, so
        // any edit (user typing, undo, programmatic setText) triggers a
        // recompose here and refreshes the highlight tokens.
        val text = tab.text
        val path = tab.contents.filePath
        val transformation = remember(text, path) {
            val ruleSet = SyntaxRuleRegistry.rulesFor(path)
            val tokens = HighlightCommand().execute(HighlightCommand.Input(text, ruleSet))
            HighlightOutputTransformation(tokens)
        }

        // The new BasicTextField API has no onValueChange callback - the
        // TextFieldState is mutated directly by the field. To keep
        // Contents.isDirty / Contents.text in sync with what the user is
        // typing, we observe textState.text and mirror it into contents.
        LaunchedEffect(tab) {
            snapshotFlow { tab.textState.text.toString() }
                .collect { newText ->
                    if (newText != tab.contents.text) {
                        tab.contents = tab.contents.copy(text = newText, isDirty = true)
                    }
                }
        }

        Row(modifier = Modifier.fillMaxSize()) {
            if (state.showLineNumbers) {
                LineNumberGutter(tab, fontSize = state.effectiveFontSize)
            }
            BasicTextField(
                state = tab.textState,
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
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
                            // layouts where '=' is its own key. JIS users get the
                            // same effect via Ctrl+Shift+- registered on the menu.
                            event.isCtrlPressed && event.key == Key.Equals -> {
                                state.zoomIn()
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
                lineLimits = TextFieldLineLimits.MultiLine(),
                outputTransformation = transformation,
                scrollState = tab.scrollState,
            )
        }
    }
}

private fun handleTab(tab: TabState, state: EditorState, outdent: Boolean): Boolean {
    val sel = tab.selection
    val text = tab.text
    val multiLine = !sel.collapsed && text.substring(sel.min, sel.max).contains('\n')
    return if (multiLine) {
        state.bulkIndent(outdent)
    } else if (!outdent) {
        val newText = text.substring(0, sel.min) + "\t" + text.substring(sel.max)
        tab.setText(newText, TextRange(sel.min + 1))
        true
    } else {
        false
    }
}
