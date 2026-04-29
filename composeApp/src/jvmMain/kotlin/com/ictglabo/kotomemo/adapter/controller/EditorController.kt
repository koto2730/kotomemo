package com.ictglabo.kotomemo.adapter.controller

import com.ictglabo.kotomemo.entity.Contents
import com.ictglabo.kotomemo.usecase.NewContentsCommand
import com.ictglabo.kotomemo.usecase.OpenContentsCommand
import com.ictglabo.kotomemo.usecase.SaveContentsCommand
import java.nio.file.Path

class EditorController(
    private val newContentsCommand: NewContentsCommand,
    private val openContentsCommand: OpenContentsCommand,
    private val saveContentsCommand: SaveContentsCommand,
) {
    fun newContents(): Contents = newContentsCommand.execute(Unit)

    fun open(path: Path): Contents = openContentsCommand.execute(path)

    fun save(contents: Contents, path: Path): Contents =
        saveContentsCommand.execute(SaveContentsCommand.Input(contents, path))
}
