package com.ictglabo.kotomemo.framework.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name
import org.jetbrains.skia.Image as SkiaImage

private val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "gif", "webp")
private const val THUMBNAIL_SIZE_DP = 150

/**
 * Grid of image thumbnails from the tab's shared attachments folder.
 * Newest first (by modification time). Click a thumbnail to open the
 * full-size preview modal.
 *
 * Empty states cover: Untitled tab, folder missing, folder empty.
 * Broken image files render a "?" placeholder rather than crashing so
 * one bad PNG does not take down the whole view.
 */
@Composable
fun ImagesView(tab: TabState, state: EditorState) {
    val folder = remember(tab.contents.filePath, state.appConfig.attachmentsFolder) {
        AttachmentsPath.folderFor(tab, state.appConfig)
    }
    var previewPath by remember { mutableStateOf<Path?>(null) }

    Box(Modifier.fillMaxSize()) {
        when {
            folder == null -> EmptyPlaceholder("Save this file first to attach and view images.")
            !Files.isDirectory(folder) -> EmptyPlaceholder("No attachments folder yet (${folder.fileName}/). Paste an image while in Text view to create one.")
            else -> {
                val paths = remember(folder) { listImages(folder) }
                if (paths.isEmpty()) {
                    EmptyPlaceholder("No images in ${folder.fileName}/ yet.")
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(THUMBNAIL_SIZE_DP.dp),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(paths) { path ->
                            Thumbnail(path, onClick = { previewPath = path })
                        }
                    }
                }
            }
        }
    }

    previewPath?.let { path ->
        ImagePreviewModal(path, onDismiss = { previewPath = null })
    }
}

private fun listImages(folder: Path): List<Path> {
    val out = ArrayList<Path>()
    Files.newDirectoryStream(folder).use { stream ->
        for (p in stream) {
            if (!Files.isRegularFile(p)) continue
            val ext = p.fileName.toString().substringAfterLast('.', "").lowercase()
            if (ext in IMAGE_EXTENSIONS) out.add(p)
        }
    }
    // Newest first so freshly pasted screenshots appear at the top.
    return out.sortedByDescending {
        runCatching { Files.getLastModifiedTime(it).toMillis() }.getOrDefault(0L)
    }
}

@Composable
private fun EmptyPlaceholder(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Thumbnail(path: Path, onClick: () -> Unit) {
    // Load lazily per composition. remember(path) means each file is decoded
    // once until the tab or file is replaced. For very large folders / very
    // large images this could get expensive; that's a later optimisation.
    val bitmap = remember(path) {
        runCatching {
            SkiaImage.makeFromEncoded(Files.readAllBytes(path)).toComposeImageBitmap()
        }.getOrNull()
    }
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(6.dp),
    ) {
        Box(
            modifier = Modifier.size(THUMBNAIL_SIZE_DP.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = path.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text("?", style = MaterialTheme.typography.headlineMedium)
            }
        }
        Text(
            path.name,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(THUMBNAIL_SIZE_DP.dp).padding(top = 4.dp),
        )
    }
}

@Composable
private fun ImagePreviewModal(path: Path, onDismiss: () -> Unit) {
    val bitmap = remember(path) {
        runCatching {
            SkiaImage.makeFromEncoded(Files.readAllBytes(path)).toComposeImageBitmap()
        }.getOrNull()
    }
    val dialogState = rememberDialogState(size = DpSize(1000.dp, 800.dp))
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    DialogWindow(
        onCloseRequest = onDismiss,
        state = dialogState,
        title = path.name,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onDismiss)
                .focusRequester(focusRequester)
                .focusable()
                .onPreviewKeyEvent { ev ->
                    if (ev.type == KeyEventType.KeyDown && ev.key == Key.Escape) {
                        onDismiss(); true
                    } else false
                },
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = path.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                )
            } else {
                Text(
                    "Failed to load ${path.name}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
