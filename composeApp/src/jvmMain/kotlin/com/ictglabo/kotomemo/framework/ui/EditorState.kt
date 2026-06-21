@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.ictglabo.kotomemo.framework.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import com.ictglabo.kotomemo.adapter.controller.EditorController
import com.ictglabo.kotomemo.entity.ApiPreset
import com.ictglabo.kotomemo.entity.AppConfig
import com.ictglabo.kotomemo.entity.Contents
import com.ictglabo.kotomemo.entity.LineEnding
import com.ictglabo.kotomemo.entity.ResponseTarget
import com.ictglabo.kotomemo.usecase.BulkIndentCommand
import com.ictglabo.kotomemo.usecase.FindMatchesCommand
import com.ictglabo.kotomemo.usecase.ReplaceAllCommand
import com.ictglabo.kotomemo.usecase.SendRequestCommand
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import kotlin.concurrent.thread

/**
 * Per-tab editor state. The text content lives in a TextFieldState
 * (modern Compose BasicTextField API) so we get:
 *   - built-in undo/redo via textState.undoState
 *   - a shareable ScrollState the gutter and editor can both observe
 *   - cleaner IME handling than the legacy TextFieldValue overload
 *
 * `contents` mirrors the buffer text plus on-disk metadata (charset, line
 * ending, BOM, isDirty, file path). It is kept in sync by EditorPane via
 * a snapshotFlow watching textState.text.
 */
class TabState(initial: Contents) {
    var contents by mutableStateOf(initial)
    val textState: TextFieldState = TextFieldState(initial.text)
    val scrollState: ScrollState = ScrollState(0)

    /** Convenience reads. Both are observable from composable scopes. */
    val text: String get() = textState.text.toString()
    val selection: TextRange get() = textState.selection

    /** Replace the entire buffer text and position the caret/selection. */
    fun setText(newText: String, newSelection: TextRange = TextRange.Zero) {
        textState.edit {
            replace(0, length, newText)
            selection = newSelection
        }
    }

    /** Move only the selection without touching the text. */
    fun setSelection(newSelection: TextRange) {
        textState.edit {
            selection = newSelection
        }
    }

    /**
     * Swap the underlying Contents. Saving reuses this with identical text
     * just to refresh isDirty / path metadata - we preserve the caret in
     * that case so BasicTextField's internal scroll-to-caret does not snap
     * the view to the top after every Ctrl+S.
     *
     * On a true text change (file open) we replace the buffer text and wipe
     * undo history so undo cannot rewind across an Open boundary.
     */
    fun replaceContents(newContents: Contents) {
        val sameText = newContents.text == text
        contents = newContents
        if (!sameText) {
            setText(newContents.text, TextRange.Zero)
            textState.undoState.clearHistory()
        }
    }

    fun undo() {
        textState.undoState.undo()
    }

    fun redo() {
        textState.undoState.redo()
    }
}

