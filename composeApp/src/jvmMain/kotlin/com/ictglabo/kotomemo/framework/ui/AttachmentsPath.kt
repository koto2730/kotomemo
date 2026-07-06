package com.ictglabo.kotomemo.framework.ui

import com.ictglabo.kotomemo.entity.AppConfig
import java.nio.file.Path

/**
 * Resolves where attachments live for a given editor tab.
 *
 * kotomemo uses a *shared* attachments folder placed alongside the edited
 * file (Obsidian-style): for `D:\notes\my-notes.txt`, images live under
 * `D:\notes\{attachmentsFolder}\`. The name comes from AppConfig so users
 * can pick `.attachments`, `img`, `attach`, etc.
 *
 * Files with no on-disk path (Untitled) return null so callers can gate
 * paste / view behaviour with a "save first" prompt.
 */
object AttachmentsPath {
    fun folderFor(tab: TabState, config: AppConfig): Path? {
        val filePath = tab.contents.filePath ?: return null
        val folderName = config.attachmentsFolder.ifBlank { AppConfig.DEFAULT_ATTACHMENTS_FOLDER }
        return filePath.parent?.resolve(folderName)
    }
}
