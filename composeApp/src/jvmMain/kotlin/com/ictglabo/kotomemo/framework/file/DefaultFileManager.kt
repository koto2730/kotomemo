package com.ictglabo.kotomemo.framework.file

import com.ictglabo.kotomemo.entity.LineEnding
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class DefaultFileManager : FileManager {

    override fun readBytes(path: Path): ByteArray = Files.readAllBytes(path)

    override fun writeBytes(path: Path, bytes: ByteArray) {
        Files.write(path, bytes)
    }

    override fun detectEncoding(bytes: ByteArray): Pair<Charset, Boolean> = when {
        bytes.startsWith(BOM_UTF8) -> StandardCharsets.UTF_8 to true
        bytes.startsWith(BOM_UTF16BE) -> StandardCharsets.UTF_16BE to true
        bytes.startsWith(BOM_UTF16LE) -> StandardCharsets.UTF_16LE to true
        else -> StandardCharsets.UTF_8 to false
    }

    override fun decode(bytes: ByteArray, charset: Charset, hasBom: Boolean): String {
        val skip = if (hasBom) bomLength(charset) else 0
        return String(bytes, skip, bytes.size - skip, charset)
    }

    override fun encode(
        text: String,
        charset: Charset,
        lineEnding: LineEnding,
        withBom: Boolean,
    ): ByteArray {
        val normalized = text.replace("\r\n", "\n").replace("\r", "\n")
        val converted = if (lineEnding == LineEnding.CRLF) {
            normalized.replace("\n", "\r\n")
        } else {
            normalized
        }
        val body = converted.toByteArray(charset)
        return if (withBom) bomFor(charset) + body else body
    }

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean {
        if (size < prefix.size) return false
        for (i in prefix.indices) if (this[i] != prefix[i]) return false
        return true
    }

    private fun bomLength(charset: Charset): Int = bomFor(charset).size

    private fun bomFor(charset: Charset): ByteArray = when (charset) {
        StandardCharsets.UTF_8 -> BOM_UTF8
        StandardCharsets.UTF_16BE -> BOM_UTF16BE
        StandardCharsets.UTF_16LE -> BOM_UTF16LE
        else -> ByteArray(0)
    }

    companion object {
        private val BOM_UTF8 = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        private val BOM_UTF16BE = byteArrayOf(0xFE.toByte(), 0xFF.toByte())
        private val BOM_UTF16LE = byteArrayOf(0xFF.toByte(), 0xFE.toByte())
    }
}
