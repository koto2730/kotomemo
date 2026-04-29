package com.ictglabo.kotomemo.entity

enum class LineEnding(val sequence: String) {
    LF("\n"),
    CRLF("\r\n");

    companion object {
        fun detect(text: String): LineEnding =
            if (text.contains("\r\n")) CRLF else LF

        fun platformDefault(): LineEnding =
            if (System.lineSeparator() == "\r\n") CRLF else LF
    }
}
