package com.ictglabo.kotomemo.framework.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Tab strip. LazyRow instead of a plain scrollable Row so that:
 *  - the active tab is scrolled into view automatically (newly opened
 *    tabs land at the end and used to be unreachable off-screen),
 *  - overflow arrows can page the strip when there are more tabs than
 *    fit the window width.
 */
@Composable
fun TabBar(state: EditorState) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Whenever the selection (or tab count) changes, keep the active tab
    // visible - covers "open new tab at the end" and Ctrl+W reflows.
    LaunchedEffect(state.selectedIndex, state.tabs.size) {
        if (state.selectedIndex in state.tabs.indices) {
            listState.animateScrollToItem(state.selectedIndex)
        }
    }

    val hasOverflow = listState.canScrollBackward || listState.canScrollForward

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Both arrows grouped at the left edge - split ends means a long
        // mouse travel between them on a wide window.
        if (hasOverflow) {
            ScrollArrow("◀", enabled = listState.canScrollBackward) {
                scope.launch { listState.animateScrollBy(-320f) }
            }
            ScrollArrow("▶", enabled = listState.canScrollForward) {
                scope.launch { listState.animateScrollBy(320f) }
            }
        }
        LazyRow(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            itemsIndexed(state.tabs, key = { _, tab -> tab.contents.id }) { index, tab ->
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
            }
        }
    }
}

@Composable
private fun ScrollArrow(glyph: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        text = glyph,
        style = MaterialTheme.typography.bodyMedium,
        color = if (enabled) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    )
}
