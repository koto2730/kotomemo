package com.ictglabo.kotomemo.framework

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.ictglabo.kotomemo.adapter.controller.EditorController
import com.ictglabo.kotomemo.adapter.http.JdkHttpClient
import com.ictglabo.kotomemo.adapter.repository.FileContentsRepository
import com.ictglabo.kotomemo.adapter.repository.JsonConfigRepository
import com.ictglabo.kotomemo.framework.config.ConfigPaths
import com.ictglabo.kotomemo.framework.file.DefaultFileManager
import com.ictglabo.kotomemo.framework.ui.AppWindow
import com.ictglabo.kotomemo.usecase.NewContentsCommand
import com.ictglabo.kotomemo.usecase.OpenContentsCommand
import com.ictglabo.kotomemo.usecase.SaveContentsCommand
import com.ictglabo.kotomemo.usecase.SendRequestCommand
import java.nio.file.Path

fun main(args: Array<String>) {
    val fileManager = DefaultFileManager()
    val contentsRepository = FileContentsRepository(fileManager)
    val httpClient = JdkHttpClient()
    val configRepository = JsonConfigRepository(ConfigPaths.configFile)

    val controller = EditorController(
        newContentsCommand = NewContentsCommand(),
        openContentsCommand = OpenContentsCommand(contentsRepository),
        saveContentsCommand = SaveContentsCommand(contentsRepository),
        sendRequestCommand = SendRequestCommand(httpClient),
        configLoader = configRepository::load,
        configSaver = configRepository::save,
    )

    val initialPaths = args.mapNotNull { runCatching { Path.of(it).toAbsolutePath() }.getOrNull() }

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "kotomemo",
        ) {
            AppWindow(controller, initialPaths, onExit = ::exitApplication)
        }
    }
}
