package com.ictglabo.kotomemo.framework.ui

import java.awt.Image
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.image.BufferedImage

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

    /**
     * Returns the clipboard content as a BufferedImage if it holds one
     * (Windows Win+Shift+S, macOS Cmd+Shift+Ctrl+4, etc.). Returns null
     * for text-only clipboard content or on any read failure.
     */
    fun pasteImage(): BufferedImage? = runCatching {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val transferable = clipboard.getContents(null) ?: return@runCatching null
        if (!transferable.isDataFlavorSupported(DataFlavor.imageFlavor)) return@runCatching null
        val image = transferable.getTransferData(DataFlavor.imageFlavor) as? Image
            ?: return@runCatching null
        image.toBufferedImage()
    }.getOrNull()

    private fun Image.toBufferedImage(): BufferedImage {
        if (this is BufferedImage) return this
        val w = getWidth(null).coerceAtLeast(1)
        val h = getHeight(null).coerceAtLeast(1)
        val buffered = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val g = buffered.createGraphics()
        try {
            g.drawImage(this, 0, 0, null)
        } finally {
            g.dispose()
        }
        return buffered
    }
}
