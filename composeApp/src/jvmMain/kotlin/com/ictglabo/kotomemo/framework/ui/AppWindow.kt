@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
)

package com.ictglabo.kotomemo.framework.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import androidx.compose.ui.window.FrameWindowScope
import com.ictglabo.kotomemo.adapter.controller.EditorController
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

@Composable
fun FrameWindowScope.AppWindow(
    controller: EditorController,
    initialPaths: List<Path> = emptyList(),
    onExit: () -> Unit,
) {
    val state = remember {
        EditorState(controller).also { s ->
            initialPaths.forEach { s.openOrPrepare(it) }
            s.closeInitialEmptyIfPossible()
        }
    }

    // Files dragged from Explorer/Finder open as tabs. Uses Compose's
    // external drag-and-drop API (CMP 1.7+): an AWT DropTarget on the
    // frame does NOT work here because the Skia canvas is the deepest
    // heavyweight component under the cursor and AWT DnD doesn't bubble
    // up to the frame.
    val fileDropTarget = remember {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                val data = event.dragData()
                if (data !is DragData.FilesList) return false
                val dropped = data.readFiles().mapNotNull { uri ->
                    runCatching { Path.of(URI(uri)) }.getOrNull()
                }
                // Directories are skipped: kotomemo has no folder view, and
                // silently exploding a tree into dozens of tabs would be
                // worse than doing nothing.
                val files = dropped.filter { Files.isRegularFile(it) }
                files.forEach { state.openFile(it) }
                return files.isNotEmpty()
            }
        }
    }

    AppMenuBar(state, onExit)
    ApiPresetDialog(state)
    SendPaletteDialog(state)
    SaveFirstDialog(state, window)
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .dragAndDropTarget(
                    shouldStartDragAndDrop = { true },
                    target = fileDropTarget,
                ),
        ) {
            TabBar(state)
            FindReplaceBar(state)
            Box(modifier = Modifier.weight(1f)) {
                EditorPane(state, state.current)
            }
            StatusBar(state)
        }
    }
}
