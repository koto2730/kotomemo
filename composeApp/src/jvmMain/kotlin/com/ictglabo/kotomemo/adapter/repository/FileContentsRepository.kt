package com.ictglabo.kotomemo.adapter.repository

import com.ictglabo.kotomemo.entity.Contents
import com.ictglabo.kotomemo.entity.ContentsId
import com.ictglabo.kotomemo.entity.LineEnding
import com.ictglabo.kotomemo.framework.file.FileManager
import com.ictglabo.kotomemo.usecase.port.ContentsRepository
import java.nio.file.Path

class FileContentsRepository(
    private val fileManager: FileManager,
) : ContentsRepository {

    override fun load(path: Path): Contents {
        val raw = fileManager.readBytes(path)
        val (charset, hasBom) = fileManager.detectEncoding(raw)
        val text = fileManager.decode(raw, charset, hasBom)
        return Contents(
            id = ContentsId.generate(),
            text = text,
            filePath = path,
            charset = charset,
            lineEnding = LineEnding.detect(text),
            hasBom = hasBom,
            isDirty = false,
        )
    }

    override fun save(contents: Contents, path: Path): Contents {
        val bytes = fileManager.encode(
            text = contents.text,
            charset = contents.charset,
            lineEnding = contents.lineEnding,
            withBom = contents.hasBom,
        )
        fileManager.writeBytes(path, bytes)
        return contents.markSaved(path)
    }
}
