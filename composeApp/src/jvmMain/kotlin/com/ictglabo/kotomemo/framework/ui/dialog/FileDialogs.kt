package com.ictglabo.kotomemo.framework.ui.dialog

import java.awt.FileDialog
import java.awt.Frame
import java.nio.file.Path

object FileDialogs {

    fun openFile(parent: Frame?, title: String = "Open"): Path? =
        showDialog(parent, title, FileDialog.LOAD)

    fun saveFile(parent: Frame?, title: String = "Save As", suggestedName: String? = null): Path? =
        showDialog(parent, title, FileDialog.SAVE, suggestedName)

    private fun showDialog(
        parent: Frame?,
        title: String,
        mode: Int,
        suggestedName: String? = null,
    ): Path? {
        val dialog = FileDialog(parent, title, mode)
        if (suggestedName != null) dialog.file = suggestedName
        dialog.isVisible = true
        val dir = dialog.directory ?: return null
        val file = dialog.file ?: return null
        return Path.of(dir, file)
    }
}
