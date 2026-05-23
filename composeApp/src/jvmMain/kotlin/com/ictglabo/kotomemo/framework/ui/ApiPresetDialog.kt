package com.ictglabo.kotomemo.framework.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.ictglabo.kotomemo.entity.ApiPreset
import com.ictglabo.kotomemo.entity.AppConfig
import com.ictglabo.kotomemo.entity.HttpMethod
import com.ictglabo.kotomemo.entity.ResponseTarget

@Composable
fun ApiPresetDialog(state: EditorState) {
    if (!state.presetDialogOpen) return

    val dialogState = rememberDialogState(size = DpSize(880.dp, 640.dp))
    val initial = remember { state.appConfig }
    val presets = remember { mutableStateListOf<ApiPreset>().apply { addAll(initial.presets) } }
    // Headers are stored as raw text per preset while the dialog is open and
    // only parsed to Map<String,String> at save time. If we round-tripped
    // through ApiPreset.headers on every keystroke, half-typed lines like
    // "Authoriz" (no colon yet) would parse to an empty map and the keystroke
    // would disappear from the field. Same pattern as tokensText below.
    val headerDrafts = remember {
        mutableStateListOf<String>().apply {
            initial.presets.forEach { add(headersToText(it.headers)) }
        }
    }
    var tokensText by remember {
        mutableStateOf(initial.tokens.entries.joinToString("\n") { "${it.key}=${it.value}" })
    }
    var selectedIndex by remember { mutableStateOf(if (presets.isEmpty()) -1 else 0) }

    DialogWindow(
        onCloseRequest = { state.presetDialogOpen = false },
        state = dialogState,
        title = "Configure API Presets",
    ) {
        MaterialTheme {
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth().height(380.dp)) {
                    PresetList(
                        presets = presets,
                        selectedIndex = selectedIndex,
                        onSelect = { selectedIndex = it },
                        onAdd = {
                            presets += ApiPreset(name = "new-${presets.size + 1}", url = "https://")
                            headerDrafts += ""
                            selectedIndex = presets.lastIndex
                        },
                        onRemove = {
                            if (selectedIndex in presets.indices) {
                                presets.removeAt(selectedIndex)
                                headerDrafts.removeAt(selectedIndex)
                                selectedIndex = if (presets.isEmpty()) -1
                                else selectedIndex.coerceAtMost(presets.lastIndex)
                            }
                        },
                    )
                    Spacer(Modifier.width(12.dp))
                    if (selectedIndex in presets.indices) {
                        PresetForm(
                            preset = presets[selectedIndex],
                            headersText = headerDrafts[selectedIndex],
                            onUpdate = { presets[selectedIndex] = it },
                            onHeadersTextChange = { headerDrafts[selectedIndex] = it },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    } else {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            contentAlignment = Alignment.Center,
                        ) { Text("Select or add a preset") }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Tokens (one per line, key=value)", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = tokensText,
                    onValueChange = { tokensText = it },
                    modifier = Modifier.fillMaxWidth().height(110.dp),
                    placeholder = { Text("openai=sk-...\nmyapi=abc123") },
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val tokens = parseTokens(tokensText)
                        val finalPresets = presets.mapIndexed { idx, p ->
                            p.copy(headers = textToHeaders(headerDrafts[idx]))
                        }
                        state.saveConfig(AppConfig(presets = finalPresets, tokens = tokens))
                        state.presetDialogOpen = false
                    }) { Text("Save") }
                    OutlinedButton(onClick = { state.presetDialogOpen = false }) { Text("Cancel") }
                }
            }
        }
    }
}

@Composable
private fun PresetList(
    presets: List<ApiPreset>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(modifier = Modifier.width(220.dp).fillMaxHeight()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                presets.forEachIndexed { idx, p ->
                    val bg = if (idx == selectedIndex) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                    Text(
                        text = p.name.ifBlank { "(unnamed)" },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bg)
                            .clickable { onSelect(idx) }
                            .padding(8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant),
                    )
                }
                if (presets.isEmpty()) {
                    Text(
                        "(empty)",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        Row(modifier = Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = onAdd) { Text("Add") }
            OutlinedButton(onClick = onRemove) { Text("Remove") }
        }
    }
}

@Composable
private fun PresetForm(
    preset: ApiPreset,
    headersText: String,
    onUpdate: (ApiPreset) -> Unit,
    onHeadersTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        OutlinedTextField(
            value = preset.name,
            onValueChange = { onUpdate(preset.copy(name = it)) },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = preset.url,
            onValueChange = { onUpdate(preset.copy(url = it)) },
            label = { Text("URL — supports {{selection}}, {{tokens.NAME}}, etc.") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MethodPicker(preset.method) { onUpdate(preset.copy(method = it)) }
            ResponseTargetPicker(preset.responseTarget) { onUpdate(preset.copy(responseTarget = it)) }
        }
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = headersText,
            onValueChange = onHeadersTextChange,
            label = {
                Text("Headers (one per line, key: value) — values support {{selection}}, {{tokens.NAME}}, etc.")
            },
            placeholder = { Text("Authorization: Bearer {{tokens.openai}}\nContent-Type: application/json") },
            modifier = Modifier.fillMaxWidth().height(96.dp),
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = preset.bodyTemplate,
            onValueChange = { onUpdate(preset.copy(bodyTemplate = it)) },
            label = { Text("Body template — use {{selection}}, {{selectionJson}}, {{tokens.NAME}}") },
            modifier = Modifier.fillMaxWidth().height(140.dp),
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = preset.responseJsonPath.orEmpty(),
            onValueChange = { onUpdate(preset.copy(responseJsonPath = it.ifBlank { null })) },
            label = { Text("Response JSON path (optional, e.g. choices.0.message.content)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
    }
}

@Composable
private fun MethodPicker(current: HttpMethod, onChange: (HttpMethod) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }) { Text("Method: ${current.name}") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            HttpMethod.entries.forEach { m ->
                DropdownMenuItem(text = { Text(m.name) }, onClick = { onChange(m); open = false })
            }
        }
    }
}

@Composable
private fun ResponseTargetPicker(current: ResponseTarget, onChange: (ResponseTarget) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }) { Text("Response: ${current.name}") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            ResponseTarget.entries.forEach { t ->
                DropdownMenuItem(text = { Text(t.name) }, onClick = { onChange(t); open = false })
            }
        }
    }
}

private fun headersToText(headers: Map<String, String>): String =
    headers.entries.joinToString("\n") { "${it.key}: ${it.value}" }

private fun textToHeaders(text: String): Map<String, String> =
    text.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains(':') }
        .associate {
            val idx = it.indexOf(':')
            it.substring(0, idx).trim() to it.substring(idx + 1).trim()
        }

private fun parseTokens(text: String): Map<String, String> =
    text.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains('=') }
        .associate {
            val idx = it.indexOf('=')
            it.substring(0, idx).trim() to it.substring(idx + 1)
        }
