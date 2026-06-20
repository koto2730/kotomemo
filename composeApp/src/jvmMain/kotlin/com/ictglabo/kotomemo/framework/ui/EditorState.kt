package com.ictglabo.kotomemo.framework.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
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

class TabState(initial: Contents) {
    var contents by mutableStateOf(initial)
    var fieldValue by mutableStateOf(TextFieldValue(initial.text))

    private val undoStack = ArrayDeque<TextFieldValue>()
    private val redoStack = ArrayDeque<TextFieldValue>()
    private val maxHistory = 200

    fun applyText(newValue: TextFieldValue) {
        if (newValue.text != fieldValue.text) {
            undoStack.addLast(fieldValue)
            if (undoStack.size > maxHistory) undoStack.removeFirst()
            redoStack.clear()
            contents = contents.withText(newValue.text)
        }
        fieldValue = newValue
    }

    fun replaceContents(newContents: Contents) {
        // Save reuses this with identical text just to refresh isDirty and
        // path metadata. Rebuilding fieldValue from scratch in that case
        // discards the user's cursor/selection (TextFieldValue(text) defaults
        // to TextRange.Zero), which makes BasicTextField's internal
        // scroll-to-caret snap the view to the top after every save.
        val sameText = newContents.text == fieldValue.text
        contents = newContents
        if (!sameText) {
            fieldValue = TextFieldValue(newContents.text)
            undoStack.clear()
            redoStack.clear()
        }
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.addLast(fieldValue)
        val prev = undoStack.removeLast()
        fieldValue = prev
        contents = contents.copy(text = prev.text, isDirty = true)
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.addLast(fieldValue)
        val next = redoStack.removeLast()
        fieldValue = next
        contents = contents.copy(text = next.text, isDirty = true)
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
        val sel = tab.fieldValue.selection
        val selectedText = if (sel.collapsed) tab.fieldValue.text
        else tab.fieldValue.text.substring(sel.min, sel.max)
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
        val text = tab.fieldValue.text
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
        val newSel = androidx.compose.ui.text.TextRange(selStart, selEnd)
        tab.applyText(androidx.compose.ui.text.input.TextFieldValue(newText, newSel))
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
        val saved = controller.save(tab.contents, path)
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

    fun selectAll() {
        val tab = current ?: return
        val len = tab.fieldValue.text.length
        tab.fieldValue = tab.fieldValue.copy(selection = TextRange(0, len))
    }

    fun copySelection() {
        val tab = current ?: return
        val sel = tab.fieldValue.selection
        if (sel.collapsed) return
        ClipboardBridge.copy(tab.fieldValue.text.substring(sel.min, sel.max))
    }

    fun cutSelection() {
        val tab = current ?: return
        val sel = tab.fieldValue.selection
        if (sel.collapsed) return
        val text = tab.fieldValue.text
        ClipboardBridge.copy(text.substring(sel.min, sel.max))
        val newText = text.removeRange(sel.min, sel.max)
        tab.applyText(TextFieldValue(newText, TextRange(sel.min)))
    }

    fun pasteAtCursor() {
        val tab = current ?: return
        val payload = ClipboardBridge.paste() ?: return
        val sel = tab.fieldValue.selection
        val text = tab.fieldValue.text
        val newText = text.substring(0, sel.min) + payload + text.substring(sel.max)
        val cursor = sel.min + payload.length
        tab.applyText(TextFieldValue(newText, TextRange(cursor)))
    }

    fun bulkIndent(outdent: Boolean): Boolean {
        val tab = current ?: return false
        val sel = tab.fieldValue.selection
        if (sel.collapsed || !tab.fieldValue.text.substring(sel.min, sel.max).contains('\n')) {
            return false
        }
        val mode = if (outdent) BulkIndentCommand.Mode.Outdent else BulkIndentCommand.Mode.Indent
        val r = bulkIndentCommand.execute(
            BulkIndentCommand.Input(tab.fieldValue.text, sel.min, sel.max, mode),
        )
        tab.applyText(TextFieldValue(r.text, TextRange(r.selectionStart, r.selectionEnd)))
        return true
    }

    fun runFind() {
        val tab = current ?: return
        val matches = findMatchesCommand.execute(
            FindMatchesCommand.Input(
                text = tab.fieldValue.text,
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
        val cursor = tab.fieldValue.selection.max
        val nextAfter = matches.firstOrNull { it.first >= cursor }
        val target = nextAfter ?: matches.first()
        finder.wrappedAround = nextAfter == null
        tab.fieldValue = tab.fieldValue.copy(
            selection = TextRange(target.first, target.last + 1),
        )
    }

    fun runReplaceOne() {
        val tab = current ?: return
        val sel = tab.fieldValue.selection
        if (sel.collapsed) {
            runFind()
            return
        }
        val text = tab.fieldValue.text
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
        tab.applyText(TextFieldValue(newText, TextRange(cursor)))
        runFind()
        finder.lastReplaceCount = 1
    }

    fun runReplaceAll() {
        val tab = current ?: return
        val r = replaceAllCommand.execute(
            ReplaceAllCommand.Input(
                text = tab.fieldValue.text,
                query = finder.query,
                replacement = finder.replacement,
                regex = finder.regex,
                caseSensitive = finder.caseSensitive,
            ),
        )
        finder.lastReplaceCount = r.count
        if (r.count > 0) {
            tab.applyText(TextFieldValue(r.text, TextRange(0)))
        }
    }
}
