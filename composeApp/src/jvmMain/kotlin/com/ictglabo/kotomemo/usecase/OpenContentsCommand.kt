package com.ictglabo.kotomemo.usecase

import com.ictglabo.kotomemo.entity.Contents
import com.ictglabo.kotomemo.usecase.port.ContentsRepository
import java.nio.file.Path

class OpenContentsCommand(
    private val repository: ContentsRepository,
) : Command<Path, Contents> {
    override fun execute(input: Path): Contents = repository.load(input)
}
