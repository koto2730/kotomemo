package com.ictglabo.kotomemo.framework.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp

@Composable
fun FindReplaceBar(state: EditorState) {
    val finder = state.finder
    if (!finder.visible) return

    val queryFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(finder.focusTick) {
        runCatching { queryFocus.requestFocus() }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(
                value = finder.query,
                onValueChange = {
                    finder.query = it
                    finder.wrappedAround = false
                },
                label = { Text("Find") },
                modifier = Modifier
                    .width(280.dp)
                    .focusRequester(queryFocus)
                    .onPreviewKeyEvent { ev -> handleBarKey(ev, state, focusManager, isQuery = true) },
                singleLine = true,
            )
            Button(
                onClick = { state.runFind() },
                modifier = Modifier.onPreviewKeyEvent { ev ->
                    if (ev.type == KeyEventType.KeyDown && ev.key == Key.Enter) {
                        state.runFind(); true
                    } else handleBarKey(ev, state, focusManager, isQuery = false)
                },
            ) { Text("Find Next") }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = finder.regex, onCheckedChange = { finder.regex = it })
                Text("Regex")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = finder.caseSensitive, onCheckedChange = { finder.caseSensitive = it })
                Text("Aa")
            }
            TextButton(onClick = { finder.hide() }) { Text("Close (Esc)") }
        }

        if (finder.mode == FinderState.Mode.Replace) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = finder.replacement,
                    onValueChange = { finder.replacement = it },
                    label = { Text("Replace with") },
                    modifier = Modifier
                        .width(280.dp)
                        .onPreviewKeyEvent { ev -> handleBarKey(ev, state, focusManager, isQuery = false, isReplace = true) },
                    singleLine = true,
                )
                Button(
                    onClick = { state.runReplaceOne() },
                    modifier = Modifier.onPreviewKeyEvent { ev ->
                        if (ev.type == KeyEventType.KeyDown && ev.key == Key.Enter) {
                            state.runReplaceOne(); true
                        } else handleBarKey(ev, state, focusManager, isQuery = false)
                    },
                ) { Text("Replace") }
                Button(
                    onClick = { state.runReplaceAll() },
                    modifier = Modifier.onPreviewKeyEvent { ev ->
                        handleBarKey(ev, state, focusManager, isQuery = false)
                    },
                ) { Text("Replace All") }
            }
        }

        val msg = buildString {
            when {
                finder.lastReplaceCount >= 0 -> {
                    append("Replaced ${finder.lastReplaceCount}")
                    if (finder.wrappedAround) append(" — wrapped to top")
                }
                finder.wrappedAround -> {
                    append("Wrapped to top — Matches: ${finder.lastMatchCount}")
                }
                finder.query.isNotEmpty() -> {
                    append("Matches: ${finder.lastMatchCount}")
                }
            }
        }
        if (msg.isNotEmpty()) {
            Text(msg, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun handleBarKey(
    ev: androidx.compose.ui.input.key.KeyEvent,
    state: EditorState,
    focusManager: androidx.compose.ui.focus.FocusManager,
    isQuery: Boolean,
    isReplace: Boolean = false,
): Boolean {
    if (ev.type != KeyEventType.KeyDown) return false
    return when {
        ev.key == Key.Escape -> {
            state.finder.hide()
            true
        }
        isQuery && ev.key == Key.Enter -> {
            state.runFind()
            true
        }
        isReplace && ev.key == Key.Enter -> {
            state.runReplaceOne()
            true
        }
        ev.key == Key.Tab && ev.isShiftPressed -> {
            focusManager.moveFocus(FocusDirection.Previous)
            true
        }
        ev.key == Key.Tab -> {
            focusManager.moveFocus(FocusDirection.Next)
            true
        }
        else -> false
    }
}
