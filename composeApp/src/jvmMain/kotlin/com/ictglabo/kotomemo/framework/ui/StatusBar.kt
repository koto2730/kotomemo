package com.ictglabo.kotomemo.framework.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StatusBar(state: EditorState) {
    val tab = state.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (tab == null) {
            Text("—", style = MaterialTheme.typography.bodySmall)
            return@Row
        }
        val pos = CursorPosition.of(tab.fieldValue.text, tab.fieldValue.selection.start)
        Text("Ln ${pos.line}, Col ${pos.column}", style = MaterialTheme.typography.bodySmall)
        Text(tab.contents.charset.name(), style = MaterialTheme.typography.bodySmall)
        Text(tab.contents.lineEnding.name, style = MaterialTheme.typography.bodySmall)
        Text(if (tab.contents.hasBom) "BOM" else "no BOM", style = MaterialTheme.typography.bodySmall)
        Text("${state.zoomPercent}%", style = MaterialTheme.typography.bodySmall)
        state.sendStatus?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}
