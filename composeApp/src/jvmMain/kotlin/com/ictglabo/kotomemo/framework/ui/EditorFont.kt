package com.ictglabo.kotomemo.framework.ui

import androidx.compose.ui.text.font.FontFamily

data class EditorFont(
    val family: Family = Family.Monospace,
    val size: Int = 14,
) {
    enum class Family { Monospace, SansSerif, Serif }

    fun toFontFamily(): FontFamily = when (family) {
        Family.Monospace -> FontFamily.Monospace
        Family.SansSerif -> FontFamily.SansSerif
        Family.Serif -> FontFamily.Serif
    }

    companion object {
        const val MIN_SIZE = 8
        const val MAX_SIZE = 48
    }
}
