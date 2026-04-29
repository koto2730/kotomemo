package com.ictglabo.kotomemo.framework.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TabBar(state: EditorState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        state.tabs.forEachIndexed { index, tab ->
            val selected = index == state.selectedIndex
            val bg = if (selected) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant
            Row(
                modifier = Modifier
                    .background(bg)
                    .clickable { state.selectedIndex = index }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val mark = if (tab.contents.isDirty) "●" else ""
                Text(
                    text = "${tab.contents.displayName}$mark",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "×",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable { state.closeTab(index) },
                )
            }
            Spacer(Modifier.width(1.dp))
        }
    }
}
