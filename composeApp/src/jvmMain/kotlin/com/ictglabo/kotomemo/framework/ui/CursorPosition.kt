package com.ictglabo.kotomemo.framework.ui

data class CursorPosition(val line: Int, val column: Int) {
    companion object {
        fun of(text: String, offset: Int): CursorPosition {
            val safe = offset.coerceIn(0, text.length)
            var line = 1
            var lastBreak = -1
            for (i in 0 until safe) {
                if (text[i] == '\n') {
                    line++
                    lastBreak = i
                }
            }
            val column = safe - lastBreak
            return CursorPosition(line, column)
        }
    }
}
