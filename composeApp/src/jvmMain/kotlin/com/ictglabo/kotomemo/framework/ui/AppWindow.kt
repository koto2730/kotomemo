package com.ictglabo.kotomemo.framework.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.FrameWindowScope
import com.ictglabo.kotomemo.adapter.controller.EditorController
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
    AppMenuBar(state, onExit)
    ApiPresetDialog(state)
    SendPaletteDialog(state)
    SaveFirstDialog(state, window)
    MaterialTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            TabBar(state)
            FindReplaceBar(state)
            Box(modifier = Modifier.weight(1f)) {
                EditorPane(state, state.current)
            }
            StatusBar(state)
        }
    }
}
