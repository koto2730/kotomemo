package com.ictglabo.kotomemo.usecase.port

import com.ictglabo.kotomemo.entity.Contents
import java.nio.file.Path

interface ContentsRepository {
    fun load(path: Path): Contents
    fun save(contents: Contents, path: Path): Contents
}