class EditorState(
    private val controller: EditorController,
    private val bulkIndentCommand: BulkIndentCommand = BulkIndentCommand(),
    private val findMatchesCommand: FindMatchesCommand = FindMatchesCommand(),
    private val replaceAllCommand: ReplaceAllCommand = ReplaceAllCommand(),
) {
    val tabs = mutableStateListOf<TabState>()
    var selectedIndex by mutableStateOf(-1)
    var editorFont by mutableStateOf(EditorFont())
    var zoomPercent by mutableStateOf(100)
    val finder = FinderState()
    var appConfig by mutableStateOf(AppConfig.EMPTY)
    var sendBusy by mutableStateOf(false)
    var sendStatus by mutableStateOf<String?>(null)
    var presetDialogOpen by mutableStateOf(false)
    var sendPaletteOpen by mutableStateOf(false)
    var showLineNumbers by mutableStateOf(false)

    val effectiveFontSize: Int
        get() = ((editorFont.size * zoomPercent) / 100).coerceAtLeast(EditorFont.MIN_SIZE)

    val current: TabState? get() = tabs.getOrNull(selectedIndex)

    init {
        newTab()
        appConfig = controller.loadConfig()
    }

    fun reloadConfig() {
        appConfig = controller.loadConfig()
    }

    fun saveConfig(newConfig: AppConfig) {
        controller.saveConfig(newConfig)
        appConfig = newConfig
    }

    fun sendWithPreset(preset: ApiPreset) {
        if (sendBusy) return
        val tab = current ?: return
        val sel = tab.selection
        val text = tab.text
        val selectedText = if (sel.collapsed) text
        else text.substring(sel.min, sel.max)
        val filename = tab.contents.displayName
        sendBusy = true
        sendStatus = "Sending to '${preset.name}'…"
        thread(name = "kotomemo-send", isDaemon = true) {
            val out = controller.send(
                SendRequestCommand.Input(
                    preset = preset,
                    selection = selectedText,
                    filename = filename,
                    tokens = appConfig.tokens,
                ),
            )
            handleSendResult(preset, sel.min, sel.max, out)
        }
    }

    private fun handleSendResult(
        preset: ApiPreset,
        selStart: Int,
        selEnd: Int,
        out: SendRequestCommand.Output,
    ) {
        when (out) {
            is SendRequestCommand.Output.Failure -> {
                sendStatus = "Send failed: ${out.message}"
            }
            is SendRequestCommand.Output.Success -> {
                sendStatus = "Send OK (${out.status})"
                when (preset.responseTarget) {
                    ResponseTarget.NewTab -> {
                        val empty = controller.newContents().copy(text = out.extracted, isDirty = true)
                        tabs += TabState(empty)
                        selectedIndex = tabs.lastIndex
                    }
                    ResponseTarget.AfterSelection -> insertAfterSelection(selStart, selEnd, out.extracted)
                    ResponseTarget.StatusOnly -> Unit
                }
            }
        }
        sendBusy = false
    }

    private fun insertAfterSelection(selStart: Int, selEnd: Int, response: String) {
        val tab = current ?: return
        val text = tab.text
        val end = selEnd.coerceIn(0, text.length)
        // ensure we land at the start of the next line: insert a newline if needed
        val needsNewlineBefore = end > 0 && text[end - 1] != '\n'
        val needsNewlineAfter = !response.endsWith('\n')
        val payload = buildString {
            if (needsNewlineBefore) append('\n')
            append(response)
            if (needsNewlineAfter) append('\n')
        }
        val newText = text.substring(0, end) + payload + text.substring(end)
        // Restore the original selection (offsets are unaffected because insertion is to the right of selEnd)
        tab.setText(newText, TextRange(selStart, selEnd))
    }

    fun newTab() {
        tabs += TabState(controller.newContents())
        selectedIndex = tabs.lastIndex
    }

    fun openFile(path: Path) {
        val loaded = controller.open(path)
        val existing = tabs.indexOfFirst { it.contents.filePath == path }
        if (existing >= 0) {
            tabs[existing].replaceContents(loaded)
            selectedIndex = existing
        } else {
            tabs += TabState(loaded)
            selectedIndex = tabs.lastIndex
        }
    }

    fun openOrPrepare(path: Path) {
        if (Files.exists(path)) {
            openFile(path)
        } else {
            val empty = controller.newContents().copy(filePath = path)
            tabs += TabState(empty)
            selectedIndex = tabs.lastIndex
        }
    }

    fun closeInitialEmptyIfPossible() {
        if (tabs.size > 1 && tabs[0].contents.filePath == null && tabs[0].contents.text.isEmpty()) {
            tabs.removeAt(0)
            if (selectedIndex > 0) selectedIndex -= 1
        }
    }

    fun saveCurrent(targetPath: Path? = null) {
        val tab = current ?: return
        val path = targetPath ?: tab.contents.filePath ?: return
        // tab.contents.text may lag the buffer by a snapshot frame (the
        // EditorPane LaunchedEffect mirrors textState.text -> contents.text).
        // Take the buffer text directly so an immediate Ctrl+S after typing
        // saves what's actually on screen.
        val toSave = tab.contents.copy(text = tab.text)
        val saved = controller.save(toSave, path)
        tab.replaceContents(saved)
    }

    fun closeTab(index: Int) {
        if (index !in tabs.indices) return
        tabs.removeAt(index)
        if (tabs.isEmpty()) {
            newTab()
        } else if (selectedIndex >= tabs.size) {
            selectedIndex = tabs.lastIndex
        }
    }

    fun setLineEnding(le: LineEnding) {
        val tab = current ?: return
        tab.contents = tab.contents.copy(lineEnding = le, isDirty = true)
    }

    fun setBom(withBom: Boolean) {
        val tab = current ?: return
        tab.contents = tab.contents.copy(hasBom = withBom, isDirty = true)
    }

    fun setCharset(charset: Charset) {
        val tab = current ?: return
        tab.contents = tab.contents.copy(charset = charset, isDirty = true)
    }

    fun setFontFamily(family: EditorFont.Family) {
        editorFont = editorFont.copy(family = family)
    }

    fun setFontSize(size: Int) {
        editorFont = editorFont.copy(size = size.coerceIn(EditorFont.MIN_SIZE, EditorFont.MAX_SIZE))
    }

    fun zoomIn() { zoomPercent = (zoomPercent + 10).coerceAtMost(500) }
    fun zoomOut() { zoomPercent = (zoomPercent - 10).coerceAtLeast(50) }
    fun zoomReset() { zoomPercent = 100 }

    fun toggleLineNumbers() {
        showLineNumbers = !showLineNumbers
    }

    fun selectAll() {
        val tab = current ?: return
        tab.setSelection(TextRange(0, tab.text.length))
    }

    fun copySelection() {
        val tab = current ?: return
        val sel = tab.selection
        if (sel.collapsed) return
        ClipboardBridge.copy(tab.text.substring(sel.min, sel.max))
    }

    fun cutSelection() {
        val tab = current ?: return
        val sel = tab.selection
        if (sel.collapsed) return
        val text = tab.text
        ClipboardBridge.copy(text.substring(sel.min, sel.max))
        val newText = text.removeRange(sel.min, sel.max)
        tab.setText(newText, TextRange(sel.min))
    }

    fun pasteAtCursor() {
        val tab = current ?: return
        val payload = ClipboardBridge.paste() ?: return
        val sel = tab.selection
        val text = tab.text
        val newText = text.substring(0, sel.min) + payload + text.substring(sel.max)
        val cursor = sel.min + payload.length
        tab.setText(newText, TextRange(cursor))
    }

    fun bulkIndent(outdent: Boolean): Boolean {
        val tab = current ?: return false
        val sel = tab.selection
        val text = tab.text
        if (sel.collapsed || !text.substring(sel.min, sel.max).contains('\n')) {
            return false
        }
        val mode = if (outdent) BulkIndentCommand.Mode.Outdent else BulkIndentCommand.Mode.Indent
        val r = bulkIndentCommand.execute(
            BulkIndentCommand.Input(text, sel.min, sel.max, mode),
        )
        tab.setText(r.text, TextRange(r.selectionStart, r.selectionEnd))
        return true
    }

    fun runFind() {
        val tab = current ?: return
        val matches = findMatchesCommand.execute(
            FindMatchesCommand.Input(
                text = tab.text,
                query = finder.query,
                regex = finder.regex,
                caseSensitive = finder.caseSensitive,
            ),
        )
        finder.lastMatchCount = matches.size
        finder.lastReplaceCount = -1
        if (matches.isEmpty()) {
            finder.wrappedAround = false
            return
        }
        val cursor = tab.selection.max
        val nextAfter = matches.firstOrNull { it.first >= cursor }
        val target = nextAfter ?: matches.first()
        finder.wrappedAround = nextAfter == null
        tab.setSelection(TextRange(target.first, target.last + 1))
    }

    fun runReplaceOne() {
        val tab = current ?: return
        val sel = tab.selection
        if (sel.collapsed) {
            runFind()
            return
        }
        val text = tab.text
        val selectedText = text.substring(sel.min, sel.max)
        val matchesInSelection = findMatchesCommand.execute(
            FindMatchesCommand.Input(
                text = selectedText,
                query = finder.query,
                regex = finder.regex,
                caseSensitive = finder.caseSensitive,
            ),
        )
        val isFullMatch = matchesInSelection.size == 1 &&
            matchesInSelection[0].first == 0 &&
            matchesInSelection[0].last == selectedText.length - 1
        if (!isFullMatch) {
            runFind()
            return
        }
        val r = replaceAllCommand.execute(
            ReplaceAllCommand.Input(
                text = selectedText,
                query = finder.query,
                replacement = finder.replacement,
                regex = finder.regex,
                caseSensitive = finder.caseSensitive,
            ),
        )
        val newText = text.substring(0, sel.min) + r.text + text.substring(sel.max)
        val cursor = sel.min + r.text.length
        tab.setText(newText, TextRange(cursor))
        runFind()
        finder.lastReplaceCount = 1
    }

    fun runReplaceAll() {
        val tab = current ?: return
        val r = replaceAllCommand.execute(
            ReplaceAllCommand.Input(
                text = tab.text,
                query = finder.query,
                replacement = finder.replacement,
                regex = finder.regex,
                caseSensitive = finder.caseSensitive,
            ),
        )
        finder.lastReplaceCount = r.count
        if (r.count > 0) {
            tab.setText(r.text, TextRange(0))
        }
    }
}
