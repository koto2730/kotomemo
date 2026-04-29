package com.ictglabo.kotomemo.framework.file

import com.ictglabo.kotomemo.entity.LineEnding
import java.nio.charset.Charset
import java.nio.file.Path

interface FileManager {
    fun readBytes(path: Path): ByteArray
    fun writeBytes(path: Path, bytes: ByteArray)
    fun detectEncoding(bytes: ByteArray): Pair<Charset, Boolean>
    fun decode(bytes: ByteArray, charset: Charset, hasBom: Boolean): String
    fun encode(text: String, charset: Charset, lineEnding: LineEnding, withBom: Boolean): ByteArray
}
