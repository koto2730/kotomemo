package com.ictglabo.kotomemo.framework.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.input.TextFieldValue
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
        val text = tab.fieldValue.text
        val path = tab.contents.filePath
        val transformation = remember(text, path) {
            val ruleSet = SyntaxRuleRegistry.rulesFor(path)
            val tokens = HighlightCommand().execute(HighlightCommand.Input(text, ruleSet))
            HighlightTransformation(tokens)
        }
        // Don't wrap BasicTextField in Modifier.verticalScroll. It has its own
        // internal scrolling for multi-line content that auto-follows the
        // caret on Enter and survives focus changes; an outer scroll fights
        // with that and produces "Enter doesn't scroll" plus "click jumps
        // back to top" bugs.
        BasicTextField(
            value = tab.fieldValue,
            onValueChange = tab::applyText,
            modifier = Modifier
                .fillMaxSize()
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
