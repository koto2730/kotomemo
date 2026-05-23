package com.ictglabo.kotomemo.framework.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import com.ictglabo.kotomemo.entity.LineEnding
import com.ictglabo.kotomemo.framework.ui.dialog.FileDialogs
import java.awt.Frame

@Composable
fun FrameWindowScope.AppMenuBar(state: EditorState, onExit: () -> Unit) {
    val parentFrame: Frame? = window
    MenuBar {
        Menu("File", mnemonic = 'F') {
            Item(
                "New",
                shortcut = KeyShortcut(Key.N, ctrl = true),
                onClick = { state.newTab() },
            )
            Item(
                "Open…",
                shortcut = KeyShortcut(Key.O, ctrl = true),
                onClick = {
                    FileDialogs.openFile(parentFrame)?.let(state::openFile)
                },
            )
            Item(
                "Save",
                shortcut = KeyShortcut(Key.S, ctrl = true),
                onClick = {
                    val tab = state.current ?: return@Item
                    if (tab.contents.filePath != null) {
                        state.saveCurrent()
                    } else {
                        val suggested = SuggestedFilename.from(tab.fieldValue.text)
                        FileDialogs.saveFile(parentFrame, suggestedName = suggested)
                            ?.let { state.saveCurrent(it) }
                    }
                },
            )
            Item(
                "Save As…",
                shortcut = KeyShortcut(Key.S, ctrl = true, shift = true),
                onClick = {
                    val tab = state.current ?: return@Item
                    val suggested = tab.contents.filePath?.fileName?.toString()
                        ?: SuggestedFilename.from(tab.fieldValue.text)
                    FileDialogs.saveFile(parentFrame, suggestedName = suggested)
                        ?.let { state.saveCurrent(it) }
                },
            )
            Separator()
            Item(
                "Close Tab",
                shortcut = KeyShortcut(Key.W, ctrl = true),
                onClick = { state.closeTab(state.selectedIndex) },
            )
            Separator()
            Item(
                "Exit",
                shortcut = KeyShortcut(Key.Q, ctrl = true),
                onClick = onExit,
            )
        }
        Menu("Edit", mnemonic = 'E') {
            Item(
                "Undo",
                shortcut = KeyShortcut(Key.Z, ctrl = true),
                onClick = { state.current?.undo() },
            )
            Item(
                "Redo",
                shortcut = KeyShortcut(Key.Y, ctrl = true),
                onClick = { state.current?.redo() },
            )
            Separator()
            Item("Cut", onClick = { state.cutSelection() })
            Item("Copy", onClick = { state.copySelection() })
            Item("Paste", onClick = { state.pasteAtCursor() })
            Separator()
            Item("Select All", onClick = { state.selectAll() })
            Separator()
            Item(
                "Find…",
                shortcut = KeyShortcut(Key.F, ctrl = true),
                onClick = { state.finder.toggleFind() },
            )
            Item(
                "Replace…",
                shortcut = KeyShortcut(Key.H, ctrl = true),
                onClick = { state.finder.toggleReplace() },
            )
        }
        Menu("View", mnemonic = 'V') {
            // Ctrl+Shift+Minus is the same physical chord as Ctrl++ on a US
            // layout and Ctrl+= on a JIS layout, so a single binding covers
            // both keyboard layouts naturally.
            Item(
                "Zoom In",
                shortcut = KeyShortcut(Key.Minus, ctrl = true, shift = true),
                onClick = { state.zoomIn() },
            )
            Item(
                "Zoom Out",
                shortcut = KeyShortcut(Key.Minus, ctrl = true),
                onClick = { state.zoomOut() },
            )
            Item(
                "Reset Zoom (100%)",
                shortcut = KeyShortcut(Key.Zero, ctrl = true),
                onClick = { state.zoomReset() },
            )
        }
        Menu("Send", mnemonic = 'S') {
            val presets = state.appConfig.presets
            // Ctrl+; opens the SendPaletteDialog: a quick command-palette
            // style picker showing every preset with a number prefix so the
            // first ten can be fired by pressing 1-9 or 0 (10th). Listed
            // before the per-preset items so it sits near the top of the
            // menu.
            Item(
                "Send palette…",
                shortcut = KeyShortcut(Key.Semicolon, ctrl = true),
                enabled = !state.sendBusy,
                onClick = { state.sendPaletteOpen = true },
            )
            Separator()
            if (presets.isEmpty()) {
                Item("(no presets)", enabled = false, onClick = {})
            } else {
                presets.forEach { p ->
                    Item(
                        p.name,
                        enabled = !state.sendBusy,
                        onClick = { state.sendWithPreset(p) },
                    )
                }
            }
            Separator()
            Item("Configure presets…", onClick = { state.presetDialogOpen = true })
        }
        Menu("Format", mnemonic = 'O') {
            Menu("Line Ending") {
                LineEnding.entries.forEach { le ->
                    Item(le.name, onClick = { state.setLineEnding(le) })
                }
            }
            Menu("BOM") {
                Item("Add BOM", onClick = { state.setBom(true) })
                Item("Remove BOM", onClick = { state.setBom(false) })
            }
            Menu("Font") {
                EditorFont.Family.entries.forEach { f ->
                    Item("Family: ${f.name}", onClick = { state.setFontFamily(f) })
                }
                Separator()
                listOf(10, 12, 14, 16, 18, 20, 24).forEach { sz ->
                    Item("Size: $sz", onClick = { state.setFontSize(sz) })
                }
            }
        }
    }
}
