package com.ictglabo.kotomemo.usecase

import com.ictglabo.kotomemo.entity.Contents
import com.ictglabo.kotomemo.usecase.port.ContentsRepository
import java.nio.file.Path

class SaveContentsCommand(
    private val repository: ContentsRepository,
) : Command<SaveContentsCommand.Input, Contents> {

    data class Input(val contents: Contents, val path: Path)

    override fun execute(input: Input): Contents =
        repository.save(input.contents, input.path)
}
