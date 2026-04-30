package com.ictglabo.kotomemo.adapter.controller

import com.ictglabo.kotomemo.entity.AppConfig
import com.ictglabo.kotomemo.entity.Contents
import com.ictglabo.kotomemo.usecase.NewContentsCommand
import com.ictglabo.kotomemo.usecase.OpenContentsCommand
import com.ictglabo.kotomemo.usecase.SaveContentsCommand
import com.ictglabo.kotomemo.usecase.SendRequestCommand
import java.nio.file.Path

class EditorController(
    private val newContentsCommand: NewContentsCommand,
    private val openContentsCommand: OpenContentsCommand,
    private val saveContentsCommand: SaveContentsCommand,
    private val sendRequestCommand: SendRequestCommand,
    private val configLoader: () -> AppConfig,
    private val configSaver: (AppConfig) -> Unit,
) {
    fun newContents(): Contents = newContentsCommand.execute(Unit)

    fun open(path: Path): Contents = openContentsCommand.execute(path)

    fun save(contents: Contents, path: Path): Contents =
        saveContentsCommand.execute(SaveContentsCommand.Input(contents, path))

    fun loadConfig(): AppConfig = configLoader()
    fun saveConfig(config: AppConfig) = configSaver(config)
    fun send(input: SendRequestCommand.Input): SendRequestCommand.Output =
        sendRequestCommand.execute(input)
}
