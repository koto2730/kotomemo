package com.ictglabo.kotomemo.framework.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.ictglabo.kotomemo.framework.ui.dialog.FileDialogs
import java.awt.Frame

/**
 * Blocks image attach until the tab has a file path. Shown when the user
 * pastes a clipboard image into an Untitled tab: kotomemo needs a saved
 * file to know where the shared attachments folder should live.
 *
 * Save Now -> native Save dialog -> save the buffer -> retry the pending
 * attach so the user gets both the file on disk AND the pasted image
 * committed in one action.
 */
@Composable
fun SaveFirstDialog(state: EditorState, parentFrame: Frame?) {
    val pending = state.pendingImageForAttach ?: return
    // The image itself is not shown - the dialog just needs to know one is
    // waiting. Reference kept via `pending` to silence unused-var warnings
    // and to signal intent.
    @Suppress("UNUSED_VARIABLE") val _pending = pending

    val dialogState = rememberDialogState(size = DpSize(460.dp, 220.dp))

    DialogWindow(
        onCloseRequest = { state.cancelPendingImage() },
        state = dialogState,
        title = "Save this file first",
    ) {
        MaterialTheme {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(
                    "kotomemo needs to know where to put the attached image.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Save this untitled tab to a file, and the image will be attached into the shared \"${state.appConfig.attachmentsFolder}\" folder next to it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    OutlinedButton(onClick = { state.cancelPendingImage() }) {
                        Text("Cancel")
                    }
                    Button(onClick = {
                        val tab = state.current
                        if (tab == null) {
                            state.cancelPendingImage()
                            return@Button
                        }
                        val suggested = tab.contents.filePath?.fileName?.toString()
                            ?: SuggestedFilename.from(tab.fieldValue.text)
                        val path = FileDialogs.saveFile(parentFrame, suggestedName = suggested)
                        if (path != null) {
                            state.saveCurrent(path)
                            state.attachPendingImage()
                        } else {
                            // User dismissed the save dialog - keep the
                            // pending image so a subsequent Save Now still
                            // works. Do nothing here.
                        }
                    }) { Text("Save Now…") }
                }
            }
        }
    }
}
