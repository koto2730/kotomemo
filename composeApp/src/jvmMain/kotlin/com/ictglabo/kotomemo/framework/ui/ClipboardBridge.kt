package com.ictglabo.kotomemo.framework.ui

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection

object ClipboardBridge {
    fun copy(text: String) {
        if (text.isEmpty()) return
        runCatching {
            Toolkit.getDefaultToolkit().systemClipboard
                .setContents(StringSelection(text), null)
        }
    }

    fun paste(): String? = runCatching {
        Toolkit.getDefaultToolkit().systemClipboard
            .getData(DataFlavor.stringFlavor) as? String
    }.getOrNull()
}
