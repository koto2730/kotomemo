package com.ictglabo.kotomemo.framework.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.ictglabo.kotomemo.entity.ApiPreset

/**
 * Quick-pick palette for sending the current selection through one of the
 * configured API presets. Opens via Ctrl+; (see AppMenuBar).
 *
 * Interaction:
 *   - 1..9 fires presets[0..8] immediately.
 *   - 0 fires presets[9] (browser-tab numbering: last in the visible group).
 *   - Up/Down move the highlight, Enter fires the highlighted preset
 *     (works for entries beyond the numbered first ten).
 *   - Esc closes without sending.
 *   - Click on a row fires that preset.
 *
 * The first ten rows show a leading number column; rows beyond that show a
 * blank slot in the same column so the names stay aligned.
 */
@Composable
fun SendPaletteDialog(state: EditorState) {
    if (!state.sendPaletteOpen) return

    val presets = state.appConfig.presets
    val dialogState = rememberDialogState(size = DpSize(560.dp, 480.dp))
    var selectedIndex by remember { mutableStateOf(0) }
    val focusRequester = remember { FocusRequester() }

    // Number key -> preset index. 1..9 cover positions 0..8, 0 covers
    // position 9 (browser-tab convention picked in user discussion).
    val numberKeyToIndex = remember {
        mapOf(
            Key.One to 0,
            Key.Two to 1,
            Key.Three to 2,
            Key.Four to 3,
            Key.Five to 4,
            Key.Six to 5,
            Key.Seven to 6,
            Key.Eight to 7,
            Key.Nine to 8,
            Key.Zero to 9,
        )
    }

    fun close() {
        state.sendPaletteOpen = false
    }

    fun trigger(idx: Int) {
        if (idx in presets.indices) {
            state.sendWithPreset(presets[idx])
            close()
        }
    }

    LaunchedEffect(Unit) {
        // Initial focus so the key handler below receives events without the
        // user having to click anywhere first.
        focusRequester.requestFocus()
    }

    DialogWindow(
        onCloseRequest = ::close,
        state = dialogState,
        title = "Send palette",
    ) {
        MaterialTheme {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .focusRequester(focusRequester)
                    .focusable()
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (event.key) {
                            Key.Escape -> {
                                close(); true
                            }
                            Key.Enter, Key.NumPadEnter -> {
                                trigger(selectedIndex); true
                            }
                            Key.DirectionUp -> {
                                if (presets.isNotEmpty()) {
                                    selectedIndex = (selectedIndex - 1)
                                        .coerceAtLeast(0)
                                }
                                true
                            }
                            Key.DirectionDown -> {
                                if (presets.isNotEmpty()) {
                                    selectedIndex = (selectedIndex + 1)
                                        .coerceAtMost(presets.lastIndex)
                                }
                                true
                            }
                            else -> {
                                val numIdx = numberKeyToIndex[event.key]
                                if (numIdx != null) {
                                    trigger(numIdx)
                                    true
                                } else {
                                    false
                                }
                            }
                        }
                    }
            ) {
                if (presets.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "No presets configured. Open Send › Configure presets… to add one.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                } else {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        presets.forEachIndexed { idx, preset ->
                            PresetRow(
                                preset = preset,
                                numberLabel = numberLabelFor(idx),
                                selected = idx == selectedIndex,
                                onClick = { trigger(idx) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Number column text for a row.
 *   - idx 0..8 -> "1".."9"
 *   - idx 9    -> "0"
 *   - idx 10+  -> blank (kept as a space so the name column stays aligned)
 */
private fun numberLabelFor(idx: Int): String = when {
    idx in 0..8 -> (idx + 1).toString()
    idx == 9 -> "0"
    else -> " "
}

@Composable
private fun PresetRow(
    preset: ApiPreset,
    numberLabel: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = numberLabel,
            modifier = Modifier.width(20.dp),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.width(4.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = preset.name.ifBlank { "(unnamed)" },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = preset.url,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
